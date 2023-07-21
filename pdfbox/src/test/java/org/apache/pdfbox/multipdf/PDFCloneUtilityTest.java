/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.multipdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Test suite for PDFCloneUtility, see PDFBOX-2052.
 *
 * @author Cornelis Hoeflake
 * @author Tilman Hausherr
 */
class PDFCloneUtilityTest {
    /**
     * original (minimal) test from PDFBOX-2052.
     *
     * @throws IOException
     */
    @Test
    void testClonePDFWithCosArrayStream() throws IOException {
        try (PDDocument srcDoc = new PDDocument();
             PDDocument dstDoc = new PDDocument()) {

            PDPage pdPage = new PDPage();
            srcDoc.addPage(pdPage);
            new PDPageContentStream(srcDoc, pdPage, AppendMode.APPEND, true).close();
            new PDPageContentStream(srcDoc, pdPage, AppendMode.APPEND, true).close();
            new PDFCloneUtility(dstDoc).cloneForNewDocument(pdPage.getCOSObject());
        }
    }

}
