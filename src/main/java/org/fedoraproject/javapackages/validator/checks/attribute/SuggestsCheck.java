package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.SuggestsConfig;

public class SuggestsCheck extends RpmAttributeCheck<SuggestsConfig> {
    public SuggestsCheck() {
        super(SuggestsConfig.class);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new SuggestsCheck().executeCheck(args));
    }
}
