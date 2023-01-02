package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public abstract class JarValidator extends ElementwiseValidator {
    protected static final Decoration DECORATION_JAR = Decoration.bright_blue;

    protected JarValidator() {
        super(RpmInfo::isBinaryPackage);
    }

    @Override
    public final void validate(RpmInfoURI rpm) throws IOException {
        try (var is = new RpmArchiveInputStream(rpm.getURI().toURL())) {
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

    public abstract void validateJarEntry(RpmInfoURI rpm, CpioArchiveEntry rpmEntry, byte[] content) throws IOException;
}
