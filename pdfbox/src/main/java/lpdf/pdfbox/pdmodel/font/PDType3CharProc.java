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

package lpdf.pdfbox.pdmodel.font;

import lpdf.io.RandomAccessInputStream;
import lpdf.io.RandomAccessRead;
import lpdf.pdfbox.contentstream.PDContentStream;
import lpdf.pdfbox.contentstream.operator.Operator;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSNumber;
import lpdf.pdfbox.cos.COSStream;
import lpdf.pdfbox.pdfparser.PDFStreamParser;
import lpdf.pdfbox.pdmodel.PDResources;
import lpdf.pdfbox.pdmodel.common.COSObjectable;
import lpdf.pdfbox.pdmodel.common.PDRectangle;
import lpdf.pdfbox.pdmodel.common.PDStream;
import lpdf.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A Type 3 character procedure. This is a standalone PDF content stream.
 *
 * @author John Hewson
 */
public final class PDType3CharProc implements COSObjectable, PDContentStream {
    private static final Logger LOG = LoggerFactory.getLogger(PDType3CharProc.class);

    private final PDType3Font font;
    private final COSStream charStream;

    public PDType3CharProc(PDType3Font font, COSStream charStream) {
        this.font = font;
        this.charStream = charStream;
    }

    @Override
    public COSStream getCOSObject() {
        return charStream;
    }

    public PDType3Font getFont() {
        return font;
    }

    public PDStream getContentStream() {
        return new PDStream(charStream);
    }

    @Override
    public InputStream getContents() throws IOException {
        return new RandomAccessInputStream(getContentsForRandomAccess());
    }

    @Override
    public RandomAccessRead getContentsForRandomAccess() throws IOException {
        return charStream.createView();
    }

    @Override
    public PDResources getResources() {
        if (charStream.containsKey(COSName.RESOURCES)) {
            // PDFBOX-5294
            LOG.warn("Using resources dictionary found in charproc entry");
            LOG.warn("This should have been in the font or in the page dictionary");
            return new PDResources((COSDictionary) charStream.getDictionaryObject(COSName.RESOURCES));
        }
        return font.getResources();
    }

    @Override
    public PDRectangle getBBox() {
        return font.getFontBBox();
    }

    /**
     * Calculate the bounding box of this glyph. This will work only if the first operator in the
     * stream is d1.
     *
     * @return the bounding box of this glyph, or null if the first operator is not d1.
     * @throws IOException If an io error occurs while parsing the stream.
     */
    public PDRectangle getGlyphBBox() throws IOException {
        List<COSBase> arguments = new ArrayList<>();
        PDFStreamParser parser = new PDFStreamParser(this);
        Object token = parser.parseNextToken();
        while (token != null) {
            if (token instanceof Operator) {
                if (((Operator) token).getName().equals("d1") && arguments.size() == 6) {
                    for (int i = 0; i < 6; ++i) {
                        if (!(arguments.get(i) instanceof COSNumber)) {
                            return null;
                        }
                    }
                    float x = ((COSNumber) arguments.get(2)).floatValue();
                    float y = ((COSNumber) arguments.get(3)).floatValue();
                    return new PDRectangle(
                            x,
                            y,
                            ((COSNumber) arguments.get(4)).floatValue() - x,
                            ((COSNumber) arguments.get(5)).floatValue() - y);
                } else {
                    return null;
                }
            } else {
                arguments.add((COSBase) token);
            }
            token = parser.parseNextToken();
        }
        return null;
    }

    @Override
    public Matrix getMatrix() {
        return font.getFontMatrix();
    }

    /**
     * Get the width from a type3 charproc stream.
     *
     * @return the glyph width.
     * @throws IOException if the stream could not be read, or did not have d0 or d1 as first
     *                     operator, or if their first argument was not a number.
     */
    public float getWidth() throws IOException {
        List<COSBase> arguments = new ArrayList<>();
        PDFStreamParser parser = new PDFStreamParser(this);
        Object token = parser.parseNextToken();
        while (token != null) {
            if (token instanceof Operator) {
                return parseWidth((Operator) token, arguments);
            } else {
                arguments.add((COSBase) token);
            }
            token = parser.parseNextToken();
        }
        throw new IOException("Unexpected end of stream");
    }

    private float parseWidth(Operator operator, List<COSBase> arguments) throws IOException {
        if (operator.getName().equals("d0") || operator.getName().equals("d1")) {
            COSBase obj = arguments.get(0);
            if (obj instanceof COSNumber) {
                return ((COSNumber) obj).floatValue();
            }
            throw new IOException("Unexpected argument type: " + obj.getClass().getName());
        } else {
            throw new IOException("First operator must be d0 or d1");
        }
    }
}
