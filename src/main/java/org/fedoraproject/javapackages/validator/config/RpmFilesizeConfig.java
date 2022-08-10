package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface RpmFilesizeConfig {
    boolean allowedFilesize(RpmInfo rpm, long sizeBytes);
}
