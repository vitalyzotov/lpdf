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
import lpdf.pdfbox.cos.COSNumber;
import lpdf.pdfbox.pdmodel.graphics.state.RenderingMode;

import java.io.IOException;
import java.util.List;

/**
 * Tr: Set text rendering mode.
 *
 * @author Ben Litchfield
 */
public class SetTextRenderingMode extends OperatorProcessor {
    public SetTextRenderingMode(PDFStreamEngine context) {
        super(context);
    }

    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException {
        if (arguments.isEmpty()) {
            throw new MissingOperandException(operator, arguments);
        }
        COSBase base0 = arguments.get(0);
        if (!(base0 instanceof COSNumber)) {
            return;
        }
        COSNumber mode = (COSNumber) base0;
        int val = mode.intValue();
        if (val < 0 || val >= RenderingMode.values().length) {
            return;
        }
        RenderingMode renderingMode = RenderingMode.fromInt(val);
        getContext().getGraphicsState().getTextState().setRenderingMode(renderingMode);
    }

    @Override
    public String getName() {
        return OperatorName.SET_TEXT_RENDERINGMODE;
    }
}
