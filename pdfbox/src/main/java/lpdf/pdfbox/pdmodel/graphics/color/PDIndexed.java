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
import lpdf.pdfbox.cos.COSInteger;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSNumber;
import lpdf.pdfbox.cos.COSStream;
import lpdf.pdfbox.cos.COSString;
import lpdf.pdfbox.pdmodel.PDResources;
import lpdf.pdfbox.pdmodel.common.PDStream;

import java.io.IOException;

/**
 * An Indexed colour space specifies that an area is to be painted using a colour table
 * of arbitrary colours from another color space.
 *
 * @author John Hewson
 * @author Ben Litchfield
 */
public final class PDIndexed extends PDSpecialColorSpace {
    private final PDColor initialColor = new PDColor(new float[]{0}, this);

    private PDColorSpace baseColorSpace = null;

    // cached lookup data
    private byte[] lookupData;
    private float[][] colorTable;
    private int actualMaxIndex;
    private int[][] rgbColorTable;

    /**
     * Creates a new Indexed color space.
     * Default DeviceRGB, hival 255.
     */
    public PDIndexed() {
        array = new COSArray();
        array.add(COSName.INDEXED);
        array.add(COSName.DEVICERGB);
        array.add(COSInteger.get(255));
        array.add(lpdf.pdfbox.cos.COSNull.NULL);
    }

    /**
     * Creates a new indexed color space from the given PDF array.
     *
     * @param indexedArray the array containing the indexed parameters
     * @throws IOException if the colorspace could not be created
     */
    public PDIndexed(COSArray indexedArray) throws IOException {
        this(indexedArray, null);
    }

    /**
     * Creates a new indexed color space from the given PDF array.
     *
     * @param indexedArray the array containing the indexed parameters
     * @param resources    the resources, can be null. Allows to use its cache for the colorspace.
     * @throws IOException if the colorspace could not be created
     */
    public PDIndexed(COSArray indexedArray, PDResources resources) throws IOException {
        array = indexedArray;
        // don't call getObject(1), we want to pass a reference if possible
        // to profit from caching (PDFBOX-4149)
        baseColorSpace = PDColorSpace.create(array.get(1), resources);
        readColorTable();
    }

    @Override
    public String getName() {
        return COSName.INDEXED.getName();
    }

    @Override
    public int getNumberOfComponents() {
        return 1;
    }

    @Override
    public float[] getDefaultDecode(int bitsPerComponent) {
        return new float[]{0, (float) Math.pow(2, bitsPerComponent) - 1};
    }

    @Override
    public PDColor getInitialColor() {
        return initialColor;
    }


    /**
     * Returns the base color space.
     *
     * @return the base color space.
     */
    public PDColorSpace getBaseColorSpace() {
        return baseColorSpace;
    }

    // returns "hival" array element
    private int getHival() {
        return ((COSNumber) array.getObject(2)).intValue();
    }

    // reads the lookup table data from the array
    private void readLookupData() throws IOException {
        if (lookupData == null) {
            COSBase lookupTable = array.getObject(3);
            if (lookupTable instanceof COSString) {
                lookupData = ((COSString) lookupTable).getBytes();
            } else if (lookupTable instanceof COSStream) {
                lookupData = new PDStream((COSStream) lookupTable).toByteArray();
            } else if (lookupTable == null) {
                lookupData = new byte[0];
            } else {
                throw new IOException("Error: Unknown type for lookup table " + lookupTable);
            }
        }
    }

    //
    // WARNING: this method is performance sensitive, modify with care!
    //
    private void readColorTable() throws IOException {
        readLookupData();

        int maxIndex = Math.min(getHival(), 255);
        int numComponents = baseColorSpace.getNumberOfComponents();

        // some tables are too short
        if (lookupData.length / numComponents < maxIndex + 1) {
            maxIndex = lookupData.length / numComponents - 1;
        }
        actualMaxIndex = maxIndex;  // TODO "actual" is ugly, tidy this up

        colorTable = new float[maxIndex + 1][numComponents];
        for (int i = 0, offset = 0; i <= maxIndex; i++) {
            for (int c = 0; c < numComponents; c++) {
                colorTable[i][c] = (lookupData[offset] & 0xff) / 255f;
                offset++;
            }
        }
    }

    /**
     * Sets the base color space.
     *
     * @param base the base color space
     */
    public void setBaseColorSpace(PDColorSpace base) {
        array.set(1, base.getCOSObject());
        baseColorSpace = base;
    }

    /**
     * Sets the highest value that is allowed. This cannot be higher than 255.
     *
     * @param high the highest value for the lookup table
     */
    public void setHighValue(int high) {
        array.set(2, high);
    }

    @Override
    public String toString() {
        return "Indexed{base:" + baseColorSpace + " " +
                "hival:" + getHival() + " " +
                "lookup:(" + colorTable.length + " entries)}";
    }
}
