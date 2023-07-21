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
package org.apache.pdfbox.pdmodel.graphics.color;

import org.apache.pdfbox.cos.COSBase;

import java.io.IOException;

/**
 * A color space embedded in a JPX file.
 * This wraps the AWT ColorSpace which is obtained after JAI Image I/O reads a JPX stream.
 *
 * @author John Hewson
 */
public final class PDJPXColorSpace extends PDColorSpace
{


    /**
     * Creates a new JPX color space from the given AWT color space.
     */
    public PDJPXColorSpace()
    {
    }

    @Override
    public String getName()
    {
        return "JPX";
    }

    @Override
    public int getNumberOfComponents()
    {
        throw new UnsupportedOperationException("Not implemented"); //todo: vz fixme
        //return awtColorSpace.getNumComponents();
    }

    @Override
    public float[] getDefaultDecode(int bitsPerComponent)
    {
        throw new UnsupportedOperationException("Not implemented"); //todo: vz fixme
        /*
        int n = getNumberOfComponents();
        float[] decode = new float[n * 2];
        for (int i = 0; i < n; i++)
        {
            decode[i * 2] = awtColorSpace.getMinValue(i);
            decode[i * 2 + 1] = awtColorSpace.getMaxValue(i);
        }
        return decode;

         */
    }

    @Override
    public PDColor getInitialColor()
    {
        throw new UnsupportedOperationException("JPX color spaces don't support drawing");
    }

    @Override
    public COSBase getCOSObject()
    {
        throw new UnsupportedOperationException("JPX color spaces don't have COS objects");
    }
}
