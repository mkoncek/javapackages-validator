package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.RequiresConfig;

public class RequiresCheck extends RpmAttributeCheck<RequiresConfig> {
    public RequiresCheck() {
        this(null);
    }

    public RequiresCheck(RequiresConfig config) {
        super(RequiresConfig.class, config);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new RequiresCheck().executeCheck(args));
    }
}
