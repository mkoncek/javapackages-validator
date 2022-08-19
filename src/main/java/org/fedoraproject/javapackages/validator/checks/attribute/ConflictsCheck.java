package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.ConflictsConfig;

public class ConflictsCheck extends RpmAttributeCheck<ConflictsConfig> {
    public ConflictsCheck() {
        super(ConflictsConfig.class);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new ConflictsCheck().executeCheck(args));
    }
}
