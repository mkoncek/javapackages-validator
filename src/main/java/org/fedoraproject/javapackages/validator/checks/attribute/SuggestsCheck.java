package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.SuggestsConfig;

public class SuggestsCheck extends RpmAttributeCheck<SuggestsConfig> {
    public SuggestsCheck() {
        this(null);
    }

    public SuggestsCheck(SuggestsConfig config) {
        super(SuggestsConfig.class, config);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new SuggestsCheck().executeCheck(args));
    }
}
