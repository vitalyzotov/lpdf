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
package lpdf.pdfbox.filter;

import lpdf.io.IOUtils;
import lpdf.pdfbox.cos.COSDictionary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The IdentityFilter filter passes the data through without any modifications.
 * It is defined in section 7.6.5 of the PDF 1.7 spec and also stated in table 26.
 *
 * @author Adam Nichols
 */
final class IdentityFilter extends Filter {
    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded,
                               COSDictionary parameters, int index)
            throws IOException {
        IOUtils.copy(encoded, decoded);
        decoded.flush();
        return new DecodeResult(parameters);
    }

    @Override
    protected void encode(InputStream input, OutputStream encoded, COSDictionary parameters)
            throws IOException {
        IOUtils.copy(input, encoded);
        encoded.flush();
    }
}
