package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.validators.DuplicateFileValidator.DuplicateFileValidatorDefault;

public class DuplicateFileValidatorJP extends DuplicateFileValidatorDefault {
    @Override
    public boolean validateDefault(Path path, Collection<? extends RpmInfo> providerRpms) throws IOException {
        if (path.toString().startsWith("/usr/share/licenses/")) {
            return providerRpms.stream().map(RpmInfo::getPackageName).distinct().count() == 1;
        }

        if (providerRpms.stream().allMatch(rpm -> rpm.getName().startsWith("maven-openjdk"))) {
            return true;
        }

        if (providerRpms.stream().allMatch(rpm -> rpm.getName().startsWith("maven-local-openjdk"))) {
            return true;
        }

        return false;
    }
}
