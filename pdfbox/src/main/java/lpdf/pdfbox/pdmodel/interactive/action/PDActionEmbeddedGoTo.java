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
package lpdf.pdfbox.pdmodel.interactive.action;

import lpdf.pdfbox.cos.COSBoolean;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.pdmodel.common.filespecification.PDFileSpecification;

import java.io.IOException;

/**
 * This represents a embedded go-to action that can be executed in a PDF document.
 *
 * @author Ben Litchfield
 * @author Panagiotis Toumasis
 * @author Tilman Hausherr
 */
public class PDActionEmbeddedGoTo extends PDAction {
    /**
     * This type of action this object represents.
     */
    public static final String SUB_TYPE = "GoToE";

    /**
     * Default constructor.
     */
    public PDActionEmbeddedGoTo() {
        setSubType(SUB_TYPE);
    }

    /**
     * Constructor.
     *
     * @param a The action dictionary.
     */
    public PDActionEmbeddedGoTo(COSDictionary a) {
        super(a);
    }

    /**
     * This will get the file in which the destination is located.
     *
     * @return The F entry of the specific embedded go-to action dictionary.
     * @throws IOException If there is an error creating the file spec.
     */
    public PDFileSpecification getFile() throws IOException {
        return PDFileSpecification.createFS(getCOSObject().getDictionaryObject(COSName.F));
    }

    /**
     * This will set the file in which the destination is located.
     *
     * @param fs The file specification.
     */
    public void setFile(PDFileSpecification fs) {
        getCOSObject().setItem(COSName.F, fs);
    }

    /**
     * This will specify whether to open the destination document in a new window, in the same
     * window, or behave in accordance with the current user preference.
     *
     * @return A flag specifying how to open the destination document.
     */
    public OpenMode getOpenInNewWindow() {
        if (getCOSObject().getDictionaryObject(COSName.NEW_WINDOW) instanceof COSBoolean) {
            COSBoolean b = (COSBoolean) getCOSObject().getDictionaryObject(COSName.NEW_WINDOW);
            return b.getValue() ? OpenMode.NEW_WINDOW : OpenMode.SAME_WINDOW;
        }
        return OpenMode.USER_PREFERENCE;
    }

    /**
     * This will specify whether to open the destination document in a new window.
     *
     * @param value The flag value.
     */
    public void setOpenInNewWindow(OpenMode value) {
        if (null == value) {
            getCOSObject().removeItem(COSName.NEW_WINDOW);
            return;
        }
        switch (value) {
            case USER_PREFERENCE:
                getCOSObject().removeItem(COSName.NEW_WINDOW);
                break;
            case SAME_WINDOW:
                getCOSObject().setBoolean(COSName.NEW_WINDOW, false);
                break;
            case NEW_WINDOW:
                getCOSObject().setBoolean(COSName.NEW_WINDOW, true);
                break;
            default:
                // shouldn't happen unless the enum type is changed
                break;
        }
    }

    /**
     * Get the target directory.
     *
     * @return the target directory or null if there is none.
     */
    public PDTargetDirectory getTargetDirectory() {
        COSDictionary targetDict = getCOSObject().getCOSDictionary(COSName.T);
        return targetDict != null ? new PDTargetDirectory(targetDict) : null;
    }

    /**
     * Sets the target directory.
     *
     * @param targetDirectory the target directory
     */
    public void setTargetDirectory(PDTargetDirectory targetDirectory) {
        getCOSObject().setItem(COSName.T, targetDirectory);
    }
}
