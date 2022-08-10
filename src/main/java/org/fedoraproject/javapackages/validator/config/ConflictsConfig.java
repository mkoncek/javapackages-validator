package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface ConflictsConfig {
    boolean allowedConflicts(RpmInfo rpm, String value);
}
