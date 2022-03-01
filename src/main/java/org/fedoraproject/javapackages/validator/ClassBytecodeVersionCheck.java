/*-
 * Copyright (c) 2022 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fedoraproject.javapackages.validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;

public class ClassBytecodeVersionCheck {
	private static final String INCOMPLETE_READ = "Incomplete read in RPM stream";
	
    static Collection<String> checkClassBytecodeVersion(Path path, int lower, int upper) throws IOException {
        if (lower > upper) {
            throw new IllegalArgumentException("ClassBytecodeVersionCheck::checkClassBytecodeVersion: parameter `lower` is larger than parameter `upper`");
        }

        var result = new ArrayList<String>(0);

        try (var is = new RpmArchiveInputStream(path)) {
            for (CpioArchiveEntry rpmEntry; ((rpmEntry = is.getNextEntry()) != null);) {
                var content = new byte[(int) rpmEntry.getSize()];
                if (is.read(content) != content.length) {
                    throw new IOException(INCOMPLETE_READ);
                }

                if (! rpmEntry.isSymbolicLink() && rpmEntry.getName().endsWith(".jar")) {
                    String jarName = rpmEntry.getName().substring(1);

                    try (var jarStream = new JarArchiveInputStream(new ByteArrayInputStream(content))) {
                        for (JarArchiveEntry jarEntry; ((jarEntry = jarStream.getNextJarEntry()) != null);) {
                            String className = jarEntry.getName();

                            if (className.endsWith(".class")) {
                                // Read 6-th and 7-th bytes which indicate the .class bytecode version
                                if (jarStream.skip(6) != 6) {
                                    throw new IOException(INCOMPLETE_READ);
                                }

                                // ByteBuffer's initial byte order is big endian
                                // which is the same as is used in java .class files
                                var versionBuffer = ByteBuffer.allocate(2);
                                if (jarStream.read(versionBuffer.array()) != versionBuffer.capacity()) {
                                    throw new IOException(INCOMPLETE_READ);
                                }

                                var version = versionBuffer.getShort();

                                System.out.println("version is: " + version);
                                
                                if (version < lower || upper < version) {
                                    result.add(MessageFormat.format(
                                            "{0}: {1}: {2}: class bytecode version is {3} which is not in range <{4}-{5}>",
                                            path, jarName, className, version, lower, upper));
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        int returnCode = 0;

        int lower = Integer.parseInt(args[0]);
        int upper = Integer.parseInt(args[1]);

        for (int i = 2; i != args.length; ++i) {
            for (var message : checkClassBytecodeVersion(Paths.get(args[i]).resolve(".").toAbsolutePath().normalize(), lower, upper)) {
                returnCode = 1;
                System.out.println(message);
            }
        }

        System.exit(returnCode);
    }
}
