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
package lpdf.pdfbox.contentstream.operator.markedcontent;

import lpdf.pdfbox.contentstream.PDFStreamEngine;
import lpdf.pdfbox.contentstream.operator.MissingOperandException;
import lpdf.pdfbox.contentstream.operator.Operator;
import lpdf.pdfbox.contentstream.operator.OperatorName;
import lpdf.pdfbox.contentstream.operator.OperatorProcessor;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.pdmodel.graphics.PDXObject;
import lpdf.pdfbox.pdmodel.graphics.form.PDFormXObject;
import lpdf.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import lpdf.pdfbox.text.PDFMarkedContentExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Do: Draws an XObject.
 *
 * @author Ben Litchfield
 * @author Mario Ivankovits
 */
public class DrawObject extends OperatorProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(DrawObject.class);

    public DrawObject(PDFStreamEngine context) {
        super(context);
    }

    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException {
        if (arguments.isEmpty()) {
            throw new MissingOperandException(operator, arguments);
        }
        COSBase base0 = arguments.get(0);
        if (!(base0 instanceof COSName)) {
            return;
        }
        COSName name = (COSName) base0;
        PDFStreamEngine context = getContext();
        PDXObject xobject = context.getResources().getXObject(name);
        ((PDFMarkedContentExtractor) context).xobject(xobject);

        if (xobject instanceof PDFormXObject) {
            try {
                context.increaseLevel();
                if (context.getLevel() > 50) {
                    LOG.error("recursion is too deep, skipping form XObject");
                    return;
                }
                if (xobject instanceof PDTransparencyGroup) {
                    context.showTransparencyGroup((PDTransparencyGroup) xobject);
                } else {
                    context.showForm((PDFormXObject) xobject);
                }
            } finally {
                context.decreaseLevel();
            }
        }
    }

    @Override
    public String getName() {
        return OperatorName.DRAW_OBJECT;
    }
}
