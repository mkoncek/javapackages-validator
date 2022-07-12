package org.fedoraproject.javapackages.validator.config;

public interface SuggestsConfig {
    boolean allowedSuggests(RpmPackage rpmPackage, String value);
}
