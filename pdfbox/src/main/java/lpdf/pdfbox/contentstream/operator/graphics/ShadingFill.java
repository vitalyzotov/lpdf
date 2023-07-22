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

import java.io.IOException;
import java.util.List;

import lpdf.pdfbox.contentstream.PDFGraphicsStreamEngine;
import lpdf.pdfbox.contentstream.operator.MissingOperandException;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSName;
import lpdf.pdfbox.contentstream.operator.Operator;
import lpdf.pdfbox.contentstream.operator.OperatorName;

/**
 * sh Fills the clipping area with the given shading pattern.
 *
 * @author Daniel Wilson
 */
public final class ShadingFill extends GraphicsOperatorProcessor
{
    public ShadingFill(PDFGraphicsStreamEngine context)
    {
        super(context);
    }

    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException
    {
        if (operands.isEmpty())
        {
            throw new MissingOperandException(operator, operands);
        }
        COSBase base = operands.get(0);
        if (!(base instanceof COSName))
        {
            throw new MissingOperandException(operator, operands);
        }
        getGraphicsContext().shadingFill((COSName) base);
    }

    @Override
    public String getName()
    {
        return OperatorName.SHADING_FILL;
    }
}
