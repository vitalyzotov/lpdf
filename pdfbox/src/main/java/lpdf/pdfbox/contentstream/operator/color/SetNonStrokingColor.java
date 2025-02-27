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
package lpdf.pdfbox.contentstream.operator.color;

import lpdf.pdfbox.contentstream.PDFStreamEngine;
import lpdf.pdfbox.contentstream.operator.OperatorName;
import lpdf.pdfbox.pdmodel.graphics.color.PDColor;
import lpdf.pdfbox.pdmodel.graphics.color.PDColorSpace;

/**
 * sc: Sets the colour to use for non-stroking operations.
 *
 * @author John Hewson
 */
public class SetNonStrokingColor extends SetColor {
    public SetNonStrokingColor(PDFStreamEngine context) {
        super(context);
    }

    /**
     * Returns the non-stroking color.
     *
     * @return The non-stroking color.
     */
    @Override
    protected PDColor getColor() {
        return getContext().getGraphicsState().getNonStrokingColor();
    }

    /**
     * Sets the non-stroking color.
     *
     * @param color The new non-stroking color.
     */
    @Override
    protected void setColor(PDColor color) {
        getContext().getGraphicsState().setNonStrokingColor(color);
    }

    /**
     * Returns the non-stroking color space.
     *
     * @return The non-stroking color space.
     */
    @Override
    protected PDColorSpace getColorSpace() {
        return getContext().getGraphicsState().getNonStrokingColorSpace();
    }

    @Override
    public String getName() {
        return OperatorName.NON_STROKING_COLOR;
    }
}
