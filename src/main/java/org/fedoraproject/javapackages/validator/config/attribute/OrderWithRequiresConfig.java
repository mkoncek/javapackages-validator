package org.fedoraproject.javapackages.validator.config.attribute;

import org.fedoraproject.javadeptools.rpm.RpmAttribute;
import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface OrderWithRequiresConfig {
    boolean allowedOrderWithRequires(RpmInfo rpm, RpmAttribute value);
}
