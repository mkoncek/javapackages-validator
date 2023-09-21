package org.fedoraproject.javapackages.validator.validators;

import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Locale;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Decorated;

public abstract class RpmFilesizeValidator extends ElementwiseValidator {
    @Override
    public void validate(RpmFile rpm) throws Exception {
        long filesize = 0;
        try (var is = rpm.getContent(); var os = OutputStream.nullOutputStream()) {
            filesize = is.transferTo(os);
        }
        Decorated formattedFilesize = Decorated.actual(NumberFormat.getInstance(Locale.ENGLISH).format(filesize));

        if (allowedFilesize(rpm.getInfo(), filesize)) {
            pass("{0}: file size is: {1} bytes", Decorated.rpm(rpm), formattedFilesize);
        } else {
            fail("{0}: file size is: {1} bytes", Decorated.rpm(rpm), formattedFilesize);
        }
    }

    public abstract boolean allowedFilesize(RpmInfo rpm, long sizeBytes);
}
