package org.fedoraproject.javapackages.validator.validators;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;

public class AttributeBuildRequiresValidator extends ElementwiseValidator {
    @Override
    public String getTestName() {
        return "/java/attributes/build-requires";
    }

    public AttributeBuildRequiresValidator() {
        super(RpmInfo::isSourcePackage);
    }

    @Override
    public void validate(RpmFile rpm) throws Exception {
        // TODO
    }
}
