package org.fedoraproject.javapackages.validator;

import java.io.IOException;

public class Common {
    static final IOException INCOMPLETE_READ = new IOException("Incomplete read in RPM stream");
}
