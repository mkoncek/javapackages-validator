package org.fedoraproject.javapackages.validator.validators;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmPathInfo;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public abstract class BytecodeVersionJarValidator extends ElementwiseValidator {
    protected static final Decoration DECORATION_JAR = Decoration.bright_blue;

    @Override
    public void validate(RpmPathInfo rpm) throws IOException {
        try (var is = new RpmArchiveInputStream(rpm.getPath())) {
            for (CpioArchiveEntry rpmEntry; ((rpmEntry = is.getNextEntry()) != null);) {
                var content = new byte[(int) rpmEntry.getSize()];

                if (is.read(content) != content.length) {
                    throw Common.INCOMPLETE_READ;
                }

                if (!rpmEntry.isSymbolicLink() && rpmEntry.getName().endsWith(".jar")) {
                    String jarName = rpmEntry.getName().substring(1);
                    var classVersions = new TreeMap<String, Integer>();

                    try (var jarStream = new JarArchiveInputStream(new ByteArrayInputStream(content))) {
                        for (JarArchiveEntry jarEntry; ((jarEntry = jarStream.getNextJarEntry()) != null);) {
                            String className = jarEntry.getName();

                            if (className.endsWith(".class")) {
                                // Read 6-th and 7-th bytes which indicate the .class bytecode version
                                if (jarStream.skip(6) != 6) {
                                    throw Common.INCOMPLETE_READ;
                                }

                                // ByteBuffer's initial byte order is big-endian
                                // which is the same as is used in java .class files
                                var versionBuffer = ByteBuffer.allocate(2);

                                if (jarStream.read(versionBuffer.array()) != versionBuffer.capacity()) {
                                    throw Common.INCOMPLETE_READ;
                                }

                                var version = versionBuffer.getShort();

                                classVersions.put(className, Integer.valueOf(version));
                            }
                        }
                    }

                    validate(rpm, jarName, Collections.unmodifiableMap(classVersions));

                    if (!failed()) {
                        passMessages.clear();
                        pass("{0}: {1}: found bytecode versions: {2}",
                                Decorated.rpm(rpm),
                                Decorated.custom(jarName, DECORATION_JAR),
                                Decorated.list(classVersions.values().stream().distinct().toList()));
                    }
                }
            }
        }
    }

    public abstract void validate(RpmPathInfo rpm, String jarName, Map<String, Integer> classVersions);

    public static abstract class BytecodeVersionClassValidator extends BytecodeVersionJarValidator {
        protected static final Decoration DECORATION_CLASS = Decoration.bright_yellow;

        @Override
        public void validate(RpmPathInfo rpm, String jarName, Map<String, Integer> classVersions) {
            for (var entry : classVersions.entrySet()) {
                validate(rpm, jarName, entry.getKey(), entry.getValue());
            }
        }

        public abstract void validate(RpmPathInfo rpm, String jarName, String className, int version);
    }
}
