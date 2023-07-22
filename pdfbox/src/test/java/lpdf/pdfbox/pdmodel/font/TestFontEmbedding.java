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

package lpdf.pdfbox.pdmodel.font;

import lpdf.fontbox.ttf.OS2WindowsMetricsTable;
import lpdf.fontbox.ttf.TTFParser;
import lpdf.fontbox.ttf.TrueTypeFont;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

/**
 * Tests font embedding.
 *
 * @author John Hewson
 * @author Tilman Hausherr
 */
@Execution(ExecutionMode.CONCURRENT)
class TestFontEmbedding
{
    private static final File OUT_DIR = new File("target/test-output");

    @BeforeAll
    static void setUp()
    {
        OUT_DIR.mkdirs();
    }

    private class TrueTypeEmbedderTester extends TrueTypeEmbedder
    {

        /**
         * Common functionality for testing the TrueTypeFontEmbedder
         *
         */
        TrueTypeEmbedderTester(PDDocument document, COSDictionary dict, TrueTypeFont ttf, boolean embedSubset)
                throws IOException
        {
            super(document, dict, ttf, embedSubset);
        }

        @Override
        protected void buildSubset(InputStream ttfSubset, String tag, Map<Integer, Integer> gidToCid)
                throws IOException
        {
            // no-op.  Need to define method to extend abstract class, but
            // this method is not currently needed for testing
        }
    }

    /**
     * Test that we validate embedding permissions properly for all legal permissions combinations
     *
     * @throws IOException
     */
    @Test
    void testIsEmbeddingPermittedMultipleVersions() throws IOException
    {
        // SETUP
        PDDocument doc = new PDDocument();
        COSDictionary cosDictionary = new COSDictionary();
        InputStream input = PDFont.class.getResourceAsStream("/lpdf/pdfbox/resources/ttf/LiberationSans-Regular.ttf");
        TrueTypeFont ttf = new TTFParser().parseEmbedded(input);
        TrueTypeEmbedderTester tester = new TrueTypeEmbedderTester(doc, cosDictionary, ttf, true);
        TrueTypeFont mockTtf = Mockito.mock(TrueTypeFont.class);
        OS2WindowsMetricsTable mockOS2 = Mockito.mock(OS2WindowsMetricsTable.class);
        given(mockTtf.getOS2Windows()).willReturn(mockOS2);
        Boolean embeddingIsPermitted;

        // TEST 1: 0000 -- Installable embedding versions 0-3+
        given(mockTtf.getOS2Windows().getFsType()).willReturn((short) 0x0000);
        embeddingIsPermitted = tester.isEmbeddingPermitted(mockTtf);

        // VERIFY
        assertTrue(embeddingIsPermitted);

        // no test for 0001, since bit 0 is permanently reserved, and its use is deprecated
        // TEST 2: 0010 -- Restricted License embedding versions 0-3+
        given(mockTtf.getOS2Windows().getFsType()).willReturn((short) 0x0002);
        embeddingIsPermitted = tester.isEmbeddingPermitted(mockTtf);

        // VERIFY
        assertFalse(embeddingIsPermitted);

        // no test for 0011
        // TEST 3: 0100 -- Preview & Print embedding versions 0-3+
        given(mockTtf.getOS2Windows().getFsType()).willReturn((short) 0x0004);
        embeddingIsPermitted = tester.isEmbeddingPermitted(mockTtf);

        // VERIFY
        assertTrue(embeddingIsPermitted);

        // no test for 0101
        // TEST 4: 0110 -- Restricted License embedding AND Preview & Print embedding versions 0-2
        //              -- illegal permissions combination for versions 3+
        given(mockTtf.getOS2Windows().getFsType()).willReturn((short) 0x0006);
        embeddingIsPermitted = tester.isEmbeddingPermitted(mockTtf);

        // VERIFY
        assertTrue(embeddingIsPermitted);

        // no test for 0111
        // TEST 5: 1000 -- Editable embedding versions 0-3+
        given(mockTtf.getOS2Windows().getFsType()).willReturn((short) 0x0008);
        embeddingIsPermitted = tester.isEmbeddingPermitted(mockTtf);

        // VERIFY
        assertTrue(embeddingIsPermitted);

        // no test for 1001
        // TEST 6: 1010 -- Restricted License embedding AND Editable embedding versions 0-2
        //              -- illegal permissions combination for versions 3+
        given(mockTtf.getOS2Windows().getFsType()).willReturn((short) 0x000A);
        embeddingIsPermitted = tester.isEmbeddingPermitted(mockTtf);

        // VERIFY
        assertTrue(embeddingIsPermitted);

        // no test for 1011
        // TEST 7: 1100 -- Editable embedding AND Preview & Print embedding versions 0-2
        //              -- illegal permissions combination for versions 3+
        given(mockTtf.getOS2Windows().getFsType()).willReturn((short) 0x000C);
        embeddingIsPermitted = tester.isEmbeddingPermitted(mockTtf);

        // VERIFY
        assertTrue(embeddingIsPermitted);

        // no test for 1101
        // TEST 8: 1110 Editable embedding AND Preview & Print embedding AND Restricted License embedding versions 0-2
        //              -- illegal permissions combination for versions 3+
        given(mockTtf.getOS2Windows().getFsType()).willReturn((short) 0x000E);
        embeddingIsPermitted = tester.isEmbeddingPermitted(mockTtf);

        // VERIFY
        assertTrue(embeddingIsPermitted);

        // no test for 1111
    }
}
