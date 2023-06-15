package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;

import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;

public class JavadocNoarchValidator extends ElementwiseValidator {
    public JavadocNoarchValidator() {
        super(rpm -> rpm.isBinaryPackage() && rpm.getName().equals(rpm.getPackageName() + "-javadoc"));
    }

    @Override
    public void validate(RpmInfoURI rpm) throws IOException {
        if (!rpm.getArch().equals("noarch")) {
            fail("{0} is a javadoc package but its architecture is {1}",
                    Decorated.rpm(rpm),
                    Decorated.actual(rpm.getArch()));
        } else {
            pass("{0} is a javadoc package and its architecture is {1}",
                    Decorated.rpm(rpm),
                    Decorated.actual("noarch"));
        }
    }
}
