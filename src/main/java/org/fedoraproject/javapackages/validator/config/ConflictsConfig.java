package org.fedoraproject.javapackages.validator.config;

public interface ConflictsConfig {
    boolean allowedConflicts(RpmPackage rpm, String value);
}
