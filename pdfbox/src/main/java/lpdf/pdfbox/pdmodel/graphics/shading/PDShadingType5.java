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
package lpdf.pdfbox.pdmodel.graphics.shading;

import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;

/**
 * Resources for a shading type 5 (Lattice-Form Gouraud-Shade Triangle Mesh).
 */
public class PDShadingType5 extends PDTriangleBasedShadingType {
    /**
     * Constructor using the given shading dictionary.
     *
     * @param shadingDictionary the dictionary for this shading
     */
    public PDShadingType5(COSDictionary shadingDictionary) {
        super(shadingDictionary);
    }

    @Override
    public int getShadingType() {
        return PDShading.SHADING_TYPE5;
    }

    /**
     * The vertices per row of this shading. This will return -1 if one has not
     * been set.
     *
     * @return the number of vertices per row
     */
    public int getVerticesPerRow() {
        return getCOSObject().getInt(COSName.VERTICES_PER_ROW, -1);
    }

}
