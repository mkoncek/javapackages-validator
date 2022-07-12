package org.fedoraproject.javapackages.validator.config;

public interface RequiresConfig {
    boolean allowedRequires(RpmPackage rpmPackage, String value);
}
