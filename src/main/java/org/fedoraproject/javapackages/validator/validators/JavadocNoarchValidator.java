package org.fedoraproject.javapackages.validator.validators;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.helpers.ElementwiseValidator;

public class JavadocNoarchValidator extends ElementwiseValidator {
    @Override
    public String getTestName() {
        return "/java/javadoc_noarch";
    }

    public JavadocNoarchValidator() {
        super(rpm -> rpm.isBinaryPackage() && rpm.getName().equals(rpm.getPackageName() + "-javadoc"));
    }

    @Override
    public void validate(RpmFile rpm) throws Exception {
        if (!rpm.getInfo().getArch().equals("noarch")) {
            fail("{0} is a javadoc package but its architecture is {1}",
                    Decorated.rpm(rpm),
                    Decorated.actual(rpm.getInfo().getArch()));
        } else {
            pass("{0} is a javadoc package and its architecture is {1}",
                    Decorated.rpm(rpm),
                    Decorated.actual("noarch"));
        }
    }
}
