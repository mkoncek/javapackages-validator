package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.Locale;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmPathInfo;

public abstract class RpmFilesizeValidator extends ElementwiseValidator {
    @Override
    public void validate(RpmPathInfo rpm) throws IOException {
        long filesize = Files.size(rpm.getPath());
        Decorated formattedFilesize = Decorated.actual(NumberFormat.getInstance(Locale.ENGLISH).format(filesize));

        if (allowedFilesize(rpm, filesize)) {
            pass("{0}: file size is: {1} bytes", Decorated.rpm(rpm), formattedFilesize);
        } else {
            fail("{0}: file size is: {1} bytes", Decorated.rpm(rpm), formattedFilesize);
        }
    }

    public abstract boolean allowedFilesize(RpmInfo rpm, long sizeBytes);
}
