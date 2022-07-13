package org.fedoraproject.javapackages.validator.config;

public interface SupplementsConfig {
    boolean allowedSupplements(RpmPackage rpm, String value);
}
