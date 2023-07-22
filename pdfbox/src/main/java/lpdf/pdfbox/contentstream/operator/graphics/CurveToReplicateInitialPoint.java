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
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lpdf.pdfbox.contentstream.PDFGraphicsStreamEngine;
import lpdf.pdfbox.contentstream.operator.MissingOperandException;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSNumber;
import lpdf.pdfbox.contentstream.operator.Operator;
import lpdf.pdfbox.contentstream.operator.OperatorName;

/**
 * v Append curved segment to path with the initial point replicated.
 *
 * @author Ben Litchfield
 */
public class CurveToReplicateInitialPoint extends GraphicsOperatorProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(CurveToReplicateInitialPoint.class);

    public CurveToReplicateInitialPoint(PDFGraphicsStreamEngine context)
    {
        super(context);
    }

    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException
    {
        if (operands.size() < 4)
        {
            throw new MissingOperandException(operator, operands);
        }
        if (!checkArrayTypesClass(operands, COSNumber.class))
        {
            return;
        }
        COSNumber x2 = (COSNumber)operands.get(0);
        COSNumber y2 = (COSNumber)operands.get(1);
        COSNumber x3 = (COSNumber)operands.get(2);
        COSNumber y3 = (COSNumber)operands.get(3);

        PDFGraphicsStreamEngine context = getGraphicsContext();
        Point2D currentPoint = context.getCurrentPoint();

        Point2D.Float point2 = context.transformedPoint(x2.floatValue(), y2.floatValue());
        Point2D.Float point3 = context.transformedPoint(x3.floatValue(), y3.floatValue());

        if (currentPoint == null)
        {
            LOG.warn("curveTo (" + point3.x + "," + point3.y + ") without initial MoveTo");
            context.moveTo(point3.x, point3.y);
        }
        else
        {
            context.curveTo((float) currentPoint.getX(), (float) currentPoint.getY(),
                    point2.x, point2.y,
                    point3.x, point3.y);
        }
    }

    @Override
    public String getName()
    {
        return OperatorName.CURVE_TO_REPLICATE_INITIAL_POINT;
    }
}
