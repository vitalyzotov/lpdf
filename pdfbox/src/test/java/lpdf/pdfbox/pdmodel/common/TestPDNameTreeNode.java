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
package lpdf.pdfbox.pdmodel.common;

import lpdf.pdfbox.cos.COSInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A test case for PDNameTreeNode.
 *
 * @author Koch
 */
class TestPDNameTreeNode {
    private PDNameTreeNode<COSInteger> node1;
    private PDNameTreeNode<COSInteger> node2;
    private PDNameTreeNode<COSInteger> node4;
    private PDNameTreeNode<COSInteger> node5;
    private PDNameTreeNode<COSInteger> node24;

    @BeforeEach
    protected void setUp() throws Exception {
        this.node5 = new PDIntegerNameTreeNode();
        Map<String, COSInteger> names = new TreeMap<>();
        names.put("Actinium", COSInteger.get(89));
        names.put("Aluminum", COSInteger.get(13));
        names.put("Americium", COSInteger.get(95));
        names.put("Antimony", COSInteger.get(51));
        names.put("Argon", COSInteger.get(18));
        names.put("Arsenic", COSInteger.get(33));
        names.put("Astatine", COSInteger.get(85));
        this.node5.setNames(names);

        this.node24 = new PDIntegerNameTreeNode();
        names = new TreeMap<>();
        names.put("Xenon", COSInteger.get(54));
        names.put("Ytterbium", COSInteger.get(70));
        names.put("Yttrium", COSInteger.get(39));
        names.put("Zinc", COSInteger.get(30));
        names.put("Zirconium", COSInteger.get(40));
        this.node24.setNames(names);

        this.node2 = new PDIntegerNameTreeNode();
        List<PDNameTreeNode<COSInteger>> kids = this.node2.getKids();
        if (kids == null) {
            kids = new COSArrayList<>();
        }
        kids.add(this.node5);
        this.node2.setKids(kids);

        this.node4 = new PDIntegerNameTreeNode();
        kids = this.node4.getKids();
        if (kids == null) {
            kids = new COSArrayList<>();
        }
        kids.add(this.node24);
        this.node4.setKids(kids);

        this.node1 = new PDIntegerNameTreeNode();
        kids = this.node1.getKids();
        if (kids == null) {
            kids = new COSArrayList<>();
        }
        kids.add(this.node2);
        kids.add(this.node4);
        this.node1.setKids(kids);
    }

    @Test
    void testUpperLimit() throws IOException {
        assertEquals("Astatine", this.node5.getUpperLimit());
        assertEquals("Astatine", this.node2.getUpperLimit());

        assertEquals("Zirconium", this.node24.getUpperLimit());
        assertEquals("Zirconium", this.node4.getUpperLimit());

        assertEquals(null, this.node1.getUpperLimit());
    }

    @Test
    void testLowerLimit() throws IOException {
        assertEquals("Actinium", this.node5.getLowerLimit());
        assertEquals("Actinium", this.node2.getLowerLimit());

        assertEquals("Xenon", this.node24.getLowerLimit());
        assertEquals("Xenon", this.node4.getLowerLimit());

        assertEquals(null, this.node1.getLowerLimit());
    }
}
