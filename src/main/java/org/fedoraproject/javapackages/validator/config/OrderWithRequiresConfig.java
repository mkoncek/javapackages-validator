package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface OrderWithRequiresConfig {
    boolean allowedOrderWithRequires(RpmInfo rpm, String value);
}
