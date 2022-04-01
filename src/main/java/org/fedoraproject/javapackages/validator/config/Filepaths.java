package org.fedoraproject.javapackages.validator.config;

import java.util.Collection;

public interface Filepaths {
    boolean allowedDuplicateFile(String packageName, String fileName, Collection<String> providerRpms);
}
