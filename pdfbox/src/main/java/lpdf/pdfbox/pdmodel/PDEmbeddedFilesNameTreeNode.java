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

import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.pdmodel.common.PDNameTreeNode;
import lpdf.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;

import java.io.IOException;

/**
 * This class holds all of the name trees that are available at the document level.
 *
 * @author Ben Litchfield
 */
public class PDEmbeddedFilesNameTreeNode extends PDNameTreeNode<PDComplexFileSpecification> {
    /**
     * Constructor.
     */
    public PDEmbeddedFilesNameTreeNode() {
        super();
    }

    /**
     * Constructor.
     *
     * @param dic The COS dictionary.
     */
    public PDEmbeddedFilesNameTreeNode(COSDictionary dic) {
        super(dic);
    }

    @Override
    protected PDComplexFileSpecification convertCOSToPD(COSBase base) throws IOException {
        return new PDComplexFileSpecification((COSDictionary) base);
    }

    @Override
    protected PDNameTreeNode<PDComplexFileSpecification> createChildNode(COSDictionary dic) {
        return new PDEmbeddedFilesNameTreeNode(dic);
    }
}
