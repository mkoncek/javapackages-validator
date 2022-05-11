package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public class Common {
    static final IOException INCOMPLETE_READ = new IOException("Incomplete read in RPM stream");

    public static String getPackageName(Path rpmPath) {
        try {
            String sourceRPM = new RpmInfo(rpmPath).getSourceRPM();
            String result = sourceRPM;
            result = result.substring(0, result.lastIndexOf('-'));
            result = result.substring(0, result.lastIndexOf('-'));

            if (result.isEmpty()) {
                throw new RuntimeException(MessageFormat.format(
                        "Could not read package name for binary RPM: {0}, source RPM: {1}",
                        rpmPath, sourceRPM));
            }

            return result;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
