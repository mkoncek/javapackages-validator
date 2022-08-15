package org.fedoraproject.javapackages.validator.config.attribute;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface ProvidesConfig {
    boolean allowedProvides(RpmInfo rpm, String value);
}
