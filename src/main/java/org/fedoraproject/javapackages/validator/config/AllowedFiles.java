package org.fedoraproject.javapackages.validator.config;

public interface AllowedFiles {
    boolean allowedFile(String packageName, String rpmName, String rpmEntryPath);
}
