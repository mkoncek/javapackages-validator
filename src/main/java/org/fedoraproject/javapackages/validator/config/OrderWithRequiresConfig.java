package org.fedoraproject.javapackages.validator.config;

public interface OrderWithRequiresConfig {
    boolean allowedOrderWithRequires(RpmPackage rpm, String value);
}
