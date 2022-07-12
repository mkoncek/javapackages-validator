package org.fedoraproject.javapackages.validator.config;

public interface OrderWithRequiresConfig {
    boolean allowedOrderWithRequires(RpmPackage rpmPackage, String value);
}
