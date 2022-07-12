package org.fedoraproject.javapackages.validator.config;

public interface RpmFilesizeConfig {
    boolean allowedFilesize(RpmPackage rpmPackage, long sizeBytes);
}
