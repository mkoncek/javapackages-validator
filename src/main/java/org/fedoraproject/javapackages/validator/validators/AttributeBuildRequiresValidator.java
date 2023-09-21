package org.fedoraproject.javapackages.validator.validators;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;

public class AttributeBuildRequiresValidator extends ElementwiseValidator {
    public AttributeBuildRequiresValidator() {
        super(RpmInfo::isSourcePackage);
    }

    @Override
    public void validate(RpmFile rpm) throws Exception {
        // TODO
    }
}
