package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface EnhancesConfig {
    boolean allowedEnhances(RpmInfo rpm, String value);
}
