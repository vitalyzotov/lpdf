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
import java.util.List;
import java.util.Map;

import lpdf.pdfbox.cos.COSArray;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSNull;
import lpdf.pdfbox.pdmodel.common.function.PDFunction;

/**
 * DeviceN colour spaces may contain an arbitrary number of colour components.
 * DeviceN represents a colour space containing multiple components that correspond to colorants
 * of some target device. As with Separation colour spaces, readers are able to approximate the
 * colorants if they are not available on the current output device, such as a display
 *
 * @author John Hewson
 * @author Ben Litchfield
 */
public class PDDeviceN extends PDSpecialColorSpace
{
    // array indexes
    private static final int COLORANT_NAMES = 1;
    private static final int ALTERNATE_CS = 2;
    private static final int TINT_TRANSFORM = 3;
    private static final int DEVICEN_ATTRIBUTES = 4;

    // fields
    private PDColorSpace alternateColorSpace = null;
    private PDFunction tintTransform = null;
    private PDDeviceNAttributes attributes;
    private PDColor initialColor;

    // color conversion cache
    private int numColorants;
    private int[] colorantToComponent;
    private PDColorSpace processColorSpace;
    private PDSeparation[] spotColorSpaces;

    /**
     * Creates a new DeviceN color space.
     */
    public PDDeviceN()
    {
        array = new COSArray();
        array.add(COSName.DEVICEN);

        // empty placeholder
        array.add(COSNull.NULL);
        array.add(COSNull.NULL);
        array.add(COSNull.NULL);
    }

    /**
     * Creates a new DeviceN color space from the given COS array.
     *
     * @param deviceN an array containing the color space information
     *
     * @throws IOException if the colorspace could not be created
     */
    public PDDeviceN(COSArray deviceN) throws IOException
    {
        array = deviceN;
        alternateColorSpace = PDColorSpace.create(array.getObject(ALTERNATE_CS));
        tintTransform = PDFunction.create(array.getObject(TINT_TRANSFORM));

        if (array.size() > DEVICEN_ATTRIBUTES)
        {
            attributes = new PDDeviceNAttributes((COSDictionary)array.getObject(DEVICEN_ATTRIBUTES));
        }
        initColorConversionCache();

        // set initial color space
        int n = getNumberOfComponents();
        float[] initial = new float[n];
        for (int i = 0; i < n; i++)
        {
            initial[i] = 1;
        }
        initialColor = new PDColor(initial, this);
    }

    // initializes the color conversion cache
    private void initColorConversionCache() throws IOException
    {
        // there's nothing to cache for non-attribute spaces
        if (attributes == null)
        {
            return;
        }

        // colorant names
        List<String> colorantNames = getColorantNames();
        numColorants = colorantNames.size();

        // process components
        colorantToComponent = new int[numColorants];
        if (attributes.getProcess() != null)
        {
            List<String> components = attributes.getProcess().getComponents();

            // map each colorant to the corresponding process component (if any)
            for (int c = 0; c < numColorants; c++)
            {
                colorantToComponent[c] = components.indexOf(colorantNames.get(c));
            }

            // process color space
            processColorSpace = attributes.getProcess().getColorSpace();
        }
        else
        {
            for (int c = 0; c < numColorants; c++)
            {
                colorantToComponent[c] = -1;
            }
        }

        // spot colorants
        spotColorSpaces = new PDSeparation[numColorants];

        // spot color spaces
        Map<String, PDSeparation> spotColorants = attributes.getColorants();

        // map each colorant to the corresponding spot color space
        for (int c = 0; c < numColorants; c++)
        {
            String name = colorantNames.get(c);
            PDSeparation spot = spotColorants.get(name);
            if (spot != null)
            {
                // spot colorant
                spotColorSpaces[c] = spot;

                // spot colors may replace process colors with same name
                // providing that the subtype is not NChannel.
                if (!isNChannel())
                {
                    colorantToComponent[c] = -1;
                }
            }
            else
            {
                // process colorant
                spotColorSpaces[c] = null;
            }
        }
    }

    /**
     * Returns true if this color space has the NChannel subtype.
     * @return true if subtype is NChannel
     */
    public boolean isNChannel()
    {
        return attributes != null && attributes.isNChannel();
    }

    @Override
    public String getName()
    {
        return COSName.DEVICEN.getName();
    }

    @Override
    public final int getNumberOfComponents()
    {
        return getColorantNames().size();
    }

    @Override
    public float[] getDefaultDecode(int bitsPerComponent)
    {
        int n = getNumberOfComponents();
        float[] decode = new float[n * 2];
        for (int i = 0; i < n; i++)
        {
            decode[i * 2 + 1] = 1;
        }
        return decode;
    }

    @Override
    public PDColor getInitialColor()
    {
        return initialColor;
    }

    /**
     * Returns the list of colorants.
     * @return the list of colorants
     */
    public List<String> getColorantNames()
    {
        return ((COSArray) array.getObject(COLORANT_NAMES)).toCOSNameStringList();
    }

    /**
     * Returns the attributes associated with the DeviceN color space.
     * @return the DeviceN attributes
     */
    public PDDeviceNAttributes getAttributes()
    {
        return attributes;
    }

    /**
     * Sets the list of colorants
     * @param names the list of colorants
     */
    public void setColorantNames(List<String> names)
    {
        COSArray namesArray = COSArray.ofCOSNames(names);
        array.set(COLORANT_NAMES, namesArray);
    }

    /**
     * Sets the color space attributes.
     * If null is passed in then all attribute will be removed.
     * @param attributes the color space attributes, or null
     */
    public void setAttributes(PDDeviceNAttributes attributes)
    {
        this.attributes = attributes;
        if (attributes == null)
        {
            array.remove(DEVICEN_ATTRIBUTES);
        }
        else
        {
            // make sure array is large enough
            while (array.size() <= DEVICEN_ATTRIBUTES)
            {
                array.add(COSNull.NULL);
            }
            array.set(DEVICEN_ATTRIBUTES, attributes.getCOSDictionary());
        }
    }

    /**
     * This will get the alternate color space for this separation.
     *
     * @return The alternate color space.
     *
     * @throws IOException If there is an error getting the alternate color
     * space.
     */
    public PDColorSpace getAlternateColorSpace() throws IOException
    {
        if (alternateColorSpace == null)
        {
            alternateColorSpace = PDColorSpace.create(array.getObject(ALTERNATE_CS));
        }
        return alternateColorSpace;
    }

    /**
     * This will set the alternate color space.
     *
     * @param cs The alternate color space.
     */
    public void setAlternateColorSpace(PDColorSpace cs)
    {
        alternateColorSpace = cs;
        COSBase space = null;
        if (cs != null)
        {
            space = cs.getCOSObject();
        }
        array.set(ALTERNATE_CS, space);
    }

    /**
     * This will get the tint transform function.
     *
     * @return The tint transform function.
     *
     * @throws IOException if there is an error creating the function.
     */
    public PDFunction getTintTransform() throws IOException
    {
        if (tintTransform == null)
        {
            tintTransform = PDFunction.create(array.getObject(TINT_TRANSFORM));
        }
        return tintTransform;
    }

    /**
     * This will set the tint transform function.
     *
     * @param tint The tint transform function.
     */
    public void setTintTransform(PDFunction tint)
    {
        tintTransform = tint;
        array.set(TINT_TRANSFORM, tint);
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getName());
        sb.append('{');
        for (String col : getColorantNames())
        {
            sb.append('\"');
            sb.append(col);
            sb.append("\" ");
        }
        sb.append(alternateColorSpace.getName());
        sb.append(' ');
        sb.append(tintTransform);
        sb.append(' ');
        if (attributes != null)
        {
            sb.append(attributes);
        }
        sb.append('}');
        return sb.toString();
    }
}
