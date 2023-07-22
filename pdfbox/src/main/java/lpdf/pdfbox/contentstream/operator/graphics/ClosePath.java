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

import lpdf.pdfbox.contentstream.PDFGraphicsStreamEngine;
import lpdf.pdfbox.contentstream.operator.Operator;
import lpdf.pdfbox.contentstream.operator.OperatorName;
import lpdf.pdfbox.cos.COSBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * h Close the path.
 *
 * @author Ben Litchfield
 */
public final class ClosePath extends GraphicsOperatorProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ClosePath.class);

    public ClosePath(PDFGraphicsStreamEngine context) {
        super(context);
    }

    @Override
    public void process(Operator operator, List<COSBase> operands) throws IOException {
        if (getGraphicsContext().getCurrentPoint() == null) {
            LOG.warn("ClosePath without initial MoveTo");
            return;
        }
        getGraphicsContext().closePath();
    }

    @Override
    public String getName() {
        return OperatorName.CLOSE_PATH;
    }
}
