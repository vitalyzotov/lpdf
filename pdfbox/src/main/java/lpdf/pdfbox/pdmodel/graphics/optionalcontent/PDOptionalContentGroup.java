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
package lpdf.pdfbox.pdmodel.graphics.optionalcontent;

import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;

/**
 * An optional content group (OCG).
 */
public class PDOptionalContentGroup extends PDPropertyList {
    /**
     * Creates a new optional content group (OCG).
     *
     * @param name the name of the content group
     */
    public PDOptionalContentGroup(String name) {
        this.dict.setItem(COSName.TYPE, COSName.OCG);
        setName(name);
    }

    /**
     * Creates a new instance based on a given {@link COSDictionary}.
     *
     * @param dict the dictionary
     */
    public PDOptionalContentGroup(COSDictionary dict) {
        super(dict);
        if (!dict.getItem(COSName.TYPE).equals(COSName.OCG)) {
            throw new IllegalArgumentException(
                    "Provided dictionary is not of type '" + COSName.OCG + "'");
        }
    }

    /**
     * Enumeration for the renderState dictionary entry on the "Export", "View"
     * and "Print" dictionary.
     */
    public enum RenderState {
        /**
         * The "ON" value.
         */
        ON(COSName.ON),
        /**
         * The "OFF" value.
         */
        OFF(COSName.OFF);

        private final COSName name;

        private RenderState(COSName value) {
            this.name = value;
        }

        /**
         * Returns the base state represented by the given {@link COSName}.
         *
         * @param state the state name
         * @return the state enum value
         */
        public static RenderState valueOf(COSName state) {
            if (state == null) {
                return null;
            }
            return RenderState.valueOf(state.getName().toUpperCase());
        }

        /**
         * Returns the PDF name for the state.
         *
         * @return the name of the state
         */
        public COSName getName() {
            return this.name;
        }
    }

    /**
     * Returns the name of the optional content group.
     *
     * @return the name
     */
    public String getName() {
        return dict.getString(COSName.NAME);
    }

    /**
     * Sets the name of the optional content group.
     *
     * @param name the name
     */
    public final void setName(String name) {
        dict.setString(COSName.NAME, name);
    }


    @Override
    public String toString() {
        return super.toString() + " (" + getName() + ")";
    }
}
