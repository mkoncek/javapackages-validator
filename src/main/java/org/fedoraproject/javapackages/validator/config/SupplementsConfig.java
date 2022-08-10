package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface SupplementsConfig {
    boolean allowedSupplements(RpmInfo rpm, String value);
}
