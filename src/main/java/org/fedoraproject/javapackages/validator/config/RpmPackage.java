package org.fedoraproject.javapackages.validator.config;

public interface RpmPackage extends Nevra {
    String getPackageName();
    boolean isSourceRpm();
}
