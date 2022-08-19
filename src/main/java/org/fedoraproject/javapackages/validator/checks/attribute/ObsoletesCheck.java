package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.ObsoletesConfig;

public class ObsoletesCheck extends RpmAttributeCheck<ObsoletesConfig> {
    public ObsoletesCheck() {
        super(ObsoletesConfig.class);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new ObsoletesCheck().executeCheck(args));
    }
}
