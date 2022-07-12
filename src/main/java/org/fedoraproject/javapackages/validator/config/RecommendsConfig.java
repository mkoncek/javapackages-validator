package org.fedoraproject.javapackages.validator.config;

public interface RecommendsConfig {
    boolean allowedRecommends(RpmPackage rpmPackage, String value);
}
