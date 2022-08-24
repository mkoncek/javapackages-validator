package org.fedoraproject.javapackages.validator.validators.jp;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.validators.RpmFilesizeValidator;

public class RpmFilesizeValidatorJP extends RpmFilesizeValidator {
    @Override
    public boolean allowedFilesize(RpmInfo rpm, long sizeBytes) {
        if (rpm.isSourcePackage()) {
            return sizeBytes <= 1_000_000_000;
        }

        if (rpm.getPackageName().equals("javapackages-bootstrap")) {
            return sizeBytes <= 40_000_000;
        }

        // javadoc rpms
        if (rpm.getName().equals(rpm.getPackageName() + "-javadoc")) {
            return sizeBytes <= 4_000_000;
        }

        // maven-lib and xmvn-minimal bundle dependencies during bootstrap
        if (rpm.getRelease().endsWith("~bootstrap")) {
            if (rpm.getName().equals("maven-lib") || rpm.getName().equals("xmvn-minimal")) {
                return sizeBytes <= 10_000_000;
            }
        }

        return sizeBytes <= 3_500_000;
    }
}
