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


import lpdf.pdfbox.cos.COSName;

/**
 * Colours in the DeviceRGB colour space are specified according to the additive
 * RGB (red-green-blue) colour model.
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
public final class PDDeviceRGB extends PDDeviceColorSpace {
    /**
     * This is the single instance of this class.
     */
    public static final PDDeviceRGB INSTANCE = new PDDeviceRGB();

    private final PDColor initialColor = new PDColor(new float[]{0, 0, 0}, this);

    private PDDeviceRGB() {
    }

    @Override
    public String getName() {
        return COSName.DEVICERGB.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfComponents() {
        return 3;
    }

    @Override
    public float[] getDefaultDecode(int bitsPerComponent) {
        return new float[]{0, 1, 0, 1, 0, 1};
    }

    @Override
    public PDColor getInitialColor() {
        return initialColor;
    }

    @Override
    public float[] toRGB(float[] value) {
        return value;
    }

}
