package org.fedoraproject.javapackages.validator.config;

public interface EnhancesConfig {
    boolean allowedEnhances(RpmPackage rpmPackage, String value);
}
