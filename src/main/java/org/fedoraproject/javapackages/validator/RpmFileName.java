package org.fedoraproject.javapackages.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RpmFileName {
    public final String name;
    public final String epoch;
    public final String version;
    public final String release;
    public final String disttag;
    public final String arch;

    private static final Pattern rpmPattern = Pattern.compile("(.*)-([^:]*:)?(.*)-(.*)\\.(.*)\\.(.*)\\.rpm");

    public RpmFileName(String rpmFileName) {
        Matcher matcher;

        if (!(matcher = rpmPattern.matcher(rpmFileName)).matches()) {
            throw new IllegalArgumentException("Could not parse " + rpmFileName + " as an rpm file name");
        }

        name = matcher.group(1);

        String varEpoch = matcher.group(2);

        if (varEpoch != null) {
            varEpoch = varEpoch.substring(0, varEpoch.length() - 1);
        }

        epoch = varEpoch;
        version = matcher.group(3);
        release = matcher.group(4);
        disttag = matcher.group(5);
        arch = matcher.group(6);
    }
}
