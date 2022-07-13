package org.fedoraproject.javapackages.validator.config;

import java.util.Collection;

public interface DuplicateFileConfig {
    /**
     * @param filename The absolute path of the file.
     * @param providerRpms Non-source RPM packages which provide the file.
     * Will always contain more than one entry.
     * @return Error messages.
     */
    boolean allowedDuplicateFile(String filename, Collection<? extends RpmPackage> providerRpms);
}
