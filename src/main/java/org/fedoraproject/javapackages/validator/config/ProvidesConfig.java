package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface ProvidesConfig {
    boolean allowedProvides(RpmInfo rpm, String value);
}
