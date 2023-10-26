package org.fedoraproject.javapackages.validator.validators.java;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.validators.ElementwiseValidator;

public class Javadoc_noarch extends ElementwiseValidator {
    public Javadoc_noarch() {
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
