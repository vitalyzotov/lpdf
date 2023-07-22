<!---
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--->

[![codeql java](https://github.com/vitalyzotov/lpdf/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/vitalyzotov/lpdf/actions/workflows/codeql-analysis.yml/badge.svg)
 
LPDF
===================================================

A port of [Apache PDFBox](https://pdfbox.apache.org/) library to be usable for parsing PDF documents in GraalVM native images without the `java.desktop` dependency. 
PDFBox is published under the Apache License, Version 2.0.

PDFBox is a project of the [Apache Software Foundation](https://www.apache.org/).

Build
-----

You need Java 8 (or higher) and [Maven 3](https://maven.apache.org/) to
build PDFBox. The recommended build command is:

    mvn clean install

The default build will compile the Java sources and package the binary
classes into jar packages. See the Maven documentation for all the
other available build options.
