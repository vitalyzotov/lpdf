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

package lpdf.pdfbox.pdmodel.interactive.action;

import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.pdmodel.common.filespecification.PDFileSpecification;

import java.io.IOException;

/**
 * @author Timur Kamalov
 */
public class PDActionImportData extends PDAction {

    /**
     * This type of action this object represents.
     */
    public static final String SUB_TYPE = "ImportData";

    /**
     * Default constructor.
     */
    public PDActionImportData() {
        setSubType(SUB_TYPE);
    }

    /**
     * Constructor.
     *
     * @param a The action dictionary.
     */
    public PDActionImportData(COSDictionary a) {
        super(a);
    }

    /**
     * This will get the file in which the destination is located.
     *
     * @return The F entry of the specific Submit-From action dictionary.
     * @throws IOException If there is an error creating the file spec.
     */
    public PDFileSpecification getFile() throws IOException {
        return PDFileSpecification.createFS(action.getDictionaryObject(COSName.F));
    }

    /**
     * This will set the file in which the destination is located.
     *
     * @param fs The file specification.
     */
    public void setFile(PDFileSpecification fs) {
        action.setItem(COSName.F, fs);
    }

}
