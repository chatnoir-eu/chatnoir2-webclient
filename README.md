# ChatNoir Web Frontend

This is the source code for the [ChatNoir](https://www.chatnoir.eu/) web frontend.
It is the center piece of the ChatNoir search engine and is written in Java 8
using the Servlet API 3.0+ and Elasticsearch Transport API 5.6+ and has been published
under the terms of the MIT License.

ChatNoir is subject to research and development at the Web Technology & Information
Systems Group at Bauhaus-Universität Weimar.

## Compiling the Source Code
The source code can be compiled using Gradle and the resulting WAR file can be deployed
to a servlet container, e.g. Tomcat.

To build the sources, call  

    gradle build

This will download all third-party dependencies, compile the source code and
create a WAR file under `build/libs`.
