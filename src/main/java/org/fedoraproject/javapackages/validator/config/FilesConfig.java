package org.fedoraproject.javapackages.validator.config;

public interface FilesConfig {
    /**
     * @param rpmPackage RpmPackage.
     * @param filename The absolute path of a file present in the RPM.
     * @return Whether or not the file is allowed in the RPM.
     */
    boolean allowedFile(RpmPackage rpm, String filename);
}
