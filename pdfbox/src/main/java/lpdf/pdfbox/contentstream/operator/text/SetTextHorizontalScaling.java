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

import java.io.IOException;
import java.util.List;

/**
 * Tz: Set horizontal text scaling.
 *
 * @author Ben Litchfield
 */
public class SetTextHorizontalScaling extends OperatorProcessor {
    public SetTextHorizontalScaling(PDFStreamEngine context) {
        super(context);
    }

    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException {
        if (arguments.isEmpty()) {
            throw new MissingOperandException(operator, arguments);
        }

        COSBase base = arguments.get(0);
        if (!(base instanceof COSNumber)) {
            return;
        }
        COSNumber scaling = (COSNumber) base;
        getContext().getGraphicsState().getTextState().setHorizontalScaling(scaling.floatValue());
    }

    @Override
    public String getName() {
        return OperatorName.SET_TEXT_HORIZONTAL_SCALING;
    }
}
