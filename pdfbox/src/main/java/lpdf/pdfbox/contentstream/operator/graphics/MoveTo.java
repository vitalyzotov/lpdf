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
package lpdf.pdfbox.contentstream.operator.graphics;

import lpdf.harmony.awt.geom.Point2D;
import lpdf.pdfbox.contentstream.PDFGraphicsStreamEngine;
import lpdf.pdfbox.contentstream.operator.MissingOperandException;
import lpdf.pdfbox.contentstream.operator.Operator;
import lpdf.pdfbox.contentstream.operator.OperatorName;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSNumber;

import java.io.IOException;
import java.util.List;

/**
 * m Begins a new subpath.
 *
 * @author Ben Litchfield
 */
public final class MoveTo extends GraphicsOperatorProcessor {
    public MoveTo(PDFGraphicsStreamEngine context) {
        super(context);
    }

    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException {
        if (operands.size() < 2) {
            throw new MissingOperandException(operator, operands);
        }
        COSBase base0 = operands.get(0);
        if (!(base0 instanceof COSNumber)) {
            return;
        }
        COSBase base1 = operands.get(1);
        if (!(base1 instanceof COSNumber)) {
            return;
        }
        COSNumber x = (COSNumber) base0;
        COSNumber y = (COSNumber) base1;
        PDFGraphicsStreamEngine context = getGraphicsContext();
        Point2D.Float pos = context.transformedPoint(x.floatValue(), y.floatValue());
        context.moveTo(pos.x, pos.y);
    }

    @Override
    public String getName() {
        return OperatorName.MOVE_TO;
    }
}
