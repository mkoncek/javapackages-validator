package org.fedoraproject.javapackages.validator.config;

public interface ConflictsConfig {
    boolean allowedConflicts(RpmPackage rpmPackage, String value);
}
