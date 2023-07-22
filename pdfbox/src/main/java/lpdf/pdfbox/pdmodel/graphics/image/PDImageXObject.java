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
package lpdf.pdfbox.pdmodel.graphics.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lpdf.pdfbox.cos.COSArray;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSInputStream;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSObject;
import lpdf.pdfbox.cos.COSStream;
import lpdf.pdfbox.filter.DecodeOptions;
import lpdf.pdfbox.filter.DecodeResult;
import lpdf.io.IOUtils;
import lpdf.pdfbox.pdmodel.PDDocument;
import lpdf.pdfbox.pdmodel.PDResources;
import lpdf.pdfbox.pdmodel.common.PDMetadata;
import lpdf.pdfbox.pdmodel.common.PDStream;
import lpdf.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import lpdf.pdfbox.pdmodel.graphics.PDXObject;
import lpdf.pdfbox.pdmodel.graphics.color.PDColorSpace;
import lpdf.pdfbox.pdmodel.graphics.color.PDDeviceGray;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * An Image XObject.
 *
 * @author John Hewson
 * @author Ben Litchfield
 */
public final class PDImageXObject extends PDXObject implements PDImage {
    /**
     * Log instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PDImageXObject.class);

    private PDColorSpace colorSpace;

    // initialize to MAX_VALUE as we prefer lower subsampling when keeping/replacing cache.
    private int cachedImageSubsampling = Integer.MAX_VALUE;

    /**
     * current resource dictionary (has color spaces)
     */
    private final PDResources resources;

    /**
     * Creates an Image XObject in the given document. This constructor is for internal PDFBox use
     * and is not for PDF generation.
     * }.
     *
     * @param document the current document
     * @throws java.io.IOException if there is an error creating the XObject.
     */
    public PDImageXObject(PDDocument document) throws IOException {
        this(new PDStream(document), null);
    }

    /**
     * Creates an Image XObject in the given document using the given filtered stream. This
     * constructor is for internal PDFBox use and is not for PDF generation.
     *
     * @param document         the current document
     * @param encodedStream    an encoded stream of image data
     * @param cosFilter        the filter or a COSArray of filters
     * @param width            the image width
     * @param height           the image height
     * @param bitsPerComponent the bits per component
     * @param initColorSpace   the color space
     * @throws IOException if there is an error creating the XObject.
     */
    public PDImageXObject(PDDocument document, InputStream encodedStream,
                          COSBase cosFilter, int width, int height, int bitsPerComponent,
                          PDColorSpace initColorSpace) throws IOException {
        super(createRawStream(document, encodedStream), COSName.IMAGE);
        getCOSObject().setItem(COSName.FILTER, cosFilter);
        resources = null;
        colorSpace = null;
        setBitsPerComponent(bitsPerComponent);
        setWidth(width);
        setHeight(height);
        setColorSpace(initColorSpace);
    }

    /**
     * Creates an Image XObject with the given stream as its contents and current color spaces. This
     * constructor is for internal PDFBox use and is not for PDF generation.
     *
     * @param stream    the XObject stream to read
     * @param resources the current resources
     * @throws java.io.IOException if there is an error creating the XObject.
     */
    public PDImageXObject(PDStream stream, PDResources resources) throws IOException {
        super(stream, COSName.IMAGE);
        this.resources = resources;
        List<COSName> filters = stream.getFilters();
        if (!filters.isEmpty() && COSName.JPX_DECODE.equals(filters.get(filters.size() - 1))) {
            try (COSInputStream is = stream.createInputStream()) {
                DecodeResult decodeResult = is.getDecodeResult();
                stream.getCOSObject().addAll(decodeResult.getParameters());
            }
        }
    }

    /**
     * Creates a thumbnail Image XObject from the given COSBase and name.
     *
     * @param cosStream the COS stream
     * @return an XObject
     * @throws IOException if there is an error creating the XObject.
     */
    public static PDImageXObject createThumbnail(COSStream cosStream) throws IOException {
        // thumbnails are special, any non-null subtype is treated as being "Image"
        PDStream pdStream = new PDStream(cosStream);
        return new PDImageXObject(pdStream, null);
    }

    /**
     * Creates a COS stream from raw (encoded) data.
     */
    private static COSStream createRawStream(PDDocument document, InputStream rawInput)
            throws IOException {
        COSStream stream = document.getDocument().createCOSStream();
        try (OutputStream output = stream.createRawOutputStream()) {
            IOUtils.copy(rawInput, output);
        }
        return stream;
    }

    /**
     * Returns the metadata associated with this XObject, or null if there is none.
     *
     * @return the metadata associated with this object.
     */
    public PDMetadata getMetadata() {
        COSStream cosStream = getCOSObject().getCOSStream(COSName.METADATA);
        if (cosStream != null) {
            return new PDMetadata(cosStream);
        }
        return null;
    }

    /**
     * Sets the metadata associated with this XObject, or null if there is none.
     *
     * @param meta the metadata associated with this object
     */
    public void setMetadata(PDMetadata meta) {
        getCOSObject().setItem(COSName.METADATA, meta);
    }

    /**
     * Returns the key of this XObject in the structural parent tree.
     *
     * @return this object's key the structural parent tree or -1 if there isn't any.
     */
    public int getStructParent() {
        return getCOSObject().getInt(COSName.STRUCT_PARENT);
    }

    /**
     * Sets the key of this XObject in the structural parent tree.
     *
     * @param key the new key for this XObject
     */
    public void setStructParent(int key) {
        getCOSObject().setInt(COSName.STRUCT_PARENT, key);
    }

    /**
     * Returns the Mask Image XObject associated with this image, or null if there is none.
     *
     * @return Mask Image XObject
     * @throws java.io.IOException if the mask data could not be read
     */
    public PDImageXObject getMask() throws IOException {
        COSArray mask = getCOSObject().getCOSArray(COSName.MASK);
        if (mask != null) {
            // color key mask, no explicit mask to return
            return null;
        } else {
            COSStream cosStream = getCOSObject().getCOSStream(COSName.MASK);
            if (cosStream != null) {
                // always DeviceGray
                return new PDImageXObject(new PDStream(cosStream), null);
            }
            return null;
        }
    }

    /**
     * Returns the color key mask array associated with this image, or null if there is none.
     *
     * @return Mask Image XObject
     */
    public COSArray getColorKeyMask() {
        return getCOSObject().getCOSArray(COSName.MASK);
    }

    /**
     * Returns the Soft Mask Image XObject associated with this image, or null if there is none.
     *
     * @return the SMask Image XObject, or null.
     * @throws java.io.IOException if the soft mask data could not be read
     */
    public PDImageXObject getSoftMask() throws IOException {
        COSStream cosStream = getCOSObject().getCOSStream(COSName.SMASK);
        if (cosStream != null) {
            // always DeviceGray
            return new PDImageXObject(new PDStream(cosStream), null);
        }
        return null;
    }

    @Override
    public int getBitsPerComponent() {
        if (isStencil()) {
            return 1;
        } else {
            return getCOSObject().getInt(COSName.BITS_PER_COMPONENT, COSName.BPC);
        }
    }

    @Override
    public void setBitsPerComponent(int bpc) {
        getCOSObject().setInt(COSName.BITS_PER_COMPONENT, bpc);
    }

    @Override
    public PDColorSpace getColorSpace() throws IOException {
        if (colorSpace == null) {
            COSBase cosBase = getCOSObject().getItem(COSName.COLORSPACE, COSName.CS);
            if (cosBase != null) {
                COSObject indirect = null;
                if (cosBase instanceof COSObject &&
                        resources != null && resources.getResourceCache() != null) {
                    // PDFBOX-4022: use the resource cache because several images
                    // might have the same colorspace indirect object.
                    indirect = (COSObject) cosBase;
                    colorSpace = resources.getResourceCache().getColorSpace(indirect);
                    if (colorSpace != null) {
                        return colorSpace;
                    }
                }
                colorSpace = PDColorSpace.create(cosBase, resources);
                if (indirect != null) {
                    resources.getResourceCache().put(indirect, colorSpace);
                }
            } else if (isStencil()) {
                // stencil mask color space must be gray, it is often missing
                return PDDeviceGray.INSTANCE;
            } else {
                // an image without a color space is always broken
                throw new IOException("could not determine color space");
            }
        }
        return colorSpace;
    }

    @Override
    public InputStream createInputStream() throws IOException {
        return getStream().createInputStream();
    }

    @Override
    public InputStream createInputStream(DecodeOptions options) throws IOException {
        return getStream().createInputStream(options);
    }

    @Override
    public InputStream createInputStream(List<String> stopFilters) throws IOException {
        return getStream().createInputStream(stopFilters);
    }

    @Override
    public boolean isEmpty() {
        return getStream().getCOSObject().getLength() == 0;
    }

    @Override
    public void setColorSpace(PDColorSpace cs) {
        getCOSObject().setItem(COSName.COLORSPACE, cs != null ? cs.getCOSObject() : null);
        colorSpace = null;
    }

    @Override
    public int getHeight() {
        return getCOSObject().getInt(COSName.HEIGHT);
    }

    @Override
    public void setHeight(int h) {
        getCOSObject().setInt(COSName.HEIGHT, h);
    }

    @Override
    public int getWidth() {
        return getCOSObject().getInt(COSName.WIDTH);
    }

    @Override
    public void setWidth(int w) {
        getCOSObject().setInt(COSName.WIDTH, w);
    }

    @Override
    public boolean getInterpolate() {
        return getCOSObject().getBoolean(COSName.INTERPOLATE, false);
    }

    @Override
    public void setInterpolate(boolean value) {
        getCOSObject().setBoolean(COSName.INTERPOLATE, value);
    }

    @Override
    public void setDecode(COSArray decode) {
        getCOSObject().setItem(COSName.DECODE, decode);
    }

    @Override
    public COSArray getDecode() {
        return getCOSObject().getCOSArray(COSName.DECODE);
    }

    @Override
    public boolean isStencil() {
        return getCOSObject().getBoolean(COSName.IMAGE_MASK, false);
    }

    @Override
    public void setStencil(boolean isStencil) {
        getCOSObject().setBoolean(COSName.IMAGE_MASK, isStencil);
    }

    /**
     * This will get the suffix for this image type, e.g. jpg/png.
     *
     * @return The image suffix or null if not available.
     */
    @Override
    public String getSuffix() {
        List<COSName> filters = getStream().getFilters();

        if (filters.isEmpty()) {
            return "png";
        } else if (filters.contains(COSName.DCT_DECODE)) {
            return "jpg";
        } else if (filters.contains(COSName.JPX_DECODE)) {
            return "jpx";
        } else if (filters.contains(COSName.CCITTFAX_DECODE)) {
            return "tiff";
        } else if (filters.contains(COSName.FLATE_DECODE)
                || filters.contains(COSName.LZW_DECODE)
                || filters.contains(COSName.RUN_LENGTH_DECODE)) {
            return "png";
        } else if (filters.contains(COSName.JBIG2_DECODE)) {
            return "jb2";
        } else {
            LOG.warn("getSuffix() returns null, filters: " + filters);
            return null;
        }
    }

    /**
     * This will get the optional content group or optional content membership dictionary.
     *
     * @return The optional content group or optional content membership dictionary or null if there
     * is none.
     */
    public PDPropertyList getOptionalContent() {
        COSDictionary optionalContent = getCOSObject().getCOSDictionary(COSName.OC);
        return optionalContent != null ? PDPropertyList.create(optionalContent) : null;
    }

    /**
     * Sets the optional content group or optional content membership dictionary.
     *
     * @param oc The optional content group or optional content membership dictionary.
     */
    public void setOptionalContent(PDPropertyList oc) {
        getCOSObject().setItem(COSName.OC, oc);
    }
}
