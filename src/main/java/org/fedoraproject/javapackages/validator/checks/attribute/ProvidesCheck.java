package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.ProvidesConfig;

public class ProvidesCheck extends RpmAttributeCheck<ProvidesConfig> {
    public ProvidesCheck() {
        super(ProvidesConfig.class);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new ProvidesCheck().executeCheck(args));
    }
}
