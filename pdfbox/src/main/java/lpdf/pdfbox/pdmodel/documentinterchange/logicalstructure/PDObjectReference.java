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
package lpdf.pdfbox.pdmodel.documentinterchange.logicalstructure;

import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSStream;
import lpdf.pdfbox.pdmodel.common.COSObjectable;
import lpdf.pdfbox.pdmodel.graphics.PDXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * An object reference.
 * <p>
 * This is described as "Entries in an object reference dictionary" in the PDF specification.
 *
 * @author Johannes Koch
 */
public class PDObjectReference implements COSObjectable {
    /**
     * Log instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PDObjectReference.class);

    /**
     * TYPE of this object.
     */
    public static final String TYPE = "OBJR";

    private final COSDictionary dictionary;

    /**
     * Default Constructor.
     */
    public PDObjectReference() {
        this.dictionary = new COSDictionary();
        this.dictionary.setName(COSName.TYPE, TYPE);
    }

    /**
     * Constructor for an existing object reference.
     *
     * @param theDictionary The existing dictionary.
     */
    public PDObjectReference(COSDictionary theDictionary) {
        dictionary = theDictionary;
    }

    /**
     * Returns the underlying dictionary.
     *
     * @return the dictionary
     */
    @Override
    public COSDictionary getCOSObject() {
        return this.dictionary;
    }

    /**
     * Gets a higher-level object for the referenced object.
     * Currently this method may return
     * a {@link PDXObject} or <code>null</code>.
     *
     * @return a higher-level object for the referenced object
     */
    public COSObjectable getReferencedObject() {
        COSDictionary objDictionary = getCOSObject().getCOSDictionary(COSName.OBJ);
        if (objDictionary == null) {
            return null;
        }
        try {
            if (objDictionary instanceof COSStream) {
                PDXObject xobject = PDXObject.createXObject(objDictionary, null); // <-- TODO: valid?
                if (xobject != null) {
                    return xobject;
                }
            }
        } catch (IOException exception) {
            LOG.debug("Couldn't get the referenced object - returning null instead", exception);
            // this can only happen if the target is an XObject.
        }
        return null;
    }

    /**
     * Sets the referenced XObject.
     *
     * @param xobject the referenced XObject
     */
    public void setReferencedObject(PDXObject xobject) {
        this.getCOSObject().setItem(COSName.OBJ, xobject);
    }

}
