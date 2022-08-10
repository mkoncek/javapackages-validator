package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface RecommendsConfig {
    boolean allowedRecommends(RpmInfo rpm, String value);
}
