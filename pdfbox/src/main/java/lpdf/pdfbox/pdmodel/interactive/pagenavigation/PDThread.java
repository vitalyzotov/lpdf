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
package lpdf.pdfbox.pdmodel.interactive.pagenavigation;

import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.pdmodel.PDDocumentInformation;
import lpdf.pdfbox.pdmodel.common.COSObjectable;

/**
 * This a single thread in a PDF document.
 *
 * @author Ben Litchfield
 */
public class PDThread implements COSObjectable {


    private final COSDictionary thread;

    /**
     * Constructor that is used for a preexisting dictionary.
     *
     * @param t The underlying dictionary.
     */
    public PDThread(COSDictionary t) {
        thread = t;
    }

    /**
     * Default constructor.
     */
    public PDThread() {
        thread = new COSDictionary();
        thread.setItem(COSName.TYPE, COSName.THREAD);
    }

    /**
     * This will get the underlying dictionary that this object wraps.
     *
     * @return The underlying info dictionary.
     */
    @Override
    public COSDictionary getCOSObject() {
        return thread;
    }

    /**
     * Get info about the thread, or null if there is nothing.
     *
     * @return The thread information.
     */
    public PDDocumentInformation getThreadInfo() {
        COSDictionary info = thread.getCOSDictionary(COSName.I);
        return info != null ? new PDDocumentInformation(info) : null;
    }

    /**
     * Set the thread info, can be null.
     *
     * @param info The info dictionary about this thread.
     */
    public void setThreadInfo(PDDocumentInformation info) {
        thread.setItem(COSName.I, info);
    }

    /**
     * Get the first bead in the thread, or null if it has not been set yet.  This
     * is a required field for this object.
     *
     * @return The first bead in the thread.
     */
    public PDThreadBead getFirstBead() {
        COSDictionary bead = thread.getCOSDictionary(COSName.F);
        return bead != null ? new PDThreadBead(bead) : null;
    }

    /**
     * This will set the first bead in the thread.  When this is set it will
     * also set the thread property of the bead object.
     *
     * @param bead The first bead in the thread.
     */
    public void setFirstBead(PDThreadBead bead) {
        if (bead != null) {
            bead.setThread(this);
        }
        thread.setItem(COSName.F, bead);
    }


}
