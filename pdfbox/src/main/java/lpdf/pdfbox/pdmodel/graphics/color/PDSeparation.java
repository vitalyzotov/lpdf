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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lpdf.pdfbox.cos.COSArray;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSNull;
import lpdf.pdfbox.pdmodel.common.function.PDFunction;

/**
 * A Separation color space used to specify either additional colorants or for isolating the
 * control of individual colour components of a device colour space for a subtractive device.
 * When such a space is the current colour space, the current colour shall be a single-component
 * value, called a tint, that controls the given colorant or colour components only.
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
public class PDSeparation extends PDSpecialColorSpace
{
    private final PDColor initialColor = new PDColor(new float[] { 1 }, this);

    // array indexes
    private static final int COLORANT_NAMES = 1;
    private static final int ALTERNATE_CS = 2;
    private static final int TINT_TRANSFORM = 3;

    // fields
    private PDColorSpace alternateColorSpace = null;
    private PDFunction tintTransform = null;

    /**
     * Creates a new Separation color space.
     */
    public PDSeparation()
    {
        array = new COSArray();
        array.add(COSName.SEPARATION);
        array.add(COSName.getPDFName(""));
        // add some placeholder
        array.add(COSNull.NULL);
        array.add(COSNull.NULL);
    }

    /**
     * Creates a new Separation color space from a PDF color space array.
     * @param separation an array containing all separation information.
     * @throws IOException if the color space or the function could not be created.
     */
    public PDSeparation(COSArray separation) throws IOException
    {
        array = separation;
        alternateColorSpace = PDColorSpace.create(array.getObject(ALTERNATE_CS));
        tintTransform = PDFunction.create(array.getObject(TINT_TRANSFORM));
        int numberOfOutputParameters = tintTransform.getNumberOfOutputParameters();
        if (numberOfOutputParameters > 0 &&
                numberOfOutputParameters < alternateColorSpace.getNumberOfComponents())
        {
            throw new IOException("The tint transform function has less output parameters (" +
                    tintTransform.getNumberOfOutputParameters() + ") than the alternate colorspace " +
                    alternateColorSpace + " (" + alternateColorSpace.getNumberOfComponents() + ")");
        }
    }

    @Override
    public String getName()
    {
        return COSName.SEPARATION.getName();
    }

    @Override
    public int getNumberOfComponents()
    {
        return 1;
    }

    @Override
    public float[] getDefaultDecode(int bitsPerComponent)
    {
        return new float[] { 0, 1 };
    }

    @Override
    public PDColor getInitialColor()
    {
        return initialColor;
    }


    protected void tintTransform(float[] samples, int[] alt) throws IOException
    {
        samples[0] /= 255; // 0..1
        float[] result = tintTransform.eval(samples);
        for (int s = 0; s < alt.length; s++)
        {
            // scale to 0..255
            alt[s] = (int) (result[s] * 255);
        }
    }

    /**
     * Returns the colorant name.
     * @return the name of the colorant
     */
    public PDColorSpace getAlternateColorSpace()
    {
       return alternateColorSpace;
    }

    /**
     * Returns the colorant name.
     * @return the name of the colorant
     */
    public String getColorantName()
    {
        COSName name = (COSName)array.getObject(COLORANT_NAMES);
        return name.getName();
    }

    /**
     * Sets the colorant name.
     * @param name the name of the colorant
     */
    public void setColorantName(String name)
    {
        array.set(1, COSName.getPDFName(name));
    }

    /**
     * Sets the alternate color space.
     * @param colorSpace The alternate color space.
     */
    public void setAlternateColorSpace(PDColorSpace colorSpace)
    {
        alternateColorSpace = colorSpace;
        COSBase space = null;
        if (colorSpace != null)
        {
            space = colorSpace.getCOSObject();
        }
        array.set(ALTERNATE_CS, space);
    }

    /**
     * Sets the tint transform function.
     * @param tint the tint transform function
     */
    public void setTintTransform(PDFunction tint)
    {
        tintTransform = tint;
        array.set(TINT_TRANSFORM, tint);
    }

    @Override
    public String toString()
    {
        return getName() + "{" +
                "\"" + getColorantName() + "\"" + " " +
                alternateColorSpace.getName() + " " +
                tintTransform + "}";
    }
}
