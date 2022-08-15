package org.fedoraproject.javapackages.validator.config.attribute;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface ConflictsConfig {
    boolean allowedConflicts(RpmInfo rpm, String value);
}
