package org.fedoraproject.javapackages.validator.validators.java.attributes;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.validators.ElementwiseValidator;

public class Build_requires extends ElementwiseValidator {
    public Build_requires() {
        super(RpmInfo::isSourcePackage);
    }

    @Override
    public void validate(RpmFile rpm) throws Exception {
        // TODO
    }
}
