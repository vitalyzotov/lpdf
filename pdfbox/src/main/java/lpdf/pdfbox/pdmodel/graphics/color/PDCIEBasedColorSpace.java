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
package lpdf.pdfbox.pdmodel.graphics.color;

/**
 * CIE-based colour spaces specify colours in a way that is independent of the characteristics
 * of any particular output device. They are based on an international standard for colour
 * specification created by the Commission Internationale de l'Éclairage (CIE).
 *
 * @author John Hewson
 */
public abstract class PDCIEBasedColorSpace extends PDColorSpace {
    @Override
    public String toString() {
        return getName();   // TODO return more info
    }
}
