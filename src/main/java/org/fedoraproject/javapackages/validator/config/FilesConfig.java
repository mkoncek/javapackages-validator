package org.fedoraproject.javapackages.validator.config;

import java.nio.file.Path;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface FilesConfig {
    boolean allowedFile(RpmInfo rpm, Path path);
}
