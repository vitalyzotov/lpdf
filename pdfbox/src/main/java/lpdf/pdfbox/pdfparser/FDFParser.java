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
package lpdf.pdfbox.pdfparser;

import lpdf.io.RandomAccessRead;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSDocument;
import lpdf.pdfbox.cos.COSName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FDFParser extends COSParser {
    private static final Logger LOG = LoggerFactory.getLogger(FDFParser.class);

    /**
     * Constructs parser for given file using memory buffer.
     *
     * @param source the source of the pdf to be parsed
     * @throws IOException If something went wrong.
     */
    public FDFParser(RandomAccessRead source) throws IOException {
        super(source);
        init();
    }

    private void init() {
        String eofLookupRangeStr = System.getProperty(SYSPROP_EOFLOOKUPRANGE);
        if (eofLookupRangeStr != null) {
            try {
                setEOFLookupRange(Integer.parseInt(eofLookupRangeStr));
            } catch (NumberFormatException nfe) {
                LOG.warn("System property " + SYSPROP_EOFLOOKUPRANGE
                        + " does not contain an integer value, but: '" + eofLookupRangeStr + "'");
            }
        }
        document = new COSDocument(this);
    }

    /**
     * The initial parse will first parse only the trailer, the xrefstart and all xref tables to have a pointer (offset)
     * to all the pdf's objects. It can handle linearized pdfs, which will have an xref at the end pointing to an xref
     * at the beginning of the file. Last the root object is parsed.
     *
     * @throws IOException If something went wrong.
     */
    private void initialParse() throws IOException {
        COSDictionary trailer = retrieveTrailer();

        COSDictionary root = trailer.getCOSDictionary(COSName.ROOT);
        if (root == null) {
            throw new IOException("Missing root object specification in trailer.");
        }
        initialParseDone = true;
    }

}
