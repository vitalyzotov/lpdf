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
package lpdf.pdfbox.pdmodel.graphics.shading;

import lpdf.pdfbox.cos.COSDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common resources for shading types 6 and 7
 */
abstract class PDMeshBasedShadingType extends PDShadingType4 {

    private static final Logger LOG = LoggerFactory.getLogger(PDMeshBasedShadingType.class);

    PDMeshBasedShadingType(COSDictionary shadingDictionary) {
        super(shadingDictionary);
    }

}
