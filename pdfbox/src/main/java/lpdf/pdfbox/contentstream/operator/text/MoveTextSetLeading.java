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
import lpdf.pdfbox.cos.COSFloat;
import lpdf.pdfbox.cos.COSNumber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TD: Move text position and set leading.
 *
 * @author Laurent Huault
 */
public class MoveTextSetLeading extends OperatorProcessor {
    public MoveTextSetLeading(PDFStreamEngine context) {
        super(context);
    }

    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException {
        if (arguments.size() < 2) {
            throw new MissingOperandException(operator, arguments);
        }

        //move text position and set leading
        COSBase base1 = arguments.get(1);
        if (!(base1 instanceof COSNumber)) {
            return;
        }
        COSNumber y = (COSNumber) base1;

        List<COSBase> args = new ArrayList<>();
        args.add(new COSFloat(-y.floatValue()));
        PDFStreamEngine context = getContext();
        context.processOperator(OperatorName.SET_TEXT_LEADING, args);
        context.processOperator(OperatorName.MOVE_TEXT, arguments);
    }

    @Override
    public String getName() {
        return OperatorName.MOVE_TEXT_SET_LEADING;
    }
}
