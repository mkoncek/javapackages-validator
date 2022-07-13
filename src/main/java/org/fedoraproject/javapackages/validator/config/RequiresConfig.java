package org.fedoraproject.javapackages.validator.config;

public interface RequiresConfig {
    boolean allowedRequires(RpmPackage rpm, String value);
}
