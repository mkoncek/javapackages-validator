package org.fedoraproject.javapackages.validator.checks;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.OrderWithRequiresConfig;

public class OrderWithRequiresCheck extends RpmAttributeCheck<OrderWithRequiresConfig> {
    public static void main(String[] args) throws Exception {
        System.exit(new OrderWithRequiresCheck().executeCheck(OrderWithRequiresConfig.class, args));
    }
}
