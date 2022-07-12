package org.fedoraproject.javapackages.validator;

import java.io.IOException;

public class Common {
    public static final IOException INCOMPLETE_READ = new IOException("Incomplete read in RPM stream");

    public static String getPackageName(String sourceRPM) {
        String result = sourceRPM;
        result = result.substring(0, result.lastIndexOf('-'));
        result = result.substring(0, result.lastIndexOf('-'));

        if (result.isEmpty()) {
            throw new RuntimeException("Could not read package name for source RPM: " + sourceRPM);
        }

        return result;
    }
}
