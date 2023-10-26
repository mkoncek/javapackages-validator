package org.fedoraproject.javapackages.validator.validators.java.attributes;

import org.fedoraproject.javadeptools.rpm.Reldep;
import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.TestResult;
import org.fedoraproject.javapackages.validator.validators.ElementwiseValidator;

public class Provides extends ElementwiseValidator {
    public Provides() {
        super(RpmInfo::isBinaryPackage);
    }

    @Override
    public void validate(RpmFile rpm) throws Exception {
        for (Reldep provide : rpm.getInfo().getProvides()) {
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

        if (!TestResult.fail.equals(getResult())) {
            pass("{0}: RPM attribute Provides - ok", Decorated.rpm(rpm));
        }
    }
}
