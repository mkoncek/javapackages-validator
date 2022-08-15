package org.fedoraproject.javapackages.validator.config.attribute;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface SuggestsConfig {
    boolean allowedSuggests(RpmInfo rpm, String value);
}
