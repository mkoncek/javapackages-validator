package org.fedoraproject.javapackages.validator.util;

import java.io.OutputStream;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.Locale;

import org.fedoraproject.javapackages.validator.spi.Decorated;

import io.kojan.javadeptools.rpm.RpmInfo;
import io.kojan.javadeptools.rpm.RpmPackage;

public abstract class RpmFilesizeValidator extends ElementwiseValidator {
    @Override
    public void validate(RpmPackage rpm) throws Exception {
        long filesize = 0;
        try (var is = Files.newInputStream(rpm.getPath()); var os = OutputStream.nullOutputStream()) {
            filesize = is.transferTo(os);
        }
        Decorated formattedFilesize = Decorated.actual(NumberFormat.getInstance(Locale.ENGLISH).format(filesize));

        if (allowedFilesize(rpm.getInfo(), filesize)) {
            pass("{0}: file size is: {1} bytes", Decorated.rpm(rpm), formattedFilesize);
        } else {
            fail("{0}: file size is: {1} bytes", Decorated.rpm(rpm), formattedFilesize);
        }
    }

    public abstract boolean allowedFilesize(RpmInfo rpm, long sizeBytes) throws Exception;
}
