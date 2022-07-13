package org.fedoraproject.javapackages.validator.config;

public interface RecommendsConfig {
    boolean allowedRecommends(RpmPackage rpm, String value);
}
