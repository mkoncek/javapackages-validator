package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;

import org.fedoraproject.javadeptools.rpm.Reldep;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.TestResult;

public class AttributeProvidesValidator extends ElementwiseValidator {
    public AttributeProvidesValidator() {
        super(RpmInfo::isBinaryPackage);
    }

    @Override
    public void validate(RpmInfoURI rpm) throws IOException {
        for (Reldep provide : rpm.getProvides()) {
            if (provide.getName().startsWith("mvn(") && provide.getName().endsWith(")")) {
                if (provide.getVersion() == null) {
                    fail("{0}: Provide field {1} does not contain version",
                            Decorated.rpm(rpm), Decorated.actual(provide));
                } else if (provide.getVersion().chars().noneMatch(Character::isDigit)) {
                    fail("{0}: The provided version of field {1} does not contain a number",
                            Decorated.rpm(rpm), Decorated.actual(provide));
                }
            }
        }

        if (TestResult.pass.equals(getResult())) {
            pass("{0}: ok", Decorated.rpm(rpm));
        }
    }
}
