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
package lpdf.fontbox.ttf.gsub;

import lpdf.fontbox.ttf.GlyphSubstitutionTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * A default implementation of {@link GsubWorker} that actually does not transform the glyphs yet allows to correctly
 * {@linkplain GlyphSubstitutionTable#getGsubData(String) load} GSUB table data even from fonts for which a complete
 * glyph substitution is not implemented.
 *
 * @author Vladimir Plizga
 */
class DefaultGsubWorker implements GsubWorker {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultGsubWorker.class);

    @Override
    public List<Integer> applyTransforms(List<Integer> originalGlyphIds) {
        LOG.warn(getClass().getSimpleName() + " class does not perform actual GSUB substitutions. "
                + "Perhaps the selected language is not yet supported by the FontBox library.");
        // Make the result read-only to prevent accidental modifications of the source list
        return Collections.unmodifiableList(originalGlyphIds);
    }
}
