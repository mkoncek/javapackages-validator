package org.fedoraproject.javapackages.validator.helpers;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.TestResult;

public abstract class BytecodeVersionJarValidator extends JarValidator {
    @Override
    public void acceptJarEntry(RpmFile rpm, CpioArchiveEntry rpmEntry, byte[] content) throws Exception {
        var jarPath = Paths.get(rpmEntry.getName().substring(1));
        var classVersions = new TreeMap<Path, Short>();

        try (var jarStream = new JarArchiveInputStream(new ByteArrayInputStream(content))) {
            for (JarArchiveEntry jarEntry; ((jarEntry = jarStream.getNextJarEntry()) != null);) {
                var classPath = Paths.get(jarEntry.getName());

                if (classPath.toString().endsWith(".class")) {
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

                    classVersions.put(classPath, Short.valueOf(version));
                }
            }
        }

        validate(rpm, jarPath, classVersions);

        if (TestResult.pass.equals(getResult())) {
            pass("{0}: {1}: found bytecode versions: {2}",
                    Decorated.rpm(rpm),
                    Decorated.custom(jarPath, DECORATION_JAR),
                    Decorated.list(classVersions.values().stream().sorted().distinct().toList()));
        }
    }

    public void validate(RpmFile rpm, Path jarPath, Map<Path, Short> classVersions) {
        for (var entry : classVersions.entrySet()) {
            info("{0}: {1}: {2}: bytecode version: {3}",
                    Decorated.rpm(rpm),
                    Decorated.custom(jarPath, DECORATION_JAR),
                    Decorated.struct(entry.getKey()),
                    Decorated.actual(entry.getValue()));
        }
    }
}
