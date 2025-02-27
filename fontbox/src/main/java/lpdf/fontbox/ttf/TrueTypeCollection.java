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

package lpdf.fontbox.ttf;

import lpdf.io.RandomAccessRead;
import lpdf.io.RandomAccessReadBuffer;
import lpdf.io.RandomAccessReadBufferedFile;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A TrueType Collection, now more properly known as a "Font Collection" as it may contain either
 * TrueType or OpenType fonts.
 *
 * @author John Hewson
 */
public class TrueTypeCollection implements Closeable {
    private final TTFDataStream stream;
    private final int numFonts;
    private final long[] fontOffsets;

    /**
     * Creates a new TrueTypeCollection from a .ttc file.
     *
     * @param file The TTC file.
     * @throws IOException If the font could not be parsed.
     */
    public TrueTypeCollection(File file) throws IOException {
        this(new RandomAccessReadBufferedFile(file));
    }

    /**
     * Creates a new TrueTypeCollection from a .ttc input stream.
     *
     * @param stream A TTC input stream.
     * @throws IOException If the font could not be parsed.
     */
    public TrueTypeCollection(InputStream stream) throws IOException {
        this(new RandomAccessReadBuffer(stream));
    }

    /**
     * Creates a new TrueTypeCollection from a RandomAccessRead.
     *
     * @param randomAccessRead
     * @throws IOException If the font could not be parsed.
     */
    TrueTypeCollection(RandomAccessRead randomAccessRead) throws IOException {
        this.stream = new RandomAccessReadDataStream(randomAccessRead);

        // TTC header
        String tag = stream.readTag();
        if (!tag.equals("ttcf")) {
            throw new IOException("Missing TTC header");
        }
        float version = stream.read32Fixed();
        numFonts = (int) stream.readUnsignedInt();
        if (numFonts <= 0 || numFonts > 1024) {
            throw new IOException("Invalid number of fonts " + numFonts);
        }
        fontOffsets = new long[numFonts];
        for (int i = 0; i < numFonts; i++) {
            fontOffsets[i] = stream.readUnsignedInt();
        }
        if (version >= 2) {
            // not used at this time
            int ulDsigTag = stream.readUnsignedShort();
            int ulDsigLength = stream.readUnsignedShort();
            int ulDsigOffset = stream.readUnsignedShort();
        }
    }

    /**
     * Run the callback for each TT font in the collection.
     *
     * @param trueTypeFontProcessor the object with the callback method.
     * @throws IOException if something went wrong when calling the TrueTypeFontProcessor
     */
    public void processAllFonts(TrueTypeFontProcessor trueTypeFontProcessor) throws IOException {
        for (int i = 0; i < numFonts; i++) {
            TrueTypeFont font = getFontAtIndex(i);
            trueTypeFontProcessor.process(font);
        }
    }

    private TrueTypeFont getFontAtIndex(int idx) throws IOException {
        stream.seek(fontOffsets[idx]);
        TTFParser parser;
        if (stream.readTag().equals("OTTO")) {
            parser = new OTFParser(false);
        } else {
            parser = new TTFParser(false);
        }
        stream.seek(fontOffsets[idx]);
        return parser.parse(new TTCDataStream(stream));
    }

    /**
     * Get a TT font from a collection.
     *
     * @param name The postscript name of the font.
     * @return The found font, nor null if none is found.
     * @throws IOException if there is an error reading the font data
     */
    public TrueTypeFont getFontByName(String name) throws IOException {
        for (int i = 0; i < numFonts; i++) {
            TrueTypeFont font = getFontAtIndex(i);
            if (font.getName().equals(name)) {
                return font;
            }
        }
        return null;
    }

    /**
     * Implement the callback method to call {@link TrueTypeCollection#processAllFonts(TrueTypeFontProcessor)}.
     */
    @FunctionalInterface
    public interface TrueTypeFontProcessor {
        void process(TrueTypeFont ttf) throws IOException;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
