package org.fedoraproject.javapackages.validator.config.attribute;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface EnhancesConfig {
    boolean allowedEnhances(RpmInfo rpm, String value);
}
