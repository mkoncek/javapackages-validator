package org.fedoraproject.javapackages.validator.validators;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.TestResult;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public class BytecodeVersionJarValidator extends JarValidator {
    protected static final Decoration DECORATION_CLASS = Decoration.bright_yellow;

    @Override
    public void validateJarEntry(RpmInfoURI rpm, CpioArchiveEntry rpmEntry, byte[] content) throws IOException {
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


        validate(rpm, jarName, classVersions);

        if (TestResult.pass.equals(getResult())) {
            pass("{0}: {1}: found bytecode versions: {2}",
                    Decorated.rpm(rpm),
                    Decorated.custom(jarName, DECORATION_JAR),
                    Decorated.list(classVersions.values().stream().sorted().distinct().toList()));
        }
    }

    public void validate(RpmInfoURI rpm, String jarName, Map<String, Integer> classVersions) {
        for (var entry : classVersions.entrySet()) {
            info("{0}: {1}: {2}: bytecode version: {3}",
                    Decorated.rpm(rpm),
                    Decorated.custom(jarName, DECORATION_JAR),
                    Decorated.custom(entry.getKey(), DECORATION_CLASS),
                    Decorated.actual(entry.getValue()));
        }
    }
}
