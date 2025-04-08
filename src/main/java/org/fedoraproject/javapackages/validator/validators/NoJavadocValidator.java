package org.fedoraproject.javapackages.validator.validators;

import org.fedoraproject.javapackages.validator.spi.Decorated;
import org.fedoraproject.javapackages.validator.util.Common;
import org.fedoraproject.javapackages.validator.util.ElementwiseValidator;

import io.kojan.javadeptools.rpm.RpmPackage;

public class NoJavadocValidator extends ElementwiseValidator {
    @Override
    public String getTestName() {
        return "/java/no-javadoc";
    }

    public NoJavadocValidator() {
        super(rpm -> !rpm.isSourcePackage() && rpm.getName().equals(Common.getPackageName(rpm) + "-javadoc"));
    }

    @Override
    public void validate(RpmPackage rpm) throws Exception {
        fail("{0}: all '-javadoc' subpackages are expected to be dropped",
                Decorated.rpm(rpm));
    }
}
