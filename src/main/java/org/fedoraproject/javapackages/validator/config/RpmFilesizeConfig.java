package org.fedoraproject.javapackages.validator.config;

public interface RpmFilesizeConfig {
    boolean allowedFilesize(RpmPackage rpm, long sizeBytes);
}
