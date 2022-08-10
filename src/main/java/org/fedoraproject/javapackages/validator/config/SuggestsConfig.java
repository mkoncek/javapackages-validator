package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface SuggestsConfig {
    boolean allowedSuggests(RpmInfo rpm, String value);
}
