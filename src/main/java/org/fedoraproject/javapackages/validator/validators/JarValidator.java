package org.fedoraproject.javapackages.validator.validators;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public abstract class JarValidator extends ElementwiseValidator {
    protected static final Decoration DECORATION_JAR = Decoration.bright_blue;

    protected JarValidator() {
        super(RpmInfo::isBinaryPackage);
    }

    @Override
    public void validate(RpmFile rpm) throws Exception {
        try (var is = new RpmArchiveInputStream(rpm)) {
            for (CpioArchiveEntry rpmEntry; ((rpmEntry = is.getNextEntry()) != null);) {
                if (!rpmEntry.isSymbolicLink() && rpmEntry.getName().endsWith(".jar")) {
                    var content = new byte[(int) rpmEntry.getSize()];

                    if (is.read(content) != content.length) {
                        throw Common.INCOMPLETE_READ;
                    }

                    validateJarEntry(rpm, rpmEntry, content);
                }
            }
        }
    }

    public abstract void validateJarEntry(RpmFile rpm, CpioArchiveEntry rpmEntry, byte[] content) throws Exception;
}
