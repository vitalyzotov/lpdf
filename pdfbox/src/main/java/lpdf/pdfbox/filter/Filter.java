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

import lpdf.pdfbox.cos.COSArray;
import lpdf.pdfbox.cos.COSBase;
import lpdf.pdfbox.cos.COSDictionary;
import lpdf.pdfbox.cos.COSName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;

/**
 * A filter for stream data.
 *
 * @author Ben Litchfield
 * @author John Hewson
 */
public abstract class Filter {
    private static final Logger LOG = LoggerFactory.getLogger(Filter.class);

    /**
     * Compression Level System Property. Set this to a value from 0 to 9 to change the zlib deflate
     * compression level used to compress /Flate streams. The default value is -1 which is
     * {@link Deflater#DEFAULT_COMPRESSION}. To set maximum compression, use
     * {@code System.setProperty(Filter.SYSPROP_DEFLATELEVEL, "9");}
     */
    public static final String SYSPROP_DEFLATELEVEL = "lpdf.pdfbox.filter.deflatelevel";

    /**
     * Constructor.
     */
    protected Filter() {
    }

    /**
     * Decodes data, producing the original non-encoded data.
     *
     * @param encoded    the encoded byte stream
     * @param decoded    the stream where decoded data will be written
     * @param parameters the parameters used for decoding
     * @param index      the index to the filter being decoded
     * @return repaired parameters dictionary, or the original parameters dictionary
     * @throws IOException if the stream cannot be decoded
     */
    public abstract DecodeResult decode(InputStream encoded, OutputStream decoded, COSDictionary parameters,
                                        int index) throws IOException;

    /**
     * Decodes data, with optional DecodeOptions. Not all filters support all options, and so
     * callers should check the options' <code>honored</code> flag to test if they were applied.
     *
     * @param encoded    the encoded byte stream
     * @param decoded    the stream where decoded data will be written
     * @param parameters the parameters used for decoding
     * @param index      the index to the filter being decoded
     * @param options    additional options for decoding
     * @return repaired parameters dictionary, or the original parameters dictionary
     * @throws IOException if the stream cannot be decoded
     */
    public DecodeResult decode(InputStream encoded, OutputStream decoded, COSDictionary parameters,
                               int index, DecodeOptions options) throws IOException {
        return decode(encoded, decoded, parameters, index);
    }

    /**
     * Encodes data.
     *
     * @param input      the byte stream to encode
     * @param encoded    the stream where encoded data will be written
     * @param parameters the parameters used for encoding
     * @param index      the index to the filter being encoded
     * @throws IOException if the stream cannot be encoded
     */
    public final void encode(InputStream input, OutputStream encoded, COSDictionary parameters,
                             int index) throws IOException {
        encode(input, encoded, parameters.asUnmodifiableDictionary());
    }

    // implemented in subclasses
    protected abstract void encode(InputStream input, OutputStream encoded,
                                   COSDictionary parameters) throws IOException;

    // gets the decode params for a specific filter index, this is used to
    // normalise the DecodeParams entry so that it is always a dictionary
    protected COSDictionary getDecodeParams(COSDictionary dictionary, int index) {
        COSBase filter = dictionary.getDictionaryObject(COSName.F, COSName.FILTER);
        COSBase obj = dictionary.getDictionaryObject(COSName.DP, COSName.DECODE_PARMS);
        if (filter instanceof COSName && obj instanceof COSDictionary) {
            // PDFBOX-3932: The PDF specification requires "If there is only one filter and that
            // filter has parameters, DecodeParms shall be set to the filter’s parameter dictionary"
            // but tests show that Adobe means "one filter name object".
            return (COSDictionary) obj;
        } else if (filter instanceof COSArray && obj instanceof COSArray) {
            COSArray array = (COSArray) obj;
            if (index < array.size()) {
                COSBase objAtIndex = array.getObject(index);
                if (objAtIndex instanceof COSDictionary) {
                    return (COSDictionary) objAtIndex;
                }
            }
        } else if (obj != null && !(filter instanceof COSArray || obj instanceof COSArray)) {
            LOG.error("Expected DecodeParams to be an Array or Dictionary but found " +
                    obj.getClass().getName());
        }
        return new COSDictionary();
    }

    /**
     * @return the ZIP compression level configured for PDFBox
     */
    public static int getCompressionLevel() {
        int compressionLevel = Deflater.DEFAULT_COMPRESSION;
        try {
            compressionLevel = Integer.parseInt(System.getProperty(Filter.SYSPROP_DEFLATELEVEL, "-1"));
        } catch (NumberFormatException ex) {
            LOG.warn(ex.getMessage(), ex);
        }
        return Math.max(-1, Math.min(Deflater.BEST_COMPRESSION, compressionLevel));
    }
}
