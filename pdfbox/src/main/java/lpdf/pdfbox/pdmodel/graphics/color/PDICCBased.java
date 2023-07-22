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
package lpdf.pdfbox.pdmodel.graphics.color;

import lpdf.pdfbox.cos.COSArray;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSFloat;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSObject;
import lpdf.pdfbox.cos.COSStream;
import lpdf.pdfbox.pdmodel.PDDocument;
import lpdf.pdfbox.pdmodel.PDResources;
import lpdf.pdfbox.pdmodel.ResourceCache;
import lpdf.pdfbox.pdmodel.common.PDRange;
import lpdf.pdfbox.pdmodel.common.PDStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * ICCBased color spaces are based on a cross-platform color profile as defined by the
 * International Color Consortium (ICC).
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
public final class PDICCBased extends PDCIEBasedColorSpace {
    private static final Logger LOG = LoggerFactory.getLogger(PDICCBased.class);

    private final PDStream stream;
    private int numberOfComponents = -1;

    private PDColorSpace alternateColorSpace;

    private PDColor initialColor;
    private boolean isRGB = false;
    // allows to force using alternate color space instead of ICC color space for performance
    // reasons with LittleCMS (LCMS), see PDFBOX-4309
    // WARNING: do not activate this in a conforming reader
    private boolean useOnlyAlternateColorSpace = false;
    private static final boolean IS_KCMS;

    static {
        String cmmProperty = System.getProperty("sun.java2d.cmm");
        boolean result = false;
        if ("sun.java2d.cmm.kcms.KcmsServiceProvider".equals(cmmProperty)) {
            try {
                Class.forName("sun.java2d.cmm.kcms.KcmsServiceProvider");
                result = true;
            } catch (ClassNotFoundException e) {
                // KCMS not available
            }
        }
        // else maybe KCMS was available, but not wished
        IS_KCMS = result;
    }

    /**
     * Creates a new ICC color space with an empty stream.
     *
     * @param doc the document to store the ICC data
     */
    public PDICCBased(PDDocument doc) {
        array = new COSArray();
        array.add(COSName.ICCBASED);
        stream = new PDStream(doc);
        array.add(stream);
    }

    /**
     * Creates a new ICC color space using the PDF array.
     *
     * @param iccArray the ICC stream object.
     * @throws IOException if there is an error reading the ICC profile or if the parameter is
     *                     invalid.
     */
    private PDICCBased(COSArray iccArray) throws IOException {
        useOnlyAlternateColorSpace = System
                .getProperty("lpdf.pdfbox.rendering.UseAlternateInsteadOfICCColorSpace") != null;
        array = iccArray;
        stream = new PDStream((COSStream) iccArray.getObject(1));

    }

    /**
     * Creates a new ICC color space using the PDF array, optionally using a resource cache.
     *
     * @param iccArray  the ICC stream object.
     * @param resources resources to use as cache, or null for no caching.
     * @return an ICC color space.
     * @throws IOException if there is an error reading the ICC profile or if the parameter is
     *                     invalid.
     */
    public static PDICCBased create(COSArray iccArray, PDResources resources) throws IOException {
        checkArray(iccArray);
        COSBase base = iccArray.get(1);
        if (base instanceof COSObject && resources != null) {
            ResourceCache resourceCache = resources.getResourceCache();
            if (resourceCache != null) {
                COSObject indirect = (COSObject) base;
                PDColorSpace space = resourceCache.getColorSpace(indirect);
                if (space instanceof PDICCBased) {
                    return (PDICCBased) space;
                } else {
                    PDICCBased newSpace = new PDICCBased(iccArray);
                    resourceCache.put(indirect, newSpace);
                    return newSpace;
                }
            }
        }
        return new PDICCBased(iccArray);
    }

    private static void checkArray(COSArray iccArray) throws IOException {
        if (iccArray.size() < 2) {
            throw new IOException("ICCBased colorspace array must have two elements");
        }
        if (!(iccArray.getObject(1) instanceof COSStream)) {
            throw new IOException("ICCBased colorspace array must have a stream as second element");
        }
    }

    @Override
    public String getName() {
        return COSName.ICCBASED.getName();
    }

    /**
     * Get the underlying ICC profile stream.
     *
     * @return the underlying ICC profile stream
     */
    public PDStream getPDStream() {
        return stream;
    }


    private static void intToBigEndian(int value, byte[] array, int index) {
        array[index] = (byte) (value >> 24);
        array[index + 1] = (byte) (value >> 16);
        array[index + 2] = (byte) (value >> 8);
        array[index + 3] = (byte) (value);
    }


    @Override
    public int getNumberOfComponents() {
        if (numberOfComponents < 0) {
            numberOfComponents = stream.getCOSObject().getInt(COSName.N);
        }
        return numberOfComponents;
    }

    @Override
    public float[] getDefaultDecode(int bitsPerComponent) {
        return alternateColorSpace.getDefaultDecode(bitsPerComponent);
    }

    @Override
    public PDColor getInitialColor() {
        return initialColor;
    }

    /**
     * Returns a list of alternate color spaces for non-conforming readers.
     * WARNING: Do not use the information in a conforming reader.
     *
     * @return A list of alternateColorSpace color spaces.
     * @throws IOException If there is an error getting the alternateColorSpace color spaces.
     */
    public PDColorSpace getAlternateColorSpace() throws IOException {
        COSBase alternate = stream.getCOSObject().getDictionaryObject(COSName.ALTERNATE);
        COSArray alternateArray;
        if (alternate == null) {
            alternateArray = new COSArray();
            int numComponents = getNumberOfComponents();
            COSName csName;
            switch (numComponents) {
                case 1:
                    csName = COSName.DEVICEGRAY;
                    break;
                case 3:
                    csName = COSName.DEVICERGB;
                    break;
                case 4:
                    csName = COSName.DEVICECMYK;
                    break;
                default:
                    throw new IOException("Unknown color space number of components:" + numComponents);
            }
            alternateArray.add(csName);
        } else {
            if (alternate instanceof COSArray) {
                alternateArray = (COSArray) alternate;
            } else if (alternate instanceof COSName) {
                alternateArray = new COSArray();
                alternateArray.add(alternate);
            } else {
                throw new IOException("Error: expected COSArray or COSName and not " +
                        alternate.getClass().getName());
            }
        }
        return PDColorSpace.create(alternateArray);
    }

    /**
     * Returns the range for a certain component number.
     * This will never return null.
     * If it is not present then the range 0..1 will be returned.
     *
     * @param n the component number to get the range for
     * @return the range for this component
     */
    public PDRange getRangeForComponent(int n) {
        COSArray rangeArray = stream.getCOSObject().getCOSArray(COSName.RANGE);
        if (rangeArray == null || rangeArray.size() < getNumberOfComponents() * 2) {
            return new PDRange(); // 0..1
        }
        return new PDRange(rangeArray, n);
    }

    /**
     * Returns the metadata stream for this object, or null if there is no metadata stream.
     *
     * @return the metadata stream, or null if there is none
     */
    public COSStream getMetadata() {
        return stream.getCOSObject().getCOSStream(COSName.METADATA);
    }


    /**
     * Sets the list of alternateColorSpace color spaces.
     *
     * @param list the list of color space objects
     */
    public void setAlternateColorSpaces(List<PDColorSpace> list) {
        COSArray altArray = null;
        if (list != null) {
            altArray = new COSArray(list);
        }
        stream.getCOSObject().setItem(COSName.ALTERNATE, altArray);
    }

    /**
     * Sets the range for this color space.
     *
     * @param range the new range for the a component
     * @param n     the component to set the range for
     */
    public void setRangeForComponent(PDRange range, int n) {
        COSArray rangeArray = stream.getCOSObject().getCOSArray(COSName.RANGE);
        if (rangeArray == null) {
            rangeArray = new COSArray();
            stream.getCOSObject().setItem(COSName.RANGE, rangeArray);
        }
        // extend range array with default values if needed
        while (rangeArray.size() < (n + 1) * 2) {
            rangeArray.add(new COSFloat(0));
            rangeArray.add(new COSFloat(1));
        }
        rangeArray.set(n * 2, new COSFloat(range.getMin()));
        rangeArray.set(n * 2 + 1, new COSFloat(range.getMax()));
    }

    /**
     * Sets the metadata stream that is associated with this color space.
     *
     * @param metadata the new metadata stream
     */
    public void setMetadata(COSStream metadata) {
        stream.getCOSObject().setItem(COSName.METADATA, metadata);
    }

    /**
     * Internal accessor to support indexed raw images.
     *
     * @return true if this colorspace is sRGB.
     */
    boolean isSRGB() {
        return isRGB;
    }

    @Override
    public String toString() {
        return getName() + "{numberOfComponents: " + getNumberOfComponents() + "}";
    }
}
