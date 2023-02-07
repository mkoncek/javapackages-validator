package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;

import org.fedoraproject.javadeptools.rpm.Reldep;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.TestResult;

public class AttributeRequiresValidator extends ElementwiseValidator {
    public AttributeRequiresValidator() {
        super(RpmInfo::isBinaryPackage);
    }

    @Override
    public void validate(RpmInfoURI rpm) throws IOException {
        boolean jpFilesystem = false;

        for (Reldep require : rpm.getRequires()) {
            jpFilesystem |= require.getName().equals("javapackages-filesystem");
            if (require.getName().startsWith("mvn(") && require.getName().endsWith(")")) {
                if (require.getVersion() != null && require.getVersion().chars().noneMatch(Character::isDigit)) {
                    fail("{0}: The required version of field {1} does not contain a number",
                            Decorated.rpm(rpm), Decorated.actual(require));
                }
            }
        }

        if (!jpFilesystem) {
            fail("{0}: Requires field does not contain javapackages-filesystem", Decorated.rpm(rpm));
        }

        if (TestResult.pass.equals(getResult())) {
            pass("{0}: ok", Decorated.rpm(rpm));
        }
    }
}
