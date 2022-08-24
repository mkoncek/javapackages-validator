package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmPathInfo;

public abstract class FilesValidator extends ElementwiseValidator {
    public abstract boolean allowedFile(RpmInfo rpm, Path path);

    @Override
    public void validate(RpmPathInfo rpm) throws IOException {
        try (var is = new RpmArchiveInputStream(rpm.getPath())) {
            boolean pass = true;
            for (CpioArchiveEntry rpmEntry; ((rpmEntry = is.getNextEntry()) != null);) {
                Path entryName = Common.getEntryPath(rpmEntry);

                if (!allowedFile(rpm, entryName)) {
                    pass = false;
                    fail("{0}: Illegal file: {1}",
                            Decorated.rpm(rpm),
                            Decorated.actual(entryName));
                }
            }

            if (pass) {
                pass("{0}: ok", Decorated.rpm(rpm));
            }
        }
    }
}
