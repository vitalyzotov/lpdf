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

package lpdf.pdfbox.contentstream.operator.text;

import lpdf.pdfbox.contentstream.PDFStreamEngine;
import lpdf.pdfbox.contentstream.operator.MissingOperandException;
import lpdf.pdfbox.contentstream.operator.Operator;
import lpdf.pdfbox.contentstream.operator.OperatorName;
import lpdf.pdfbox.contentstream.operator.OperatorProcessor;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.cos.COSNumber;
import lpdf.pdfbox.pdmodel.font.PDFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Tf: Set text font and size.
 *
 * @author Laurent Huault
 */
public class SetFontAndSize extends OperatorProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SetFontAndSize.class);

    public SetFontAndSize(PDFStreamEngine context) {
        super(context);
    }

    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException {
        if (arguments.size() < 2) {
            throw new MissingOperandException(operator, arguments);
        }

        COSBase base0 = arguments.get(0);
        COSBase base1 = arguments.get(1);
        if (!(base0 instanceof COSName)) {
            return;
        }
        if (!(base1 instanceof COSNumber)) {
            return;
        }
        COSName fontName = (COSName) base0;
        float fontSize = ((COSNumber) base1).floatValue();
        PDFStreamEngine context = getContext();
        context.getGraphicsState().getTextState().setFontSize(fontSize);
        PDFont font = context.getResources().getFont(fontName);
        if (font == null) {
            LOG.warn("font '" + fontName.getName() + "' not found in resources");
        }
        context.getGraphicsState().getTextState().setFont(font);
    }

    @Override
    public String getName() {
        return OperatorName.SET_FONT_AND_SIZE;
    }
}
