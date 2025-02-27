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


import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSStream;
import lpdf.pdfbox.pdmodel.PDDocument;
import lpdf.pdfbox.pdmodel.common.COSObjectable;

import java.io.IOException;
import java.io.InputStream;

/**
 * An Output Intent describes the colour reproduction characteristics of a possible output
 * device or production condition.
 * Output intents provide a means for matching the colour characteristics of a PDF document with
 * those of a target output device or production environment in which the document will be printed.
 *
 * @author Guillaume Bailleul
 */
public final class PDOutputIntent implements COSObjectable {
    private final COSDictionary dictionary;

    public PDOutputIntent(PDDocument doc, InputStream colorProfile) throws IOException {
        dictionary = new COSDictionary();
        dictionary.setItem(COSName.TYPE, COSName.OUTPUT_INTENT);
        dictionary.setItem(COSName.S, COSName.GTS_PDFA1);
//        PDStream destOutputIntent = configureOutputProfile(doc, colorProfile);
//        dictionary.setItem(COSName.DEST_OUTPUT_PROFILE, destOutputIntent);
    }

    public PDOutputIntent(COSDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public COSDictionary getCOSObject() {
        return dictionary;
    }

    public COSStream getDestOutputIntent() {
        return dictionary.getCOSStream(COSName.DEST_OUTPUT_PROFILE);
    }

    public String getInfo() {
        return dictionary.getString(COSName.INFO);
    }

    public void setInfo(String value) {
        dictionary.setString(COSName.INFO, value);
    }

    public String getOutputCondition() {
        return dictionary.getString(COSName.OUTPUT_CONDITION);
    }

    public void setOutputCondition(String value) {
        dictionary.setString(COSName.OUTPUT_CONDITION, value);
    }

    public String getOutputConditionIdentifier() {
        return dictionary.getString(COSName.OUTPUT_CONDITION_IDENTIFIER);
    }

    public void setOutputConditionIdentifier(String value) {
        dictionary.setString(COSName.OUTPUT_CONDITION_IDENTIFIER, value);
    }

    public String getRegistryName() {
        return dictionary.getString(COSName.REGISTRY_NAME);
    }

    public void setRegistryName(String value) {
        dictionary.setString(COSName.REGISTRY_NAME, value);
    }

}
