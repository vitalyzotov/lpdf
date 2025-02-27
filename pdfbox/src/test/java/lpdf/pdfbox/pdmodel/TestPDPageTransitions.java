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
package lpdf.pdfbox.pdmodel;

import lpdf.pdfbox.Loader;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.pdmodel.interactive.pagenavigation.PDTransition;
import lpdf.pdfbox.pdmodel.interactive.pagenavigation.PDTransitionDirection;
import lpdf.pdfbox.pdmodel.interactive.pagenavigation.PDTransitionStyle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andrea Vacondio
 */
class TestPDPageTransitions {

    @Test
    void readTransitions() throws IOException, URISyntaxException {
        try (PDDocument doc = Loader.loadPDF(new File(this.getClass().getResource(
                "/lpdf/pdfbox/pdmodel/interactive/pagenavigation/transitions_test.pdf").toURI()))) {
            PDTransition firstTransition = doc.getPages().get(0).getTransition();
            assertEquals(PDTransitionStyle.Glitter.name(), firstTransition.getStyle());
            assertEquals(2, firstTransition.getDuration(), 0);
            assertEquals(PDTransitionDirection.TOP_LEFT_TO_BOTTOM_RIGHT.getCOSBase(),
                    firstTransition.getDirection());
        }
    }

    @Test
    void saveAndReadTransitions() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // save
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDTransition transition = new PDTransition(PDTransitionStyle.Fly);
            transition.setDirection(PDTransitionDirection.NONE);
            transition.setFlyScale(0.5f);
            page.setTransition(transition, 2);
            document.save(baos);
        }

        // read
        try (PDDocument doc = Loader.loadPDF(baos.toByteArray())) {
            PDPage page = doc.getPages().get(0);
            PDTransition loadedTransition = page.getTransition();
            assertEquals(PDTransitionStyle.Fly.name(), loadedTransition.getStyle());
            assertEquals(2, page.getCOSObject().getFloat(COSName.DUR), 0);
            assertEquals(PDTransitionDirection.NONE.getCOSBase(), loadedTransition.getDirection());
        }
    }
}
