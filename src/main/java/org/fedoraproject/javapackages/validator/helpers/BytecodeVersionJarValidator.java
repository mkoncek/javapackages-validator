package org.fedoraproject.javapackages.validator.helpers;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.TestResult;

import io.kojan.javadeptools.rpm.RpmPackage;

public abstract class BytecodeVersionJarValidator extends JarValidator {
    public static record Version(short major, short minor) {
        @Override
        public String toString() {
            return String.valueOf(major) + "." + String.valueOf(minor);
        }
    }

    @Override
    public void acceptJarEntry(RpmPackage rpm, CpioArchiveEntry rpmEntry, byte[] content) throws Exception {
        var jarPath = Paths.get(rpmEntry.getName().substring(1));
        var classVersions = new TreeMap<Path, Version>();

        try (var jarStream = new JarInputStream(new ByteArrayInputStream(content))) {
            for (JarEntry jarEntry; ((jarEntry = jarStream.getNextJarEntry()) != null);) {
                var classPath = Paths.get(jarEntry.getName());

                if (classPath.toString().endsWith(".class")) {
                    if (jarStream.skip(4) != 4) {
                        throw Common.INCOMPLETE_READ;
                    }

                    // ByteBuffer's initial byte order is big-endian
                    // which is the same as is used in java .class files
                    var versionBuffer = ByteBuffer.allocate(2);

                    if (jarStream.read(versionBuffer.array()) != versionBuffer.capacity()) {
                        throw Common.INCOMPLETE_READ;
                    }

                    var minorVersion = versionBuffer.getShort();
                    versionBuffer.clear();
                    if (jarStream.read(versionBuffer.array()) != versionBuffer.capacity()) {
                        throw Common.INCOMPLETE_READ;
                    }

                    var majorVersion = versionBuffer.getShort();

                    classVersions.put(classPath, new Version(majorVersion, minorVersion));
                }
            }
        }

        validate(rpm, jarPath, classVersions);

        if (TestResult.pass.equals(getResult())) {
            pass("{0}: {1}: found bytecode versions: {2}",
                    Decorated.rpm(rpm),
                    Decorated.custom(jarPath, DECORATION_JAR),
                    Decorated.list(classVersions.values().stream().distinct().toList()));
        }
    }

    public void validate(RpmPackage rpm, Path jarPath, Map<Path, Version> classVersions) {
        for (var entry : classVersions.entrySet()) {
            info("{0}: {1}: {2}: bytecode version: {3}",
                    Decorated.rpm(rpm),
                    Decorated.custom(jarPath, DECORATION_JAR),
                    Decorated.struct(entry.getKey()),
                    Decorated.actual(entry.getValue()));
        }
    }
}
