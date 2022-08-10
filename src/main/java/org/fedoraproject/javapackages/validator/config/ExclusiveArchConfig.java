package org.fedoraproject.javapackages.validator.config;

import java.util.List;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface ExclusiveArchConfig {
    boolean allowedExclusiveArch(RpmInfo rpm, List<String> values);
}
