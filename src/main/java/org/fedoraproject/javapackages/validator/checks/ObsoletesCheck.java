package org.fedoraproject.javapackages.validator.checks;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.ObsoletesConfig;

public class ObsoletesCheck extends RpmAttributeCheck<ObsoletesConfig> {
    public static void main(String[] args) throws Exception {
        System.exit(new ObsoletesCheck().executeCheck(ObsoletesConfig.class, args));
    }
}
