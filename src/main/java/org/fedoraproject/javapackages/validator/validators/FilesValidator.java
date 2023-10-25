package org.fedoraproject.javapackages.validator.validators;

import java.nio.file.Path;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;

public abstract class FilesValidator extends ElementwiseValidator {
    public abstract boolean allowedFile(RpmInfo rpm, Path path) throws Exception;

    @Override
    public void validate(RpmFile rpm) throws Exception {
        try (var is = new RpmArchiveInputStream(rpm)) {
            boolean pass = true;
            for (CpioArchiveEntry rpmEntry; ((rpmEntry = is.getNextEntry()) != null);) {
                Path entryName = Common.getEntryPath(rpmEntry);

                if (!allowedFile(rpm.getInfo(), entryName)) {
                    pass = false;
                    fail("{0}: Illegal file: {1}",
                            Decorated.rpm(rpm),
                            Decorated.actual(entryName));
                }
            }

            if (pass) {
                pass("{0}: Listed files - ok", Decorated.rpm(rpm));
            }
        }
    }
}
