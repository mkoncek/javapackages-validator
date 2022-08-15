package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.ObsoletesConfig;

public class ObsoletesCheck extends RpmAttributeCheck<ObsoletesConfig> {
    public ObsoletesCheck() {
        this(null);
    }

    public ObsoletesCheck(ObsoletesConfig config) {
        super(ObsoletesConfig.class, config);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new ObsoletesCheck().executeCheck(args));
    }
}
