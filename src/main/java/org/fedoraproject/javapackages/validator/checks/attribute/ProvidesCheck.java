package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.ProvidesConfig;

public class ProvidesCheck extends RpmAttributeCheck<ProvidesConfig> {
    public ProvidesCheck() {
        this(null);
    }

    public ProvidesCheck(ProvidesConfig config) {
        super(ProvidesConfig.class, config);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new ProvidesCheck().executeCheck(args));
    }
}
