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
package lpdf.pdfbox.cos;

import lpdf.pdfbox.Loader;
import lpdf.io.RandomAccessReadBuffer;
import lpdf.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ConcurrentModificationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

class TestCOSIncrement
{

    // TODO Very basic and primitive test - add in depth testing for all this.


    /**
     * PDFBOX-5263: There was a ConcurrentModificationException with
     * YTW2VWJQTDAE67PGJT6GS7QSKW3GNUQR.pdf - test that this issues has been resolved.
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    void testConcurrentModification() throws IOException, URISyntaxException
    {
        URL pdfLocation =
            new URI("https://issues.apache.org/jira/secure/attachment/12891316/YTW2VWJQTDAE67PGJT6GS7QSKW3GNUQR.pdf").toURL();

        try (PDDocument document = Loader
                .loadPDF(RandomAccessReadBuffer.createBufferFromStream(pdfLocation.openStream())))
        {
            document.setAllSecurityToBeRemoved(true);
            try
            {
                document.save(new ByteArrayOutputStream());
            }
            catch (ConcurrentModificationException e)
            {
                fail("There shouldn't be a ConcurrentModificationException", e.getCause());
            }
        }
    }

    private PDDocument loadDocument(byte[] documentData)
    {
        return assertDoesNotThrow(() -> Loader.loadPDF(documentData), "Loading the document failed.");
    }


}
