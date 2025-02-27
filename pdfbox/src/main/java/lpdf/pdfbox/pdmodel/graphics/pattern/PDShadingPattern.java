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
package lpdf.pdfbox.pdmodel.graphics.pattern;

import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.pdmodel.graphics.shading.PDShading;
import lpdf.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.io.IOException;

/**
 * A shading pattern dictionary.
 */
public class PDShadingPattern extends PDAbstractPattern {
    private PDExtendedGraphicsState extendedGraphicsState;
    private PDShading shading;

    /**
     * Creates a new shading pattern.
     */
    public PDShadingPattern() {
        getCOSObject().setInt(COSName.PATTERN_TYPE, PDAbstractPattern.TYPE_SHADING_PATTERN);
    }

    /**
     * Creates a new shading pattern from the given COS dictionary.
     *
     * @param resourceDictionary The COSDictionary for this pattern resource.
     */
    public PDShadingPattern(COSDictionary resourceDictionary) {
        super(resourceDictionary);
    }

    @Override
    public int getPatternType() {
        return PDAbstractPattern.TYPE_SHADING_PATTERN;
    }

    /**
     * This will get the external graphics state for this pattern.
     *
     * @return The extended graphics state for this pattern.
     */
    public PDExtendedGraphicsState getExtendedGraphicsState() {
        if (extendedGraphicsState == null) {
            COSDictionary base = getCOSObject().getCOSDictionary(COSName.EXT_G_STATE);
            if (base != null) {
                extendedGraphicsState = new PDExtendedGraphicsState(base);
            }
        }
        return extendedGraphicsState;
    }

    /**
     * This will set the external graphics state for this pattern.
     *
     * @param extendedGraphicsState The new extended graphics state for this pattern.
     */
    public void setExtendedGraphicsState(PDExtendedGraphicsState extendedGraphicsState) {
        this.extendedGraphicsState = extendedGraphicsState;
        getCOSObject().setItem(COSName.EXT_G_STATE, extendedGraphicsState);
    }

    /**
     * This will get the shading resources for this pattern.
     *
     * @return The shading resources for this pattern.
     * @throws IOException if something went wrong
     */
    public PDShading getShading() throws IOException {
        if (shading == null) {
            COSDictionary base = getCOSObject().getCOSDictionary(COSName.SHADING);
            if (base != null) {
                shading = PDShading.create(base);
            }
        }
        return shading;
    }

    /**
     * This will set the shading resources for this pattern.
     *
     * @param shadingResources The new shading resources for this pattern.
     */
    public void setShading(PDShading shadingResources) {
        shading = shadingResources;
        getCOSObject().setItem(COSName.SHADING, shadingResources);
    }
}
