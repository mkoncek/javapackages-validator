package org.fedoraproject.javapackages.validator.validators;

import org.fedoraproject.javadeptools.rpm.Reldep;
import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.TestResult;
import org.fedoraproject.javapackages.validator.helpers.ElementwiseValidator;

public class AttributeRequiresValidator extends ElementwiseValidator {
    @Override
    public String getTestName() {
        return "/java/attributes/requires";
    }

    public AttributeRequiresValidator() {
        super(RpmInfo::isBinaryPackage);
    }

    @Override
    public void validate(RpmFile rpm) throws Exception {
        boolean jpFilesystem = false;

        for (Reldep require : rpm.getInfo().getRequires()) {
            jpFilesystem |= require.getName().equals("javapackages-filesystem");
            if (require.getName().startsWith("mvn(") && require.getName().endsWith(")")) {
                if (require.getVersion() != null && require.getVersion().chars().noneMatch(Character::isDigit)) {
                    fail("{0}: The required version of field {1} does not contain a number",
                            Decorated.rpm(rpm), Decorated.actual(require));
                }
            }
        }

        if (!jpFilesystem) {
            // fail("{0}: Requires field does not contain javapackages-filesystem", Decorated.rpm(rpm));
        }

        if (!TestResult.fail.equals(getResult())) {
            pass("{0}: RPM attribute Requires ok", Decorated.rpm(rpm));
        }
    }
}
