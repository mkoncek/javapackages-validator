package org.fedoraproject.javapackages.validator.config;

import java.nio.file.Path;

public interface FilesConfig {
    /**
     * @param rpmPackage RpmPackage.
     * @param filename The absolute path of a file present in the RPM.
     * @return Whether or not the file is allowed in the RPM.
     */
    boolean allowedFile(RpmPackage rpm, Path path);
}
