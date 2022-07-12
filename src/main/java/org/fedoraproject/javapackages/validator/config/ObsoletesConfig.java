package org.fedoraproject.javapackages.validator.config;

public interface ObsoletesConfig {
    boolean allowedObsoletes(RpmPackage rpmPackage, String value);
}
