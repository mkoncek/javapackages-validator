package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.SupplementsConfig;

public class SupplementsCheck extends RpmAttributeCheck<SupplementsConfig> {
    public SupplementsCheck() {
        super(SupplementsConfig.class);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new SupplementsCheck().executeCheck(args));
    }
}
