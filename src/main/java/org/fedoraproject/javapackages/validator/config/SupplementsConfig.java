package org.fedoraproject.javapackages.validator.config;

public interface SupplementsConfig {
    boolean allowedSupplements(RpmPackage rpmPackage, String value);
}
