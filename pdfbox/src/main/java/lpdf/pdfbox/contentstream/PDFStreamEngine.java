/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lpdf.pdfbox.contentstream;

import lpdf.harmony.awt.geom.GeneralPath;
import lpdf.harmony.awt.geom.Point2D;
import lpdf.harmony.awt.geom.Rectangle2D;
import lpdf.pdfbox.contentstream.operator.MissingOperandException;
import lpdf.pdfbox.contentstream.operator.Operator;
import lpdf.pdfbox.contentstream.operator.OperatorProcessor;
import lpdf.pdfbox.contentstream.operator.state.EmptyGraphicsStackException;
import lpdf.pdfbox.cos.COSArray;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSNumber;
import lpdf.pdfbox.cos.COSString;
import lpdf.pdfbox.filter.MissingImageReaderException;
import lpdf.pdfbox.pdfparser.PDFStreamParser;
import lpdf.pdfbox.pdmodel.MissingResourceException;
import lpdf.pdfbox.pdmodel.PDPage;
import lpdf.pdfbox.pdmodel.PDResources;
import lpdf.pdfbox.pdmodel.common.PDRectangle;
import lpdf.pdfbox.pdmodel.font.PDFont;
import lpdf.pdfbox.pdmodel.font.PDType1Font;
import lpdf.pdfbox.pdmodel.font.PDType3CharProc;
import lpdf.pdfbox.pdmodel.font.PDType3Font;
import lpdf.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import lpdf.pdfbox.pdmodel.graphics.PDLineDashPattern;
import lpdf.pdfbox.pdmodel.graphics.blend.BlendMode;
import lpdf.pdfbox.pdmodel.graphics.color.PDColor;
import lpdf.pdfbox.pdmodel.graphics.color.PDColorSpace;
import lpdf.pdfbox.pdmodel.graphics.form.PDFormXObject;
import lpdf.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import lpdf.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import lpdf.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import lpdf.pdfbox.pdmodel.graphics.state.PDTextState;
import lpdf.pdfbox.util.Matrix;
import lpdf.pdfbox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes a PDF content stream and executes certain operations.
 * Provides a callback interface for clients that want to do things with the stream.
 *
 * @author Ben Litchfield
 */
public abstract class PDFStreamEngine {
    private static final Logger LOG = LoggerFactory.getLogger(PDFStreamEngine.class);

    private final Map<String, OperatorProcessor> operators = new HashMap<>(80);

    private Matrix textMatrix;
    private Matrix textLineMatrix;

    private Deque<PDGraphicsState> graphicsStack = new ArrayDeque<>();

    private PDResources resources;
    private PDPage currentPage;
    private boolean isProcessingPage;
    private Matrix initialMatrix;

    // used to monitor potentially recursive operations.
    private int level = 0;

    // default font, used if there isn't any font available
    private PDFont defaultFont;

    /**
     * Creates a new PDFStreamEngine.
     */
    protected PDFStreamEngine() {
    }

    /**
     * Adds an operator processor to the engine.
     *
     * @param op operator processor
     */
    public final void addOperator(OperatorProcessor op) {
        operators.put(op.getName(), op);
    }

    /**
     * Initializes the stream engine for the given page.
     */
    private void initPage(PDPage page) {
        if (page == null) {
            throw new IllegalArgumentException("Page cannot be null");
        }
        currentPage = page;
        graphicsStack.clear();
        graphicsStack.push(new PDGraphicsState(page.getCropBox()));
        textMatrix = null;
        textLineMatrix = null;
        resources = null;
        initialMatrix = page.getMatrix();
    }

    /**
     * Provide standard 14 Helvetica font as default if there isn't any font available.
     *
     * @return the default font
     */
    private PDFont getDefaultFont() {
        if (defaultFont == null) {
            defaultFont = new PDType1Font(FontName.HELVETICA);
        }
        return defaultFont;
    }

    /**
     * This will initialize and process the contents of the stream.
     *
     * @param page the page to process
     * @throws IOException if there is an error accessing the stream
     */
    public void processPage(PDPage page) throws IOException {
        initPage(page);
        if (page.hasContents()) {
            isProcessingPage = true;
            processStream(page);
            isProcessingPage = false;
        }
    }

    /**
     * Shows a transparency group from the content stream.
     *
     * @param form transparency group (form) XObject
     * @throws IOException if the transparency group cannot be processed
     */
    public void showTransparencyGroup(PDTransparencyGroup form) throws IOException {
        processTransparencyGroup(form);
    }

    /**
     * Shows a form from the content stream.
     *
     * @param form form XObject
     * @throws IOException if the form cannot be processed
     */
    public void showForm(PDFormXObject form) throws IOException {
        if (currentPage == null) {
            throw new IllegalStateException("No current page, call " +
                    "#processChildStream(PDContentStream, PDPage) instead");
        }
        if (form.getCOSObject().getLength() > 0) {
            processStream(form);
        }
    }

    /**
     * Processes a soft mask transparency group stream.
     *
     * @param group transparency group used for the soft mask
     * @throws IOException if the transparency group cannot be processed
     */
    protected void processSoftMask(PDTransparencyGroup group) throws IOException {
        saveGraphicsState();
        Matrix softMaskCTM = getGraphicsState().getSoftMask().getInitialTransformationMatrix();
        getGraphicsState().setCurrentTransformationMatrix(softMaskCTM);

        // PDFBOX-5621: save text matrices (softmasks may contain BT/ET)
        Matrix textMatrixOld = textMatrix;
        textMatrix = new Matrix();
        Matrix textLineMatrixOld = textLineMatrix;
        textLineMatrix = new Matrix();
        try {
            processTransparencyGroup(group);
        } finally {
            textMatrix = textMatrixOld;
            textLineMatrix = textLineMatrixOld;
            restoreGraphicsState();
        }
    }

    /**
     * Processes a transparency group stream.
     *
     * @param group transparency group to be processed
     * @throws IOException if the transparency group cannot be processed
     */
    protected void processTransparencyGroup(PDTransparencyGroup group) throws IOException {
        if (currentPage == null) {
            throw new IllegalStateException("No current page, call " +
                    "#processChildStream(PDContentStream, PDPage) instead");
        }

        PDResources parent = pushResources(group);
        Deque<PDGraphicsState> savedStack = saveGraphicsStack();

        Matrix parentMatrix = initialMatrix;
        PDGraphicsState graphicsState = getGraphicsState();

        // the stream's initial matrix includes the parent CTM, e.g. this allows a scaled form
        initialMatrix = graphicsState.getCurrentTransformationMatrix().clone();

        // transform the CTM using the stream's matrix
        graphicsState.getCurrentTransformationMatrix().concatenate(group.getMatrix());

        // Before execution of the transparency group XObject’s content stream,
        // the current blend mode in the graphics state shall be initialized to Normal,
        // the current stroking and nonstroking alpha constants to 1.0, and the current soft mask to None.
        graphicsState.setBlendMode(BlendMode.NORMAL);
        graphicsState.setAlphaConstant(1);
        graphicsState.setNonStrokeAlphaConstant(1);
        graphicsState.setSoftMask(null);

        // clip to bounding box
        clipToRect(group.getBBox());

        try {
            processStreamOperators(group);
        } finally {
            initialMatrix = parentMatrix;

            restoreGraphicsStack(savedStack);
            popResources(parent);
        }
    }

    /**
     * Processes a Type 3 character stream.
     *
     * @param charProc            Type 3 character procedure
     * @param textRenderingMatrix the Text Rendering Matrix
     * @throws IOException if there is an error reading or parsing the character content stream.
     */
    protected void processType3Stream(PDType3CharProc charProc, Matrix textRenderingMatrix)
            throws IOException {
        if (currentPage == null) {
            throw new IllegalStateException("No current page, call " +
                    "#processChildStream(PDContentStream, PDPage) instead");
        }

        PDResources parent = pushResources(charProc);
        Deque<PDGraphicsState> savedStack = saveGraphicsStack();

        // replace the CTM with the TRM
        getGraphicsState().setCurrentTransformationMatrix(textRenderingMatrix);

        // transform the CTM using the stream's matrix (this is the FontMatrix)
        textRenderingMatrix.concatenate(charProc.getMatrix());

        // note: we don't clip to the BBox as it is often wrong, see PDFBOX-1917

        // save text matrices (Type 3 stream may contain BT/ET, see PDFBOX-2137)
        Matrix textMatrixOld = textMatrix;
        textMatrix = new Matrix();
        Matrix textLineMatrixOld = textLineMatrix;
        textLineMatrix = new Matrix();

        try {
            processStreamOperators(charProc);
        } finally {
            // restore text matrices
            textMatrix = textMatrixOld;
            textLineMatrix = textLineMatrixOld;

            restoreGraphicsStack(savedStack);
            popResources(parent);
        }
    }

    /**
     * Process the given tiling pattern.
     *
     * @param tilingPattern the tiling pattern
     * @param color         color to use, if this is an uncoloured pattern, otherwise null.
     * @param colorSpace    color space to use, if this is an uncoloured pattern, otherwise null.
     * @throws IOException if there is an error reading or parsing the tiling pattern content stream.
     */
    protected final void processTilingPattern(PDTilingPattern tilingPattern, PDColor color,
                                              PDColorSpace colorSpace) throws IOException {
        processTilingPattern(tilingPattern, color, colorSpace, tilingPattern.getMatrix());
    }

    /**
     * Process the given tiling pattern. Allows the pattern matrix to be overridden for custom
     * rendering.
     *
     * @param tilingPattern the tiling pattern
     * @param color         color to use, if this is an uncoloured pattern, otherwise null.
     * @param colorSpace    color space to use, if this is an uncoloured pattern, otherwise null.
     * @param patternMatrix the pattern matrix, may be overridden for custom rendering.
     * @throws IOException if there is an error reading or parsing the tiling pattern content stream.
     */
    protected final void processTilingPattern(PDTilingPattern tilingPattern, PDColor color,
                                              PDColorSpace colorSpace, Matrix patternMatrix)
            throws IOException {
        PDResources parent = pushResources(tilingPattern);

        Matrix parentMatrix = initialMatrix;
        initialMatrix = Matrix.concatenate(initialMatrix, patternMatrix);

        // save the original graphics state
        Deque<PDGraphicsState> savedStack = saveGraphicsStack();

        // save a clean state (new clipping path, line path, etc.)
        PDRectangle tilingBBox = tilingPattern.getBBox();
        Rectangle2D bbox = tilingBBox.transform(patternMatrix).getBounds2D();
        PDRectangle rect = new PDRectangle((float) bbox.getX(), (float) bbox.getY(),
                (float) bbox.getWidth(), (float) bbox.getHeight());
        graphicsStack.push(new PDGraphicsState(rect));
        PDGraphicsState graphicsState = getGraphicsState();

        // non-colored patterns have to be given a color
        if (colorSpace != null) {
            color = new PDColor(color.getComponents(), colorSpace);
            graphicsState.setNonStrokingColorSpace(colorSpace);
            graphicsState.setNonStrokingColor(color);
            graphicsState.setStrokingColorSpace(colorSpace);
            graphicsState.setStrokingColor(color);
        }

        // transform the CTM using the stream's matrix
        graphicsState.getCurrentTransformationMatrix().concatenate(patternMatrix);

        // clip to bounding box
        clipToRect(tilingBBox);

        // save text matrices (pattern stream may contain BT/ET, see PDFBOX-4896)
        Matrix textMatrixSave = textMatrix;
        Matrix textLineMatrixSave = textLineMatrix;

        try {
            processStreamOperators(tilingPattern);
        } finally {
            textMatrix = textMatrixSave;
            textLineMatrix = textLineMatrixSave;
            initialMatrix = parentMatrix;
            restoreGraphicsStack(savedStack);
            popResources(parent);
        }
    }

    /**
     * Process a child stream of the given page. Cannot be used with {@link #processPage(PDPage)}.
     *
     * @param contentStream the child content stream
     * @param page          the page to be used for processing
     * @throws IOException if there is an exception while processing the stream
     */
    protected void processChildStream(PDContentStream contentStream, PDPage page) throws IOException {
        if (isProcessingPage) {
            throw new IllegalStateException("Current page has already been set via " +
                    " #processPage(PDPage) call #processChildStream(PDContentStream) instead");
        }
        initPage(page);
        processStream(contentStream);
        currentPage = null;
    }

    /**
     * Process a content stream.
     *
     * @param contentStream the content stream
     * @throws IOException if there is an exception while processing the stream
     */
    private void processStream(PDContentStream contentStream) throws IOException {
        PDResources parent = pushResources(contentStream);
        Deque<PDGraphicsState> savedStack = saveGraphicsStack();
        Matrix parentMatrix = initialMatrix;
        PDGraphicsState graphicsState = getGraphicsState();

        // transform the CTM using the stream's matrix
        graphicsState.getCurrentTransformationMatrix().concatenate(contentStream.getMatrix());

        // the stream's initial matrix includes the parent CTM, e.g. this allows a scaled form
        initialMatrix = graphicsState.getCurrentTransformationMatrix().clone();

        // clip to bounding box
        PDRectangle bbox = contentStream.getBBox();
        clipToRect(bbox);

        try {
            processStreamOperators(contentStream);
        } finally {
            initialMatrix = parentMatrix;
            restoreGraphicsStack(savedStack);
            popResources(parent);
        }
    }

    /**
     * Processes the operators of the given content stream.
     *
     * @param contentStream to content stream to parse.
     * @throws IOException if there is an error reading or parsing the content stream.
     */
    private void processStreamOperators(PDContentStream contentStream) throws IOException {
        List<COSBase> arguments = new ArrayList<>();
        PDFStreamParser parser = new PDFStreamParser(contentStream);
        Object token = parser.parseNextToken();
        while (token != null) {
            if (token instanceof Operator) {
                processOperator((Operator) token, arguments);
                arguments.clear();
            } else {
                arguments.add((COSBase) token);
            }
            token = parser.parseNextToken();
        }
    }

    /**
     * Pushes the given stream's resources, returning the previous resources.
     */
    private PDResources pushResources(PDContentStream contentStream) {
        // resource lookup: first look for stream resources, then fallback to the current page
        PDResources parentResources = resources;
        PDResources streamResources = contentStream.getResources();
        if (streamResources != null) {
            resources = streamResources;
        } else if (resources != null) {
            // inherit directly from parent stream, this is not in the PDF spec, but the file from
            // PDFBOX-1359 does this and works in Acrobat
        } else {
            resources = currentPage.getResources();

            // resources are required in PDF
            if (resources == null) {
                resources = new PDResources();
            }
        }

        return parentResources;
    }

    /**
     * Pops the current resources, replacing them with the given resources.
     */
    private void popResources(PDResources parentResources) {
        resources = parentResources;
    }

    /**
     * Transforms the given rectangle using the CTM and then intersects it with the current
     * clipping area.
     */
    private void clipToRect(PDRectangle rectangle) {
        if (rectangle != null) {
            PDGraphicsState graphicsState = getGraphicsState();
            GeneralPath clip = rectangle.transform(graphicsState.getCurrentTransformationMatrix());
            graphicsState.intersectClippingPath(clip);
        }
    }

    /**
     * Called when the BT operator is encountered. This method is for overriding in subclasses, the
     * default implementation does nothing.
     *
     * @throws IOException if there was an error processing the text
     */
    public void beginText() throws IOException {
        // overridden in subclasses
    }

    /**
     * Called when the ET operator is encountered. This method is for overriding in subclasses, the
     * default implementation does nothing.
     *
     * @throws IOException if there was an error processing the text
     */
    public void endText() throws IOException {
        // overridden in subclasses
    }

    /**
     * Called when a string of text is to be shown.
     *
     * @param string the encoded text
     * @throws IOException if there was an error showing the text
     */
    public void showTextString(byte[] string) throws IOException {
        showText(string);
    }

    /**
     * Called when a string of text with spacing adjustments is to be shown.
     *
     * @param array array of encoded text strings and adjustments
     * @throws IOException if there was an error showing the text
     */
    public void showTextStrings(COSArray array) throws IOException {
        PDTextState textState = getGraphicsState().getTextState();
        float fontSize = textState.getFontSize();
        float horizontalScaling = textState.getHorizontalScaling() / 100f;
        PDFont font = textState.getFont();
        boolean isVertical = false;
        if (font != null) {
            isVertical = font.isVertical();
        }

        for (COSBase obj : array) {
            if (obj instanceof COSNumber) {
                float tj = ((COSNumber) obj).floatValue();

                // calculate the combined displacements
                float tx;
                float ty;
                if (isVertical) {
                    tx = 0;
                    ty = -tj / 1000 * fontSize;
                } else {
                    tx = -tj / 1000 * fontSize * horizontalScaling;
                    ty = 0;
                }

                applyTextAdjustment(tx, ty);
            } else if (obj instanceof COSString) {
                byte[] string = ((COSString) obj).getBytes();
                showText(string);
            } else if (obj instanceof COSArray) {
                LOG.error("Nested arrays are not allowed in an array for TJ operation: " + obj);
            } else {
                LOG.error("Unknown type " + obj.getClass().getSimpleName() +
                        " in array for TJ operation: " + obj);
            }
        }
    }

    /**
     * Applies a text position adjustment from the TJ operator. May be overridden in subclasses.
     *
     * @param tx x-translation
     * @param ty y-translation
     */
    protected void applyTextAdjustment(float tx, float ty) {
        // update the text matrix
        textMatrix.translate(tx, ty);
    }

    /**
     * Process text from the PDF Stream. You should override this method if you want to
     * perform an action when encoded text is being processed.
     *
     * @param string the encoded text
     * @throws IOException if there is an error processing the string
     */
    protected void showText(byte[] string) throws IOException {
        PDGraphicsState state = getGraphicsState();
        PDTextState textState = state.getTextState();

        // get the current font
        PDFont font = textState.getFont();
        if (font == null) {
            LOG.warn("No current font, will use default");
            font = getDefaultFont();
        }

        float fontSize = textState.getFontSize();
        float horizontalScaling = textState.getHorizontalScaling() / 100f;
        float charSpacing = textState.getCharacterSpacing();

        // put the text state parameters into matrix form
        Matrix parameters = new Matrix(
                fontSize * horizontalScaling, 0, // 0
                0, fontSize,                     // 0
                0, textState.getRise());         // 1

        // read the stream until it is empty
        InputStream in = new ByteArrayInputStream(string);
        while (in.available() > 0) {
            // decode a character
            int before = in.available();
            int code = font.readCode(in);
            int codeLength = before - in.available();

            // Word spacing shall be applied to every occurrence of the single-byte character code
            // 32 in a string when using a simple font or a composite font that defines code 32 as
            // a single-byte code.
            float wordSpacing = 0;
            if (codeLength == 1 && code == 32) {
                wordSpacing += textState.getWordSpacing();
            }

            // text rendering matrix (text space -> device space)
            Matrix ctm = state.getCurrentTransformationMatrix();
            Matrix textRenderingMatrix = parameters.multiply(textMatrix).multiply(ctm);

            // get glyph's position vector if this is vertical text
            // changes to vertical text should be tested with PDFBOX-2294 and PDFBOX-1422
            if (font.isVertical()) {
                // position vector, in text space
                Vector v = font.getPositionVector(code);

                // apply the position vector to the horizontal origin to get the vertical origin
                textRenderingMatrix.translate(v);
            }

            // get glyph's horizontal and vertical displacements, in text space
            Vector w = font.getDisplacement(code);

            // process the decoded glyph
            showGlyph(textRenderingMatrix, font, code, w);

            // calculate the combined displacements
            float tx;
            float ty;
            if (font.isVertical()) {
                tx = 0;
                ty = w.getY() * fontSize + charSpacing + wordSpacing;
            } else {
                tx = (w.getX() * fontSize + charSpacing + wordSpacing) * horizontalScaling;
                ty = 0;
            }

            // update the text matrix
            textMatrix.translate(tx, ty);
        }
    }

    /**
     * Called when a glyph is to be processed. This method is intended for overriding in subclasses,
     * the default implementation does nothing.
     *
     * @param textRenderingMatrix the current text rendering matrix, T<sub>rm</sub>
     * @param font                the current font
     * @param code                internal PDF character code for the glyph
     * @param displacement        the displacement (i.e. advance) of the glyph in text space
     * @throws IOException if the glyph cannot be processed
     */
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement)
            throws IOException {
        if (font instanceof PDType3Font) {
            showType3Glyph(textRenderingMatrix, (PDType3Font) font, code, displacement);
        } else {
            showFontGlyph(textRenderingMatrix, font, code, displacement);
        }
    }

    /**
     * Called when a glyph is to be processed. This method is intended for overriding in subclasses,
     * the default implementation does nothing.
     *
     * @param textRenderingMatrix the current text rendering matrix, T<sub>rm</sub>
     * @param font                the current font
     * @param code                internal PDF character code for the glyph
     * @param displacement        the displacement (i.e. advance) of the glyph in text space
     * @throws IOException if the glyph cannot be processed
     */
    protected void showFontGlyph(Matrix textRenderingMatrix, PDFont font,
                                 int code, Vector displacement) throws IOException {
        // overridden in subclasses
    }

    /**
     * Called when a glyph is to be processed. This method is intended for overriding in subclasses,
     * the default implementation does nothing.
     *
     * @param textRenderingMatrix the current text rendering matrix, T<sub>rm</sub>
     * @param font                the current font
     * @param code                internal PDF character code for the glyph
     * @param displacement        the displacement (i.e. advance) of the glyph in text space
     * @throws IOException if the glyph cannot be processed
     */
    protected void showType3Glyph(Matrix textRenderingMatrix, PDType3Font font, int code,
                                  Vector displacement) throws IOException {
        PDType3CharProc charProc = font.getCharProc(code);
        if (charProc != null) {
            processType3Stream(charProc, textRenderingMatrix);
        }
    }

    /**
     * Called when a marked content group begins
     *
     * @param tag        indicates the role or significance of the sequence
     * @param properties optional properties
     */
    public void beginMarkedContentSequence(COSName tag, COSDictionary properties) {
        // overridden in subclasses
    }

    /**
     * Called when a marked content group ends
     */
    public void endMarkedContentSequence() {
        // overridden in subclasses
    }

    /**
     * This is used to handle an operation.
     *
     * @param operation The operation to perform.
     * @param arguments The list of arguments.
     * @throws IOException If there is an error processing the operation.
     */
    public void processOperator(String operation, List<COSBase> arguments) throws IOException {
        Operator operator = Operator.getOperator(operation);
        processOperator(operator, arguments);
    }

    /**
     * This is used to handle an operation.
     *
     * @param operator The operation to perform.
     * @param operands The list of arguments.
     * @throws IOException If there is an error processing the operation.
     */
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String name = operator.getName();
        OperatorProcessor processor = operators.get(name);
        if (processor != null) {
            try {
                processor.process(operator, operands);
            } catch (IOException e) {
                operatorException(operator, operands, e);
            }
        } else {
            unsupportedOperator(operator, operands);
        }
    }

    /**
     * Called when an unsupported operator is encountered.
     *
     * @param operator The unknown operator.
     * @param operands The list of operands.
     * @throws IOException if there is an error processing the unsupported operator
     */
    protected void unsupportedOperator(Operator operator, List<COSBase> operands) throws IOException {
        // overridden in subclasses
    }

    /**
     * Called when an exception is thrown by an operator.
     *
     * @param operator  The unknown operator.
     * @param operands  The list of operands.
     * @param exception the excpetion which occured when processing the operator
     * @throws IOException if there is an error processing the operator exception
     */
    protected void operatorException(Operator operator, List<COSBase> operands, IOException exception)
            throws IOException {
        if (exception instanceof MissingOperandException ||
                exception instanceof MissingResourceException ||
                exception instanceof MissingImageReaderException) {
            LOG.error(exception.getMessage());
        } else if (exception instanceof EmptyGraphicsStackException) {
            LOG.warn(exception.getMessage());
        } else if (operator.getName().equals("Do")) {
            // todo: this too forgiving, but PDFBox has always worked this way for DrawObject
            //       some careful refactoring is needed
            LOG.warn(exception.getMessage());
        } else {
            throw exception;
        }
    }

    /**
     * Pushes the current graphics state to the stack.
     */
    public void saveGraphicsState() {
        graphicsStack.push(graphicsStack.peek().clone());
    }

    /**
     * Pops the current graphics state from the stack.
     */
    public void restoreGraphicsState() {
        graphicsStack.pop();
    }

    /**
     * Saves the entire graphics stack.
     *
     * @return the saved graphics state stack.
     */
    protected final Deque<PDGraphicsState> saveGraphicsStack() {
        Deque<PDGraphicsState> savedStack = graphicsStack;
        graphicsStack = new ArrayDeque<>(1);
        graphicsStack.add(savedStack.peek().clone());
        return savedStack;
    }

    /**
     * Restores the entire graphics stack.
     *
     * @param snapshot the graphics state to be restored
     */
    protected final void restoreGraphicsStack(Deque<PDGraphicsState> snapshot) {
        graphicsStack = snapshot;
    }

    /**
     * @return Returns the size of the graphicsStack.
     */
    public int getGraphicsStackSize() {
        return graphicsStack.size();
    }

    /**
     * @return Returns the graphicsState.
     */
    public PDGraphicsState getGraphicsState() {
        return graphicsStack.peek();
    }

    /**
     * @return Returns the textLineMatrix.
     */
    public Matrix getTextLineMatrix() {
        return textLineMatrix;
    }

    /**
     * @param value The textLineMatrix to set.
     */
    public void setTextLineMatrix(Matrix value) {
        textLineMatrix = value;
    }

    /**
     * @return Returns the textMatrix.
     */
    public Matrix getTextMatrix() {
        return textMatrix;
    }

    /**
     * @param value The textMatrix to set.
     */
    public void setTextMatrix(Matrix value) {
        textMatrix = value;
    }

    /**
     * @param array dash array
     * @param phase dash phase
     */
    public void setLineDashPattern(COSArray array, int phase) {
        if (phase < 0) {
            LOG.warn("Dash phase has negative value " + phase + ", set to 0");
            phase = 0;
        }
        PDLineDashPattern lineDash = new PDLineDashPattern(array, phase);
        getGraphicsState().setLineDashPattern(lineDash);
    }

    /**
     * @return the stream' resources. This is mainly to be used by the {@link OperatorProcessor}
     * classes.
     */
    public PDResources getResources() {
        return resources;
    }

    /**
     * Returns the current page.
     *
     * @return the current page
     */
    public PDPage getCurrentPage() {
        return currentPage;
    }

    /**
     * Gets the stream's initial matrix.
     *
     * @return the initial matrix
     */
    public Matrix getInitialMatrix() {
        return initialMatrix;
    }

    /**
     * Transforms a point using the CTM.
     *
     * @param x the x-coordinate of the point to be transformed
     * @param y the y-coordinate of the point to be transformed
     * @return the transformed point
     */
    public Point2D.Float transformedPoint(float x, float y) {
        float[] position = {x, y};
        getGraphicsState().getCurrentTransformationMatrix().createAffineTransform()
                .transform(position, 0, position, 0, 1);
        return new Point2D.Float(position[0], position[1]);
    }

    /**
     * Transforms a width using the CTM.
     *
     * @param width the width to be transformed
     * @return the transformed width
     */
    protected float transformWidth(float width) {
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
        float x = ctm.getScaleX() + ctm.getShearX();
        float y = ctm.getScaleY() + ctm.getShearY();
        return width * (float) Math.sqrt((x * x + y * y) * 0.5);
    }

    /**
     * Get the current level. This can be used to decide whether a recursion has done too deep and
     * an operation should be skipped to avoid a stack overflow.
     *
     * @return the current level.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Increase the level. Call this before running a potentially recursive operation.
     */
    public void increaseLevel() {
        ++level;
    }

    /**
     * Decrease the level. Call this after running a potentially recursive operation. A log message
     * is shown if the level is below 0. This can happen if the level is not decreased after an
     * operation is done, e.g. by using a "finally" block.
     */
    public void decreaseLevel() {
        --level;
        if (level < 0) {
            LOG.error("level is " + level);
        }
    }
}
