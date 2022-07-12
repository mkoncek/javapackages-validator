package org.fedoraproject.javapackages.validator.config;

public interface RpmPackage {
    String getPackageName();
    Nevra getNevra();
    boolean isSourceRpm();
}
