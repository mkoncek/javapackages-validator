package org.fedoraproject.javapackages.validator.checks.attribute;

import org.fedoraproject.javapackages.validator.RpmAttributeCheck;
import org.fedoraproject.javapackages.validator.config.attribute.OrderWithRequiresConfig;

public class OrderWithRequiresCheck extends RpmAttributeCheck<OrderWithRequiresConfig> {
    public OrderWithRequiresCheck() {
        super(OrderWithRequiresConfig.class);
    }

    public static void main(String[] args) throws Exception {
        System.exit(new OrderWithRequiresCheck().executeCheck(args));
    }
}
