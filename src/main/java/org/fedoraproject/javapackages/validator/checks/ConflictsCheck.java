package org.fedoraproject.javapackages.validator.checks;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.ConflictsConfig;

public class ConflictsCheck extends RpmAttributeCheck<ConflictsConfig> {
    public static void main(String[] args) throws Exception {
        System.exit(new ConflictsCheck().executeCheck(ConflictsConfig.class, args));
    }
}
