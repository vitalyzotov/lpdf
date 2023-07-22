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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import lpdf.fontbox.ttf.TrueTypeFont;
import lpdf.pdfbox.cos.COSArray;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSDocument;
import lpdf.pdfbox.cos.COSInteger;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSObjectKey;
import lpdf.io.IOUtils;
import lpdf.io.RandomAccessRead;
import lpdf.io.RandomAccessStreamCache.StreamCacheCreateFunction;
import lpdf.pdfbox.pdfwriter.COSWriter;
import lpdf.pdfbox.pdfwriter.compress.CompressParameters;
import lpdf.pdfbox.pdmodel.common.PDRectangle;
import lpdf.pdfbox.pdmodel.common.PDStream;
import lpdf.pdfbox.pdmodel.encryption.AccessPermission;
import lpdf.pdfbox.pdmodel.encryption.PDEncryption;
import lpdf.pdfbox.pdmodel.encryption.ProtectionPolicy;
import lpdf.pdfbox.pdmodel.encryption.SecurityHandler;
import lpdf.pdfbox.pdmodel.encryption.SecurityHandlerFactory;
import lpdf.pdfbox.pdmodel.font.PDFont;
import lpdf.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import lpdf.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import lpdf.pdfbox.pdmodel.interactive.digitalsignature.SigningSupport;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is the in-memory representation of the PDF document.
 * The #close() method must be called once the document is no longer needed.
 *
 * @author Ben Litchfield
 */
public class PDDocument implements Closeable
{
    /**
     * For signing: large reserve byte range used as placeholder in the saved PDF until the actual
     * length of the PDF is known. You'll need to fetch (with
     * {@link PDSignature#getByteRange()} ) and reassign this yourself (with
     * {@link PDSignature#setByteRange(int[])} ) only if you call
     * {@link #saveIncrementalForExternalSigning(java.io.OutputStream) saveIncrementalForExternalSigning()}
     * twice.
     */
    private static final int[] RESERVE_BYTE_RANGE = new int[] { 0, 1000000000, 1000000000, 1000000000 };

    private static final Log LOG = LogFactory.getLog(PDDocument.class);

    private final COSDocument document;

    // cached values
    private PDDocumentInformation documentInformation;
    private PDDocumentCatalog documentCatalog;

    // the encryption will be cached here. When the document is decrypted then
    // the COSDocument will not have an "Encrypt" dictionary anymore and this object must be used
    private PDEncryption encryption;

    // holds a flag which tells us if we should remove all security from this documents.
    private boolean allSecurityToBeRemoved;

    // keep tracking customized documentId for the trailer. If null, a new id will be generated
    // this ID doesn't represent the actual documentId from the trailer
    private Long documentId;

    // the pdf to be read
    private final RandomAccessRead pdfSource;

    // the access permissions of the document
    private AccessPermission accessPermission;

    // fonts to subset before saving
    private final Set<PDFont> fontsToSubset = new HashSet<>();

    // fonts to close when closing document
    private final Set<TrueTypeFont> fontsToClose = new HashSet<>();

    // Signature interface
    private SignatureInterface signInterface;

    // helper class used to create external signature
    private SigningSupport signingSupport;

    // document-wide cached resources
    private ResourceCache resourceCache = new DefaultResourceCache();

    // to make sure only one signature is added
    private boolean signatureAdded = false;

    /**
     * Creates an empty PDF document.
     * You need to add at least one page for the document to be valid.
     */
    public PDDocument()
    {
        this(IOUtils.createMemoryOnlyStreamCache());
    }

    /**
     * Creates an empty PDF document. You need to add at least one page for the document to be valid.
     *
     * @param streamCacheCreateFunction a function to create an instance of a stream cache for buffering PDF streams
     */
    public PDDocument(StreamCacheCreateFunction streamCacheCreateFunction)
    {
        document = new COSDocument(streamCacheCreateFunction);
        document.getDocumentState().setParsing(false);
        pdfSource = null;

        // First we need a trailer
        COSDictionary trailer = new COSDictionary();
        document.setTrailer(trailer);

        // Next we need the root dictionary.
        COSDictionary rootDictionary = new COSDictionary();
        trailer.setItem(COSName.ROOT, rootDictionary);
        rootDictionary.setItem(COSName.TYPE, COSName.CATALOG);
        rootDictionary.setItem(COSName.VERSION, COSName.getPDFName("1.4"));

        // next we need the pages tree structure
        COSDictionary pages = new COSDictionary();
        rootDictionary.setItem(COSName.PAGES, pages);
        pages.setItem(COSName.TYPE, COSName.PAGES);
        COSArray kidsArray = new COSArray();
        pages.setItem(COSName.KIDS, kidsArray);
        pages.setItem(COSName.COUNT, COSInteger.ZERO);
    }

    /**
     * Constructor that uses an existing document. The COSDocument that is passed in must be valid.
     *
     * @param doc The COSDocument that this document wraps.
     */
    public PDDocument(COSDocument doc)
    {
        this(doc, null);
    }

    /**
     * Constructor that uses an existing document. The COSDocument that is passed in must be valid.
     *
     * @param doc The COSDocument that this document wraps.
     * @param source input representing the pdf
     */
    public PDDocument(COSDocument doc, RandomAccessRead source)
    {
        this(doc, source, null);
    }

    /**
     * Constructor that uses an existing document. The COSDocument that is passed in must be valid.
     *
     * @param doc The COSDocument that this document wraps.
     * @param source input representing the pdf
     * @param permission he access permissions of the pdf
     *
     */
    public PDDocument(COSDocument doc, RandomAccessRead source, AccessPermission permission)
    {
        document = doc;
        document.getDocumentState().setParsing(false);
        pdfSource = source;
        accessPermission = permission;
    }

    /**
     * This will add a page to the document. This is a convenience method, that will add the page to the root of the
     * hierarchy and set the parent of the page to the root.
     *
     * @param page The page to add to the document.
     */
    public void addPage(PDPage page)
    {
        getPages().add(page);
    }


    /**
     * Remove the page from the document.
     *
     * @param page The page to remove from the document.
     */
    public void removePage(PDPage page)
    {
        getPages().remove(page);
    }

    /**
     * Remove the page from the document.
     *
     * @param pageNumber 0 based index to page number.
     */
    public void removePage(int pageNumber)
    {
        getPages().remove(pageNumber);
    }

    /**
     * This will import and copy the contents from another location. Currently the content stream is
     * stored in a scratch file. The scratch file is associated with the document. If you are adding
     * a page to this document from another document and want to copy the contents to this
     * document's scratch file then use this method otherwise just use the {@link #addPage addPage()}
     * method.
     * <p>
     * Unlike {@link #addPage addPage()}, this method creates a new PDPage object. If your page has
     * annotations, and if these link to pages not in the target document, then the target document
     * might become huge. What you need to do is to delete page references of such annotations. See
     * <a href="http://stackoverflow.com/a/35477351/535646">here</a> for how to do this.
     * <p>
     * Inherited (global) resources are ignored because these can contain resources not needed for
     * this page which could bloat your document, see
     * <a href="https://issues.apache.org/jira/browse/PDFBOX-28">PDFBOX-28</a> and related issues.
     * If you need them, call <code>importedPage.setResources(page.getResources());</code>
     * <p>
     * This method should only be used to import a page from a loaded document, not from a generated
     * document because these can contain unfinished parts, e.g. font subsetting information.
     *
     * @param page The page to import.
     * @return The page that was imported.
     *
     * @throws IOException If there is an error copying the page.
     */
    public PDPage importPage(PDPage page) throws IOException
    {
        PDPage importedPage = new PDPage(new COSDictionary(page.getCOSObject()), resourceCache);
        importedPage.getCOSObject().removeItem(COSName.PARENT);
        PDStream dest = new PDStream(this, page.getContents(), COSName.FLATE_DECODE);
        importedPage.setContents(dest);
        addPage(importedPage);
        setHighestImportedObjectNumber(importedPage);
        importedPage.setCropBox(new PDRectangle(page.getCropBox().getCOSArray()));
        importedPage.setMediaBox(new PDRectangle(page.getMediaBox().getCOSArray()));
        importedPage.setRotation(page.getRotation());
        if (page.getResources() != null && !page.getCOSObject().containsKey(COSName.RESOURCES))
        {
            LOG.warn("inherited resources of source document are not imported to destination page");
            LOG.warn("call importedPage.setResources(page.getResources()) to do this");
        }
        return importedPage;
    }

    /**
     * Determine the highest object number from the imported page to avoid mixed up numbers when saving the new pdf.
     *
     * @param importedPage the imported page.
     */
    private void setHighestImportedObjectNumber(PDPage importedPage)
    {
        List<COSObjectKey> indirectObjectKeys = new ArrayList<>();
        importedPage.getCOSObject().getIndirectObjectKeys(indirectObjectKeys);
        long highestImportedNumber = indirectObjectKeys.stream().map(COSObjectKey::getNumber)
                .max(Long::compare).orElse(0L);
        long highestXRefObjectNumber = getDocument().getHighestXRefObjectNumber();
        getDocument().setHighestXRefObjectNumber(
                Math.max(highestXRefObjectNumber, highestImportedNumber));
    }

    /**
     * This will get the low level document.
     *
     * @return The document that this layer sits on top of.
     */
    public COSDocument getDocument()
    {
        return document;
    }

    /**
     * This will get the document info dictionary. If it doesn't exist, an empty document info
     * dictionary is created in the document trailer.
     * <p>
     * In PDF 2.0 this is deprecated except for two entries, /CreationDate and /ModDate. For any other
     * document level metadata, a metadata stream should be used instead, see
     * {@link PDDocumentCatalog#getMetadata()}.
     *
     * @return The documents /Info dictionary, never null.
     */
    public PDDocumentInformation getDocumentInformation()
    {
        if (documentInformation == null)
        {
            COSDictionary trailer = document.getTrailer();
            COSDictionary infoDic = trailer.getCOSDictionary(COSName.INFO);
            if (infoDic == null)
            {
                infoDic = new COSDictionary();
                trailer.setItem(COSName.INFO, infoDic);
            }
            documentInformation = new PDDocumentInformation(infoDic);
        }
        return documentInformation;
    }

    /**
     * This will set the document information for this document.
     * <p>
     * In PDF 2.0 this is deprecated except for two entries, /CreationDate and /ModDate. For any other
     * document level metadata, a metadata stream should be used instead, see
     * {@link PDDocumentCatalog#setMetadata(lpdf.pdfbox.pdmodel.common.PDMetadata) PDDocumentCatalog#setMetadata(PDMetadata)}.
     *
     * @param info The updated document information.
     */
    public void setDocumentInformation(PDDocumentInformation info)
    {
        documentInformation = info;
        document.getTrailer().setItem(COSName.INFO, info.getCOSObject());
    }

    /**
     * This will get the document CATALOG. This is guaranteed to not return null.
     *
     * @return The documents /Root dictionary
     */
    public PDDocumentCatalog getDocumentCatalog()
    {
        if (documentCatalog == null)
        {
            COSDictionary trailer = document.getTrailer();
            COSDictionary dictionary = trailer.getCOSDictionary(COSName.ROOT);
            if (dictionary != null)
            {
                documentCatalog = new PDDocumentCatalog(this, dictionary);
            }
            else
            {
                documentCatalog = new PDDocumentCatalog(this);
            }
        }
        return documentCatalog;
    }

    /**
     * This will tell if this document is encrypted or not.
     *
     * @return true If this document is encrypted.
     */
    public boolean isEncrypted()
    {
        return document.isEncrypted();
    }

    /**
     * This will get the encryption dictionary for this document. This will still return the parameters if the document
     * was decrypted. As the encryption architecture in PDF documents is pluggable this returns an abstract class,
     * but the only supported subclass at this time is a
     * PDStandardEncryption object.
     *
     * @return The encryption dictionary(most likely a PDStandardEncryption object)
     */
    public PDEncryption getEncryption()
    {
        if (encryption == null && isEncrypted())
        {
            encryption = new PDEncryption(document.getEncryptionDictionary());
        }
        return encryption;
    }

    /**
     * This will set the encryption dictionary for this document.
     *
     * @param encryption The encryption dictionary(most likely a PDStandardEncryption object)
     */
    public void setEncryptionDictionary(PDEncryption encryption)
    {
        this.encryption = encryption;
    }

    /**
     * For internal PDFBox use when creating PDF documents: register a TrueTypeFont to make sure it is closed when the
     * PDDocument is closed to avoid memory leaks. Users don't have to call this method, it is done by the appropriate
     * PDFont classes.
     *
     * @param ttf the TrueTypeFont to be registered
     */
    public void registerTrueTypeFontForClosing(TrueTypeFont ttf)
    {
        fontsToClose.add(ttf);
    }

    /**
     * Returns the list of fonts which will be subset before the document is saved.
     */
    Set<PDFont> getFontsToSubset()
    {
        return fontsToSubset;
    }

    /**
     * Save the document to a file using default compression.
     * <p>
     * Don't use the input file as target as this will produce a corrupted file.
     * <p>
     * If encryption has been activated (with {@link #protect(lpdf.pdfbox.pdmodel.encryption.ProtectionPolicy)
     * protect(ProtectionPolicy)}), do not use the document after saving because the contents are now encrypted.
     *
     * @param fileName The file to save as.
     *
     * @throws IOException if the output could not be written
     */
    public void save(String fileName) throws IOException
    {
        save(new File(fileName));
    }

    /**
     * Save the document to a file using default compression.
     * <p>
     * Don't use the input file as target as this will produce a corrupted file.
     * <p>
     * If encryption has been activated (with {@link #protect(lpdf.pdfbox.pdmodel.encryption.ProtectionPolicy)
     * protect(ProtectionPolicy)}), do not use the document after saving because the contents are now encrypted.
     *
     * @param file The file to save as.
     *
     * @throws IOException if the output could not be written
     */
    public void save(File file) throws IOException
    {
        save(file, CompressParameters.DEFAULT_COMPRESSION);
    }

    /**
     * This will save the document to an output stream.
     * <p>
     * Don't use the input file as target as this will produce a corrupted file.
     * <p>
     * If encryption has been activated (with {@link #protect(lpdf.pdfbox.pdmodel.encryption.ProtectionPolicy)
     * protect(ProtectionPolicy)}), do not use the document after saving because the contents are now encrypted.
     *
     * @param output The stream to write to. It is recommended to wrap it in a {@link java.io.BufferedOutputStream},
     * unless it is already buffered.
     *
     * @throws IOException if the output could not be written
     */
    public void save(OutputStream output) throws IOException
    {
        save(output, CompressParameters.DEFAULT_COMPRESSION);
    }

    /**
     * Save the document using the given compression.
     * <p>
     * Don't use the input file as target as this will produce a corrupted file.
     * <p>
     * If encryption has been activated (with {@link #protect(lpdf.pdfbox.pdmodel.encryption.ProtectionPolicy)
     * protect(ProtectionPolicy)}), do not use the document after saving because the contents are now encrypted.
     *
     * @param file The file to save as.
     * @param compressParameters The parameters for the document's compression.
     * @throws IOException if the output could not be written
     */
    public void save(File file, CompressParameters compressParameters) throws IOException
    {
        if (file.exists())
        {
            LOG.warn(
                    "You are overwriting the existing file " + file.getName()
                            + ", this will produce a corrupted file if you're also reading from it");
        }
        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                new FileOutputStream(file)))
        {
            save(bufferedOutputStream, compressParameters);
        }
    }

    /**
     * Save the document to a file using the given compression.
     * <p>
     * Don't use the input file as target as this will produce a corrupted file.
     * <p>
     * If encryption has been activated (with {@link #protect(lpdf.pdfbox.pdmodel.encryption.ProtectionPolicy)
     * protect(ProtectionPolicy)}), do not use the document after saving because the contents are now encrypted.
     *
     * @param fileName The file to save as.
     * @param compressParameters The parameters for the document's compression.
     *
     * @throws IOException if the output could not be written
     */
    public void save(String fileName, CompressParameters compressParameters) throws IOException
    {
        save(new File(fileName), compressParameters);
    }

    /**
     * Save the document using the given compression.
     * <p>
     * Don't use the input file as target as this will produce a corrupted file.
     * <p>
     * If encryption has been activated (with {@link #protect(lpdf.pdfbox.pdmodel.encryption.ProtectionPolicy)
     * protect(ProtectionPolicy)}), do not use the document after saving because the contents are now encrypted.
     *
     * @param output The stream to write to. It is recommended to wrap it in a {@link java.io.BufferedOutputStream},
     * unless it is already buffered.
     * @param compressParameters The parameters for the document's compression.
     * @throws IOException if the output could not be written
     */
    public void save(OutputStream output, CompressParameters compressParameters)
            throws IOException
    {
        if (document.isClosed())
        {
            throw new IOException("Cannot save a document which has been closed");
        }

        // object stream compression requires a cross reference stream.
        document.setIsXRefStream(compressParameters != null //
                && CompressParameters.NO_COMPRESSION != compressParameters);
        subsetDesignatedFonts();

        // save PDF
        COSWriter writer = new COSWriter(output, compressParameters);
        writer.write(this);
    }

    private void subsetDesignatedFonts() throws IOException
    {
        // subset designated fonts
        for (PDFont font : fontsToSubset)
        {
            font.subset();
        }
        fontsToSubset.clear();
    }



    /**
     * Returns the page at the given 0-based index.
     * <p>
     * This method is too slow to get all the pages from a large PDF document
     * (1000 pages or more). For such documents, use the iterator of
     * {@link PDDocument#getPages()} instead.
     *
     * @param pageIndex the 0-based page index
     * @return the page at the given index.
     */
    public PDPage getPage(int pageIndex) // todo: REPLACE most calls to this method with BELOW method
    {
        return getDocumentCatalog().getPages().get(pageIndex);
    }

    /**
     * Returns the page tree.
     *
     * @return the page tree
     */
    public PDPageTree getPages()
    {
        return getDocumentCatalog().getPages();
    }

    /**
     * This will return the total page count of the PDF document.
     *
     * @return The total number of pages in the PDF document.
     */
    public int getNumberOfPages()
    {
        return getDocumentCatalog().getPages().getCount();
    }

    /**
     * This will close the underlying COSDocument object.
     *
     * @throws IOException If there is an error releasing resources.
     */
    @Override
    public void close() throws IOException
    {
        if (!document.isClosed())
        {
             // Make sure that:
            // - first Exception is kept
            // - all IO resources are closed
            // - there's a way to see which errors occurred

            IOException firstException = null;

            // close resources and COSWriter
            if (signingSupport != null)
            {
                firstException = IOUtils.closeAndLogException(signingSupport, LOG, "SigningSupport", firstException);
            }

            // close all intermediate I/O streams
            firstException = IOUtils.closeAndLogException(document, LOG, "COSDocument", firstException);

            // close the source PDF stream, if we read from one
            if (pdfSource != null)
            {
                firstException = IOUtils.closeAndLogException(pdfSource, LOG, "RandomAccessRead pdfSource", firstException);
            }

            // close fonts
            for (TrueTypeFont ttf : fontsToClose)
            {
                firstException = IOUtils.closeAndLogException(ttf, LOG, "TrueTypeFont", firstException);
            }

            // rethrow first exception to keep method contract
            if (firstException != null)
            {
                throw firstException;
            }
        }
    }

    /**
     * Protects the document with a protection policy. The document content will be really
     * encrypted when it will be saved. This method only marks the document for encryption. It also
     * calls {@link #setAllSecurityToBeRemoved(boolean)} with a false argument if it was set to true
     * previously and logs a warning.
     * <p>
     * Do not use the document after saving, because the structures are encrypted.
     *
     * @see lpdf.pdfbox.pdmodel.encryption.StandardProtectionPolicy
     * @see lpdf.pdfbox.pdmodel.encryption.PublicKeyProtectionPolicy
     *
     * @param policy The protection policy.
     * @throws IOException if there isn't any suitable security handler.
     */
    public void protect(ProtectionPolicy policy) throws IOException
    {
        if (isAllSecurityToBeRemoved())
        {
            LOG.warn("do not call setAllSecurityToBeRemoved(true) before calling protect(), "
                    + "as protect() implies setAllSecurityToBeRemoved(false)");
            setAllSecurityToBeRemoved(false);
        }

        if (!isEncrypted())
        {
            encryption = new PDEncryption();
        }

        SecurityHandler<ProtectionPolicy> securityHandler =
                SecurityHandlerFactory.INSTANCE.newSecurityHandlerForPolicy(policy);
        if (securityHandler == null)
        {
            throw new IOException("No security handler for policy " + policy);
        }

        getEncryption().setSecurityHandler(securityHandler);
    }

    /**
     * Returns the access permissions granted when the document was decrypted. If the document was not decrypted this
     * method returns the access permission for a document owner (ie can do everything). The returned object is in read
     * only mode so that permissions cannot be changed. Methods providing access to content should rely on this object
     * to verify if the current user is allowed to proceed.
     *
     * @return the access permissions for the current user on the document.
     */
    public AccessPermission getCurrentAccessPermission()
    {
        if (accessPermission == null)
        {
            accessPermission = AccessPermission.getOwnerAccessPermission();
        }
        return accessPermission;
    }

    /**
     * Indicates if all security is removed or not when writing the pdf.
     *
     * @return returns true if all security shall be removed otherwise false
     */
    public boolean isAllSecurityToBeRemoved()
    {
        return allSecurityToBeRemoved;
    }

    /**
     * Activates/Deactivates the removal of all security when writing the pdf.
     *
     * @param removeAllSecurity remove all security if set to true
     */
    public void setAllSecurityToBeRemoved(boolean removeAllSecurity)
    {
        allSecurityToBeRemoved = removeAllSecurity;
    }

    /**
     * Provides the document ID.
     *
     * @return the document ID
     */
    public Long getDocumentId()
    {
        return documentId;
    }

    /**
     * Sets the document ID to the given value.
     *
     * @param docId the new document ID
     */
    public void setDocumentId(Long docId)
    {
        documentId = docId;
    }

    /**
     * Returns the PDF specification version this document conforms to.
     *
     * @return the PDF version (e.g. 1.4f)
     */
    public float getVersion()
    {
        float headerVersionFloat = getDocument().getVersion();
        // there may be a second version information in the document catalog starting with 1.4
        if (headerVersionFloat >= 1.4f)
        {
            String catalogVersion = getDocumentCatalog().getVersion();
            float catalogVersionFloat = -1;
            if (catalogVersion != null)
            {
                try
                {
                    catalogVersionFloat = Float.parseFloat(catalogVersion);
                }
                catch(NumberFormatException exception)
                {
                    LOG.error("Can't extract the version number of the document catalog.", exception);
                }
            }
            // the most recent version is the correct one
            return Math.max(catalogVersionFloat, headerVersionFloat);
        }
        else
        {
            return headerVersionFloat;
        }
    }

    /**
     * Sets the PDF specification version for this document.
     *
     * @param newVersion the new PDF version (e.g. 1.4f)
     *
     */
    public void setVersion(float newVersion)
    {
        float currentVersion = getVersion();
        // nothing to do?
        if (Float.compare(newVersion,currentVersion) == 0)
        {
            return;
        }
        // the version can't be downgraded
        if (newVersion < currentVersion)
        {
            LOG.error("It's not allowed to downgrade the version of a pdf.");
            return;
        }
        // update the catalog version if the document version is >= 1.4
        if (getDocument().getVersion() >= 1.4f)
        {
            getDocumentCatalog().setVersion(Float.toString(newVersion));
        }
        else
        {
            // versions < 1.4f have a version header only
            getDocument().setVersion(newVersion);
        }
    }

    /**
     * Returns the resource cache associated with this document, or null if there is none.
     *
     * @return the resource cache of the document
     */
    public ResourceCache getResourceCache()
    {
        return resourceCache;
    }

    /**
     * Sets the resource cache associated with this document.
     *
     * @param resourceCache A resource cache, or null.
     */
    public void setResourceCache(ResourceCache resourceCache)
    {
        this.resourceCache = resourceCache;
    }
}
