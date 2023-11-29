package org.fedoraproject.javapackages.validator.validators;

import org.fedoraproject.javapackages.validator.util.ElementwiseValidator;

import io.kojan.javadeptools.rpm.RpmInfo;
import io.kojan.javadeptools.rpm.RpmPackage;

public class AttributeBuildRequiresValidator extends ElementwiseValidator {
    @Override
    public String getTestName() {
        return "/java/attributes/build-requires";
    }

    public AttributeBuildRequiresValidator() {
        super(RpmInfo::isSourcePackage);
    }

    @Override
    public void validate(RpmPackage rpm) throws Exception {
        // TODO
    }
}
