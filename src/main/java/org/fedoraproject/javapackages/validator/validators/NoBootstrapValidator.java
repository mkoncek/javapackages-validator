package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.util.function.Predicate;

import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmPathInfo;

public class NoBootstrapValidator extends ElementwiseValidator {
    public NoBootstrapValidator() {
        super(Predicate.not(RpmPathInfo::isSourcePackage));
    }

    @Override
    public void validate(RpmPathInfo rpm) throws IOException {
        Decorated suffix = Decorated.actual("~bootstrap");
        if (rpm.getRelease().endsWith("~bootstrap")) {
            fail("{0}: Release ends with {1} suffix", Decorated.rpm(rpm), suffix);
        } else {
            pass("{0}: Release does not end with a {1} suffix", Decorated.rpm(rpm), suffix);
        }
    }
}
