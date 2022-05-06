package org.fedoraproject.javapackages.validator.config;

public interface RpmFilesize {
    boolean allowedFilesize(String packageName, String rpmName, long sizeBytes);
}
