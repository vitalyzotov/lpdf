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
import lpdf.pdfbox.contentstream.operator.Operator;
import lpdf.pdfbox.contentstream.operator.OperatorName;
import lpdf.pdfbox.contentstream.operator.OperatorProcessor;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSName;

import java.io.IOException;
import java.util.List;

/**
 * BMC : Begins a marked-content sequence.
 *
 * @author Johannes Koch
 */
public class BeginMarkedContentSequence extends OperatorProcessor {
    public BeginMarkedContentSequence(PDFStreamEngine context) {
        super(context);
    }

    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException {
        COSName tag = null;
        for (COSBase argument : arguments) {
            if (argument instanceof COSName) {
                tag = (COSName) argument;
            }
        }
        getContext().beginMarkedContentSequence(tag, null);
    }

    @Override
    public String getName() {
        return OperatorName.BEGIN_MARKED_CONTENT;
    }
}
