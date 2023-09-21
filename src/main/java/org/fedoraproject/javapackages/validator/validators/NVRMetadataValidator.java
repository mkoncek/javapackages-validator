package org.fedoraproject.javapackages.validator.validators;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.jar.JarInputStream;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;

public class NVRMetadataValidator extends JarValidator {
    private static interface Entry {
        String name();
        String valueOf(RpmFile rpm);
    }

    private static class RpmName implements Entry {
        @Override
        public String name() {
            return "Rpm-Name";
        }
        @Override
        public String valueOf(RpmFile rpm) {
            return rpm.getInfo().getName();
        }
    }

    private static class RpmEpoch implements Entry {
        @Override
        public String name() {
            return "Rpm-Epoch";
        }
        @Override
        public String valueOf(RpmFile rpm) {
            return String.valueOf(rpm.getInfo().getEpoch());
        }
    }

    private static class RpmVersion implements Entry {
        @Override
        public String name() {
            return "Rpm-Version";
        }
        @Override
        public String valueOf(RpmFile rpm) {
            return rpm.getInfo().getVersion();
        }
    }

    private static class RpmRelease implements Entry {
        @Override
        public String name() {
            return "Rpm-Release";
        }
        @Override
        public String valueOf(RpmFile rpm) {
            return rpm.getInfo().getRelease();
        }
    }

    private static List<Entry> ENTRIES = List.of(new RpmName(), new RpmEpoch(), new RpmVersion(), new RpmRelease());

    @Override
    public void validateJarEntry(RpmFile rpm, CpioArchiveEntry rpmEntry, byte[] content) throws Exception {
        try (var is = new JarInputStream(new ByteArrayInputStream(content))) {
            var mf = is.getManifest();
            var attrs = mf.getMainAttributes();

            for (var entry : ENTRIES) {
                var attrValue = attrs.getValue(entry.name());

                if (attrValue == null) {
                    fail("{0}: {1}: Jar manifest attribute {2} is not present",
                            Decorated.rpm(rpm),
                            Decorated.custom(Common.getEntryPath(rpmEntry), DECORATION_JAR),
                            Decorated.struct(entry.name()));
                } else if (entry.valueOf(rpm).equals(attrValue)) {
                    pass("{0}: {1}: Jar manifest attribute {2} with value {3} exactly matches the RPM attribute",
                            Decorated.rpm(rpm),
                            Decorated.custom(Common.getEntryPath(rpmEntry), DECORATION_JAR),
                            Decorated.struct(entry.name()),
                            Decorated.actual(attrValue));
                } else {
                    fail("{0}: {1}: Jar manifest attribute {2} with value {3} does not match the RPM attribute value {4}",
                            Decorated.rpm(rpm),
                            Decorated.custom(Common.getEntryPath(rpmEntry), DECORATION_JAR),
                            Decorated.struct(entry.name()),
                            Decorated.actual(attrValue),
                            Decorated.expected(entry.valueOf(rpm)));
                }
            }
        }
    }
}
