package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface ObsoletesConfig {
    boolean allowedObsoletes(RpmInfo rpm, String value);
}
