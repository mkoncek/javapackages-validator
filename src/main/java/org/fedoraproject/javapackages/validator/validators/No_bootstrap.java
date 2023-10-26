package org.fedoraproject.javapackages.validator.validators;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javapackages.validator.Decorated;

public class No_bootstrap extends ElementwiseValidator {
    @Override
    public void validate(RpmFile rpm) throws Exception {
        Decorated suffix = Decorated.actual("~bootstrap");
        if (rpm.getInfo().getRelease().endsWith("~bootstrap")) {
            fail("{0}: Release ends with a {1} suffix", Decorated.rpm(rpm), suffix);
        } else {
            pass("{0}: Release does not end with a {1} suffix", Decorated.rpm(rpm), suffix);
        }
    }
}
