package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.EnhancesConfig;

public class EnhancesCheck extends RpmAttributeCheck<EnhancesConfig> {
    public EnhancesCheck() {
        super(EnhancesConfig.class);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new EnhancesCheck().executeCheck(args));
    }
}
