package org.fedoraproject.javapackages.validator.checks;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.ProvidesConfig;

public class ProvidesCheck extends RpmAttributeCheck<ProvidesConfig> {
    public static void main(String[] args) throws Exception {
        System.exit(new ProvidesCheck().executeCheck(ProvidesConfig.class, args));
    }
}
