package org.fedoraproject.javapackages.validator.validators;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarInputStream;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;

public class NVRMetadataValidator extends JarValidator {
    private static interface Entry {
        String name();
        String valueOf(RpmInfoURI rpm);
    }

    private static class RpmName implements Entry {
        @Override
        public String name() {
            return "Rpm-Name";
        }
        @Override
        public String valueOf(RpmInfoURI rpm) {
            return rpm.getName();
        }
    }

    private static class RpmEpoch implements Entry {
        @Override
        public String name() {
            return "Rpm-Epoch";
        }
        @Override
        public String valueOf(RpmInfoURI rpm) {
            return String.valueOf(rpm.getEpoch());
        }
    }

    private static class RpmVersion implements Entry {
        @Override
        public String name() {
            return "Rpm-Version";
        }
        @Override
        public String valueOf(RpmInfoURI rpm) {
            return rpm.getVersion();
        }
    }

    private static class RpmRelease implements Entry {
        @Override
        public String name() {
            return "Rpm-Release";
        }
        @Override
        public String valueOf(RpmInfoURI rpm) {
            return rpm.getRelease();
        }
    }

    private static List<Entry> ENTRIES = List.of(new RpmName(), new RpmEpoch(), new RpmVersion(), new RpmRelease());

    @Override
    public void validateJarEntry(RpmInfoURI rpm, CpioArchiveEntry rpmEntry, byte[] content) throws IOException {
        try (var is = new JarInputStream(new ByteArrayInputStream(content))) {
            var mf = is.getManifest();
            var attrs = mf.getMainAttributes();
            for (var entry : ENTRIES) {
                var attrValue = attrs.getValue(entry.name());

                if (attrValue == null) {
                    fail("{0}: {1}: Attribute {2} is not present",
                            Decorated.rpm(rpm),
                            Decorated.custom(Common.getEntryPath(rpmEntry), DECORATION_JAR),
                            Decorated.struct(entry.name()));
                } else if (entry.valueOf(rpm).equals(attrValue)) {
                    pass("{0}: {1}: Attribute {2} with value {3} exactly matches the RPM attribute",
                            Decorated.rpm(rpm),
                            Decorated.custom(Common.getEntryPath(rpmEntry), DECORATION_JAR),
                            Decorated.struct(entry.name()),
                            Decorated.actual(attrValue));
                } else {
                    fail("{0}: {1}: Attribute {2} with value {3} does not match the RPM attribute {4}",
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
