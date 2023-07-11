package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;

import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.TmtTest;

@TmtTest("/no_bootstrap")
public class NoBootstrapValidator extends ElementwiseValidator {
    public NoBootstrapValidator() {
        super(RpmInfoURI::isBinaryPackage);
    }

    @Override
    public void validate(RpmInfoURI rpm) throws IOException {
        Decorated suffix = Decorated.actual("~bootstrap");
        if (rpm.getRelease().endsWith("~bootstrap")) {
            fail("{0}: Release ends with a {1} suffix", Decorated.rpm(rpm), suffix);
        } else {
            pass("{0}: Release does not end with a {1} suffix", Decorated.rpm(rpm), suffix);
        }
    }
}
