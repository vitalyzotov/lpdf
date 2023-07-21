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

package org.apache.pdfbox.pdmodel.font;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.autodetect.FontFileFinder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author adam
 * @author Tilman Hausherr
 */
@Execution(ExecutionMode.CONCURRENT)
class PDFontTest {
    private static final File OUT_DIR = new File("target/test-output");

    @BeforeAll
    static void setUp() throws Exception {
        OUT_DIR.mkdirs();
    }


    @Test
    void testPDFBOX5486() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDTrueTypeFont ttf = PDTrueTypeFont.load(doc,
                    PDFontTest.class.getResourceAsStream(
                            "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf"),
                    WinAnsiEncoding.INSTANCE);
            assertTrue(ttf.hasGlyph("A"));
            ttf.getPath("A");
        }
    }


    /**
     * Test whether bug from PDFBOX-4318 is fixed, which had the wrong cache key.
     *
     * @throws java.io.IOException
     */
    @Test
    void testPDFox4318() throws IOException {
        PDType1Font helveticaBold = new PDType1Font(FontName.HELVETICA_BOLD);
        assertThrows(IllegalArgumentException.class,
                () -> helveticaBold.encode("\u0080"),
                "should have thrown IllegalArgumentException");
        helveticaBold.encode("€");
        assertThrows(IllegalArgumentException.class,
                () -> helveticaBold.encode("\u0080"),
                "should have thrown IllegalArgumentException");
    }

    @Test
    void testFullEmbeddingTTC() throws IOException {
        FontFileFinder fff = new FontFileFinder();
        TrueTypeCollection ttc = null;
        for (URI uri : fff.find()) {
            if (uri.getPath().endsWith(".ttc")) {
                File file = new File(uri);
                System.out.println("TrueType collection file: " + file);
                ttc = new TrueTypeCollection(file);
                break;
            }
        }
        Assumptions.assumeTrue(ttc != null, "testFullEmbeddingTTC skipped, no .ttc files available");

        final List<String> names = new ArrayList<>();
        ttc.processAllFonts((TrueTypeFont ttf) ->
        {
            System.out.println("TrueType font in collection: " + ttf.getName());
            names.add(ttf.getName());
        });

        TrueTypeFont ttf = ttc.getFontByName(names.get(0)); // take the first one
        System.out.println("TrueType font used for test: " + ttf.getName());

        IOException ex = assertThrows(IOException.class,
                () -> PDType0Font.load(new PDDocument(), ttf, false),
                "should have thrown IOException");
        assertEquals("Full embedding of TrueType font collections not supported", ex.getMessage());
    }

    /**
     * Test using broken Type1C font.
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    void testPDFox5048() throws IOException, URISyntaxException {
        try (PDDocument doc = Loader.loadPDF(RandomAccessReadBuffer.createBufferFromStream(
                new URI("https://issues.apache.org/jira/secure/attachment/13017227/stringwidth.pdf")
                        .toURL().openStream()))) {
            PDPage page = doc.getPage(0);
            PDFont font = page.getResources().getFont(COSName.getPDFName("F70"));
            assertTrue(font.isDamaged());
            assertEquals(0, font.getHeight(0));
            assertEquals(0, font.getStringWidth("Pa"));
        }
    }


    private byte[] testPDFBox3826createDoc(TrueTypeFont ttf) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            // type 0 subset embedding
            PDFont font = PDType0Font.load(doc, ttf, true);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.newLineAtOffset(10, 700);
                cs.setFont(font, 10);
                cs.showText("testMultipleFontFileReuse1");
                cs.endText();
                // type 0 full embedding
                font = PDType0Font.load(doc, ttf, false);
                cs.beginText();
                cs.newLineAtOffset(10, 650);
                cs.setFont(font, 10);
                cs.showText("testMultipleFontFileReuse2");
                cs.endText();
                // tt full embedding but only WinAnsiEncoding
                font = PDTrueTypeFont.load(doc, ttf, WinAnsiEncoding.INSTANCE);
                cs.beginText();
                cs.newLineAtOffset(10, 600);
                cs.setFont(font, 10);
                cs.showText("testMultipleFontFileReuse3");
                cs.endText();
            }

            doc.save(baos);
        }
        return baos.toByteArray();
    }


    /**
     * Test font with an unusual cmap table combination (0, 3).
     *
     * @throws IOException
     */
    @Test
    void testPDFBox5484() throws IOException {
        File fontFile = new File("target/fonts", "PDFBOX-5484.ttf");
        TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBufferedFile(fontFile));
        try (PDDocument doc = new PDDocument()) {
            PDTrueTypeFont tr = PDTrueTypeFont.load(doc, ttf, WinAnsiEncoding.INSTANCE);
            GeneralPath path1 = tr.getPath("oslash");
            GeneralPath path2 = tr.getPath(248);
            assertFalse(path2.getPathIterator(null).isDone()); // not empty
            assertTrue(new Area(path1).equals(new Area(path2))); // assertEquals does not test equals()
        }
    }
}
