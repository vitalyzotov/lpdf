/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import lpdf.pdfbox.cos.COSArray;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSObject;
import lpdf.pdfbox.cos.COSStream;
import lpdf.pdfbox.pdmodel.common.COSArrayList;
import lpdf.pdfbox.pdmodel.common.COSObjectable;
import lpdf.pdfbox.pdmodel.common.PDDestinationOrAction;
import lpdf.pdfbox.pdmodel.common.PDMetadata;
import lpdf.pdfbox.pdmodel.common.PDPageLabels;
import lpdf.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import lpdf.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import lpdf.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import lpdf.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import lpdf.pdfbox.pdmodel.interactive.action.PDDocumentCatalogAdditionalActions;
import lpdf.pdfbox.pdmodel.interactive.action.PDURIDictionary;
import lpdf.pdfbox.pdmodel.interactive.pagenavigation.PDThread;
import lpdf.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Document Catalog of a PDF.
 *
 * @author Ben Litchfield
 */
public class PDDocumentCatalog implements COSObjectable {
    private static final Logger LOG = LoggerFactory.getLogger(PDDocumentCatalog.class);

    private final COSDictionary root;
    private final PDDocument document;

    /**
     * Constructor. Internal PDFBox use only! If you need to get the document catalog, call
     * {@link PDDocument#getDocumentCatalog()}.
     *
     * @param doc The document that this catalog is part of.
     */
    protected PDDocumentCatalog(PDDocument doc) {
        document = doc;
        root = new COSDictionary();
        root.setItem(COSName.TYPE, COSName.CATALOG);
        document.getDocument().getTrailer().setItem(COSName.ROOT, root);
    }

    /**
     * Constructor. Internal PDFBox use only! If you need to get the document catalog, call
     * {@link PDDocument#getDocumentCatalog()}.
     *
     * @param doc            The document that this catalog is part of.
     * @param rootDictionary The root dictionary that this object wraps.
     */
    protected PDDocumentCatalog(PDDocument doc, COSDictionary rootDictionary) {
        document = doc;
        root = rootDictionary;
    }

    /**
     * Convert this standard java object to a COS object.
     *
     * @return The cos object that matches this Java object.
     */
    @Override
    public COSDictionary getCOSObject() {
        return root;
    }

    /**
     * Returns all pages in the document, as a page tree.
     *
     * @return PDPageTree providing all pages of the document
     */
    public PDPageTree getPages() {
        // todo: cache me?
        return new PDPageTree(root.getCOSDictionary(COSName.PAGES), document);
    }

    /**
     * Get the viewer preferences associated with this document or null if they do not exist.
     *
     * @return The document's viewer preferences.
     */
    public PDViewerPreferences getViewerPreferences() {
        COSDictionary viewerPref = root.getCOSDictionary(COSName.VIEWER_PREFERENCES);
        return viewerPref != null ? new PDViewerPreferences(viewerPref) : null;
    }

    /**
     * Sets the viewer preferences.
     *
     * @param prefs The new viewer preferences.
     */
    public void setViewerPreferences(PDViewerPreferences prefs) {
        root.setItem(COSName.VIEWER_PREFERENCES, prefs);
    }

    /**
     * Returns the document's article threads.
     *
     * @return a list of all threads of the document
     */
    public List<PDThread> getThreads() {
        COSArray array = root.getCOSArray(COSName.THREADS);
        if (array == null) {
            array = new COSArray();
            root.setItem(COSName.THREADS, array);
        }
        List<PDThread> pdObjects = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            pdObjects.add(new PDThread((COSDictionary) array.getObject(i)));
        }
        return new COSArrayList<>(pdObjects, array);
    }

    /**
     * Sets the list of threads for this pdf document.
     *
     * @param threads The list of threads, or null to clear it.
     */
    public void setThreads(List<PDThread> threads) {
        root.setItem(COSName.THREADS, new COSArray(threads));
    }

    /**
     * Get the metadata that is part of the document catalog. This will return null if there is no
     * meta data for this object.
     *
     * @return The metadata for this object.
     */
    public PDMetadata getMetadata() {
        COSStream metaObj = root.getCOSStream(COSName.METADATA);
        return metaObj != null ? new PDMetadata(metaObj) : null;
    }

    /**
     * Sets the metadata for this object. This can be null.
     *
     * @param meta The meta data for this object.
     */
    public void setMetadata(PDMetadata meta) {
        root.setItem(COSName.METADATA, meta);
    }

    /**
     * Sets the Document Open Action for this object.
     *
     * @param action The action you want to perform.
     */
    public void setOpenAction(PDDestinationOrAction action) {
        root.setItem(COSName.OPEN_ACTION, action);
    }

    /**
     * @return The Additional Actions for this Document
     */
    public PDDocumentCatalogAdditionalActions getActions() {
        COSDictionary addAction = root.getCOSDictionary(COSName.AA);
        if (addAction == null) {
            addAction = new COSDictionary();
            root.setItem(COSName.AA, addAction);
        }
        return new PDDocumentCatalogAdditionalActions(addAction);
    }

    /**
     * Sets the additional actions for the document.
     *
     * @param actions The actions that are associated with this document.
     */
    public void setActions(PDDocumentCatalogAdditionalActions actions) {
        root.setItem(COSName.AA, actions);
    }

    /**
     * @return The names dictionary for this document or null if none exist.
     */
    public PDDocumentNameDictionary getNames() {
        COSDictionary names = root.getCOSDictionary(COSName.NAMES);
        return names == null ? null : new PDDocumentNameDictionary(this, names);
    }

    /**
     * @return The named destinations dictionary for this document or null if none exists.
     */
    public PDDocumentNameDestinationDictionary getDests() {
        COSDictionary dests = root.getCOSDictionary(COSName.DESTS);
        return dests != null ? new PDDocumentNameDestinationDictionary(dests) : null;
    }

    /**
     * Sets the names dictionary for the document.
     *
     * @param names The names dictionary that is associated with this document.
     */
    public void setNames(PDDocumentNameDictionary names) {
        root.setItem(COSName.NAMES, names);
    }

    /**
     * Get info about doc's usage of tagged features. This will return null if there is no
     * information.
     *
     * @return The new mark info.
     */
    public PDMarkInfo getMarkInfo() {
        COSDictionary dic = root.getCOSDictionary(COSName.MARK_INFO);
        return dic == null ? null : new PDMarkInfo(dic);
    }

    /**
     * Set information about the doc's usage of tagged features.
     *
     * @param markInfo The new MarkInfo data.
     */
    public void setMarkInfo(PDMarkInfo markInfo) {
        root.setItem(COSName.MARK_INFO, markInfo);
    }

    /**
     * Get the list of OutputIntents defined in the document.
     *
     * @return The list of PDOutputIntent, never null.
     */
    public List<PDOutputIntent> getOutputIntents() {
        List<PDOutputIntent> retval = new ArrayList<>();
        COSArray array = root.getCOSArray(COSName.OUTPUT_INTENTS);
        if (array != null) {
            for (COSBase cosBase : array) {
                if (cosBase instanceof COSObject) {
                    cosBase = ((COSObject) cosBase).getObject();
                }
                PDOutputIntent oi = new PDOutputIntent((COSDictionary) cosBase);
                retval.add(oi);
            }
        }
        return retval;
    }

    /**
     * Add an OutputIntent to the list.  If there is not OutputIntent, the list is created and the
     * first  element added.
     *
     * @param outputIntent the OutputIntent to add.
     */
    public void addOutputIntent(PDOutputIntent outputIntent) {
        COSArray array = root.getCOSArray(COSName.OUTPUT_INTENTS);
        if (array == null) {
            array = new COSArray();
            root.setItem(COSName.OUTPUT_INTENTS, array);
        }
        array.add(outputIntent.getCOSObject());
    }

    /**
     * Replace the list of OutputIntents of the document.
     *
     * @param outputIntents the list of OutputIntents, if the list is empty all OutputIntents are
     *                      removed.
     */
    public void setOutputIntents(List<PDOutputIntent> outputIntents) {
        COSArray array = new COSArray();
        for (PDOutputIntent intent : outputIntents) {
            array.add(intent.getCOSObject());
        }
        root.setItem(COSName.OUTPUT_INTENTS, array);
    }

    /**
     * Returns the page display mode.
     *
     * @return the PageMode of the document, if not present PageMode.USE_NONE is returned
     */
    public PageMode getPageMode() {
        String mode = root.getNameAsString(COSName.PAGE_MODE);
        if (mode != null) {
            try {
                return PageMode.fromString(mode);
            } catch (IllegalArgumentException e) {
                LOG.debug("Invalid PageMode used '" + mode + "' - setting to PageMode.USE_NONE", e);
                return PageMode.USE_NONE;
            }
        } else {
            return PageMode.USE_NONE;
        }
    }

    /**
     * Sets the page mode.
     *
     * @param mode The new page mode.
     */
    public void setPageMode(PageMode mode) {
        root.setName(COSName.PAGE_MODE, mode.stringValue());
    }

    /**
     * Returns the page layout.
     *
     * @return the PageLayout of the document, if not present PageLayout.SINGLE_PAGE is returned
     */
    public PageLayout getPageLayout() {
        String mode = root.getNameAsString(COSName.PAGE_LAYOUT);
        if (mode != null && !mode.isEmpty()) {
            try {
                return PageLayout.fromString(mode);
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid PageLayout used '" + mode + "' - returning PageLayout.SINGLE_PAGE", e);
            }
        }
        return PageLayout.SINGLE_PAGE;
    }

    /**
     * Sets the page layout.
     *
     * @param layout The new page layout.
     */
    public void setPageLayout(PageLayout layout) {
        root.setName(COSName.PAGE_LAYOUT, layout.stringValue());
    }

    /**
     * Returns the document-level URI.
     *
     * @return the document level URI if present, otherwise null
     */
    public PDURIDictionary getURI() {
        COSDictionary uri = root.getCOSDictionary(COSName.URI);
        return uri == null ? null : new PDURIDictionary(uri);
    }

    /**
     * Sets the document level URI.
     *
     * @param uri The new document level URI.
     */
    public void setURI(PDURIDictionary uri) {
        root.setItem(COSName.URI, uri);
    }

    /**
     * Get the document's structure tree root, or null if none exists.
     *
     * @return the structure tree root if present, otherwise null
     */
    public PDStructureTreeRoot getStructureTreeRoot() {
        COSDictionary dict = root.getCOSDictionary(COSName.STRUCT_TREE_ROOT);
        return dict == null ? null : new PDStructureTreeRoot(dict);
    }

    /**
     * Sets the document's structure tree root.
     *
     * @param treeRoot The new structure tree.
     */
    public void setStructureTreeRoot(PDStructureTreeRoot treeRoot) {
        root.setItem(COSName.STRUCT_TREE_ROOT, treeRoot);
    }

    /**
     * Returns the language for the document, or null.
     *
     * @return the language of the document if present, otherwise null
     */
    public String getLanguage() {
        return root.getString(COSName.LANG);
    }

    /**
     * Sets the Language for the document.
     *
     * @param language The new document language.
     */
    public void setLanguage(String language) {
        root.setString(COSName.LANG, language);
    }

    /**
     * Returns the PDF specification version this document conforms to.
     *
     * @return the PDF version (e.g. "1.4")
     */
    public String getVersion() {
        return root.getNameAsString(COSName.VERSION);
    }

    /**
     * Sets the PDF specification version this document conforms to.
     *
     * @param version the PDF version (e.g. "1.4")
     */
    public void setVersion(String version) {
        root.setName(COSName.VERSION, version);
    }

    /**
     * Returns the page labels descriptor of the document.
     *
     * @return the page labels descriptor of the document.
     * @throws IOException If there is a problem retrieving the page labels.
     */
    public PDPageLabels getPageLabels() throws IOException {
        COSDictionary dict = root.getCOSDictionary(COSName.PAGE_LABELS);
        return dict == null ? null : new PDPageLabels(document, dict);
    }

    /**
     * Sets the page label descriptor for the document.
     *
     * @param labels the new page label descriptor to set.
     */
    public void setPageLabels(PDPageLabels labels) {
        root.setItem(COSName.PAGE_LABELS, labels);
    }

    /**
     * Get the optional content properties dictionary associated with this document.
     *
     * @return the optional properties dictionary or null if it is not present
     */
    public PDOptionalContentProperties getOCProperties() {
        COSDictionary dict = root.getCOSDictionary(COSName.OCPROPERTIES);
        return dict == null ? null : new PDOptionalContentProperties(dict);
    }

    /**
     * Sets the optional content properties dictionary. The document version is incremented to 1.5
     * if lower.
     *
     * @param ocProperties the optional properties dictionary
     */
    public void setOCProperties(PDOptionalContentProperties ocProperties) {
        root.setItem(COSName.OCPROPERTIES, ocProperties);

        // optional content groups require PDF 1.5
        if (ocProperties != null && document.getVersion() < 1.5) {
            document.setVersion(1.5f);
        }
    }
}
