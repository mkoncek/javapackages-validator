package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface RequiresConfig {
    boolean allowedRequires(RpmInfo rpm, String value);
}
