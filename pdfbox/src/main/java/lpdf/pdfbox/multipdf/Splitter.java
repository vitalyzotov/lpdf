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
package lpdf.pdfbox.multipdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.io.RandomAccessStreamCache.StreamCacheCreateFunction;
import lpdf.pdfbox.pdmodel.PDDocument;
import lpdf.pdfbox.pdmodel.PDDocumentInformation;
import lpdf.pdfbox.pdmodel.PDPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Split a document into several other documents.
 *
 * @author Mario Ivankovits
 * @author Ben Litchfield
 */
public class Splitter {
    private static final Logger LOG = LoggerFactory.getLogger(Splitter.class);

    private PDDocument sourceDocument;
    private PDDocument currentDestinationDocument;

    private int splitLength = 1;
    private int startPage = Integer.MIN_VALUE;
    private int endPage = Integer.MAX_VALUE;
    private List<PDDocument> destinationDocuments;

    private int currentPageNumber;

    private StreamCacheCreateFunction streamCacheCreateFunction = null;

    /**
     * @return the current function to be used to create an instance of stream cache.
     */
    public StreamCacheCreateFunction getStreamCacheCreateFunction() {
        return streamCacheCreateFunction;
    }

    /**
     * Set the current function to be used to create an instance of stream cache.
     *
     * @param streamCacheCreateFunction the current function to be used to create an instance of stream cache.
     */
    public void setStreamCacheCreateFunction(StreamCacheCreateFunction streamCacheCreateFunction) {
        this.streamCacheCreateFunction = streamCacheCreateFunction;
    }

    /**
     * This will take a document and split into several other documents.
     *
     * @param document The document to split.
     * @return A list of all the split documents. These should all be saved before closing any
     * documents, including the source document. Any further operations should be made after
     * reloading them, to avoid problems due to resource sharing. For the same reason, they should
     * not be saved with encryption.
     * @throws IOException If there is an IOError
     */
    public List<PDDocument> split(PDDocument document) throws IOException {
        // reset the currentPageNumber for a case if the split method will be used several times
        currentPageNumber = 0;
        destinationDocuments = new ArrayList<>();
        sourceDocument = document;
        processPages();
        return destinationDocuments;
    }

    /**
     * This will tell the splitting algorithm where to split the pages.  The default
     * is 1, so every page will become a new document.  If it was two then each document would
     * contain 2 pages.  If the source document had 5 pages it would split into
     * 3 new documents, 2 documents containing 2 pages and 1 document containing one
     * page.
     *
     * @param split The number of pages each split document should contain.
     * @throws IllegalArgumentException if the page is smaller than one.
     */
    public void setSplitAtPage(int split) {
        if (split <= 0) {
            throw new IllegalArgumentException("Number of pages is smaller than one");
        }
        splitLength = split;
    }

    /**
     * This will set the start page.
     *
     * @param start the 1-based start page
     * @throws IllegalArgumentException if the start page is smaller than one.
     */
    public void setStartPage(int start) {
        if (start <= 0) {
            throw new IllegalArgumentException("Start page is smaller than one");
        }
        startPage = start;
    }

    /**
     * This will set the end page.
     *
     * @param end the 1-based end page
     * @throws IllegalArgumentException if the end page is smaller than one.
     */
    public void setEndPage(int end) {
        if (end <= 0) {
            throw new IllegalArgumentException("End page is smaller than one");
        }
        endPage = end;
    }

    /**
     * Interface method to handle the start of the page processing.
     *
     * @throws IOException If an IO error occurs.
     */
    private void processPages() throws IOException {
        for (PDPage page : sourceDocument.getPages()) {
            if (currentPageNumber + 1 >= startPage && currentPageNumber + 1 <= endPage) {
                processPage(page);
                currentPageNumber++;
            } else {
                if (currentPageNumber > endPage) {
                    break;
                } else {
                    currentPageNumber++;
                }
            }
        }
    }

    /**
     * Helper method for creating new documents at the appropriate pages.
     *
     * @throws IOException If there is an error creating the new document.
     */
    private void createNewDocumentIfNecessary() throws IOException {
        if (splitAtPage(currentPageNumber) || currentDestinationDocument == null) {
            currentDestinationDocument = createNewDocument();
            destinationDocuments.add(currentDestinationDocument);
        }
    }

    /**
     * Check if it is necessary to create a new document.
     * By default a split occurs at every page.  If you wanted to split
     * based on some complex logic then you could override this method.  For example.
     * <code>
     * protected void splitAtPage()
     * {
     * // will split at pages with prime numbers only
     * return isPrime(pageNumber);
     * }
     * </code>
     *
     * @param pageNumber the 0-based page number to be checked as splitting page
     * @return true If a new document should be created.
     */
    protected boolean splitAtPage(int pageNumber) {
        return (pageNumber + 1 - Math.max(1, startPage)) % splitLength == 0;
    }

    /**
     * Create a new document to write the split contents to.
     *
     * @return the newly created PDDocument.
     * @throws IOException If there is an problem creating the new document.
     */
    protected PDDocument createNewDocument() throws IOException {
        PDDocument document = streamCacheCreateFunction != null ? new PDDocument(streamCacheCreateFunction) : new PDDocument();
        document.getDocument().setVersion(getSourceDocument().getVersion());
        PDDocumentInformation sourceDocumentInformation = getSourceDocument().getDocumentInformation();
        if (sourceDocumentInformation != null) {
            // PDFBOX-5317: Image Capture Plus files where /Root and /Info share the same dictionary
            // Only copy simple elements to avoid huge files
            COSDictionary sourceDocumentInformationDictionary = sourceDocumentInformation.getCOSObject();
            COSDictionary destDocumentInformationDictionary = new COSDictionary();
            for (COSName key : sourceDocumentInformationDictionary.keySet()) {
                COSBase value = sourceDocumentInformationDictionary.getDictionaryObject(key);
                if (value instanceof COSDictionary) {
                    LOG.warn("Nested entry for key '" + key.getName()
                            + "' skipped in document information dictionary");
                    if (sourceDocument.getDocumentCatalog().getCOSObject() ==
                            sourceDocument.getDocumentInformation().getCOSObject()) {
                        LOG.warn("/Root and /Info share the same dictionary");
                    }
                    continue;
                }
                if (COSName.TYPE.equals(key)) {
                    continue; // there is no /Type in the document information dictionary
                }
                destDocumentInformationDictionary.setItem(key, value);
            }
            document.setDocumentInformation(new PDDocumentInformation(destDocumentInformationDictionary));
        }
        document.getDocumentCatalog().setViewerPreferences(
                getSourceDocument().getDocumentCatalog().getViewerPreferences());
        return document;
    }

    /**
     * Interface to start processing a new page.
     *
     * @param page The page that is about to get processed.
     * @throws IOException If there is an error creating the new document.
     */
    protected void processPage(PDPage page) throws IOException {
        createNewDocumentIfNecessary();

        PDPage imported = getDestinationDocument().importPage(page);
        if (page.getResources() != null && !page.getCOSObject().containsKey(COSName.RESOURCES)) {
            imported.setResources(page.getResources());
            LOG.info("Resources imported in Splitter"); // follow-up to warning in importPage
        }
    }


    /**
     * The source PDF document.
     *
     * @return the pdf to be split
     */
    protected final PDDocument getSourceDocument() {
        return sourceDocument;
    }

    /**
     * The source PDF document.
     *
     * @return current destination pdf
     */
    protected final PDDocument getDestinationDocument() {
        return currentDestinationDocument;
    }
}
