package org.fedoraproject.javapackages.validator.config;

public interface ProvidesConfig {
    boolean allowedProvides(RpmPackage rpm, String value);
}
