package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;

public abstract class RpmFilesizeValidator extends ElementwiseValidator {
    @Override
    public void validate(RpmInfoURI rpm) throws IOException {
        long filesize = 0;
        try (var is = rpm.getURI().toURL().openStream()) {
            while (is.read() != -1) {
                ++filesize;
            }
        }
        Decorated formattedFilesize = Decorated.actual(NumberFormat.getInstance(Locale.ENGLISH).format(filesize));

        if (allowedFilesize(rpm, filesize)) {
            pass("{0}: file size is: {1} bytes", Decorated.rpm(rpm), formattedFilesize);
        } else {
            fail("{0}: file size is: {1} bytes", Decorated.rpm(rpm), formattedFilesize);
        }
    }

    public abstract boolean allowedFilesize(RpmInfo rpm, long sizeBytes);
}
