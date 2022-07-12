package org.fedoraproject.javapackages.validator.checks;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.SuggestsConfig;

public class SuggestsCheck extends RpmAttributeCheck<SuggestsConfig> {
    public static void main(String[] args) throws Exception {
        System.exit(new SuggestsCheck().executeCheck(SuggestsConfig.class, args));
    }
}
