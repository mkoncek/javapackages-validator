package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.RpmInfoURI;

public class AttributeBuildRequiresValidator extends ElementwiseValidator {
    public AttributeBuildRequiresValidator() {
        super(RpmInfo::isSourcePackage);
    }

    @Override
    public void validate(RpmInfoURI rpm) throws IOException {
        // TODO
    }
}
