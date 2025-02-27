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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

/**
 * Decompresses data encoded using the zlib/deflate compression method,
 * reproducing the original text or binary data.
 *
 * @author Ben Litchfield
 * @author Marcel Kammer
 */
final class FlateFilter extends Filter {
    private static final Logger LOG = LoggerFactory.getLogger(FlateFilter.class);

    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded,
                               COSDictionary parameters, int index) throws IOException {
        final COSDictionary decodeParams = getDecodeParams(parameters, index);

        try {
            decompress(encoded, Predictor.wrapPredictor(decoded, decodeParams));
        } catch (DataFormatException e) {
            // if the stream is corrupt a DataFormatException may occur
            LOG.error("FlateFilter: stop reading corrupt stream due to a DataFormatException");

            // re-throw the exception
            throw new IOException(e);
        }
        return new DecodeResult(parameters);
    }

    // Use Inflater instead of InflateInputStream to avoid an EOFException due to a probably
    // missing Z_STREAM_END, see PDFBOX-1232 for details
    private void decompress(InputStream in, OutputStream out) throws IOException, DataFormatException {
        byte[] buf = new byte[2048];
        // skip zlib header
        in.read();
        in.read();
        int read = in.read(buf);
        if (read > 0) {
            // use nowrap mode to bypass zlib-header and checksum to avoid a DataFormatException
            Inflater inflater = new Inflater(true);
            inflater.setInput(buf, 0, read);
            byte[] res = new byte[1024];
            boolean dataWritten = false;
            try {
                while (true) {
                    int resRead = 0;
                    try {
                        resRead = inflater.inflate(res);
                    } catch (DataFormatException exception) {
                        if (dataWritten) {
                            // some data could be read -> don't throw an exception
                            LOG.warn("FlateFilter: premature end of stream due to a DataFormatException");
                            break;
                        } else {
                            // nothing could be read -> re-throw exception
                            throw exception;
                        }
                    }
                    if (resRead != 0) {
                        out.write(res, 0, resRead);
                        dataWritten = true;
                        continue;
                    }
                    if (inflater.finished() || inflater.needsDictionary() || in.available() == 0) {
                        break;
                    }
                    read = in.read(buf);
                    inflater.setInput(buf, 0, read);
                }
            } finally {
                inflater.end();
            }
        }
        out.flush();
    }

    @Override
    protected void encode(InputStream input, OutputStream encoded, COSDictionary parameters)
            throws IOException {
        int compressionLevel = getCompressionLevel();
        Deflater deflater = new Deflater(compressionLevel);
        try (DeflaterOutputStream out = new DeflaterOutputStream(encoded, deflater)) {
            IOUtils.copy(input, out);
        }
        encoded.flush();
        deflater.end();
    }
}
