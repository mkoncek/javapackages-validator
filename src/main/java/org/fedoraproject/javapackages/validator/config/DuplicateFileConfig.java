package org.fedoraproject.javapackages.validator.config;

import java.nio.file.Path;
import java.util.Collection;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface DuplicateFileConfig {
    /**
     * @param filename The absolute path of the file.
     * @param providerRpms Non-source RPM packages which provide the file.
     * Always contains more than one entry.
     * @return Whether or not the multiple provider rpms are allowed to provide
     * the file.
     */
    boolean allowedDuplicateFile(Path path, Collection<? extends RpmInfo> providerRpms);
}
