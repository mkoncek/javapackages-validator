package org.fedoraproject.javapackages.validator.config;

public interface Nevra {
    String getName();
    int getEpoch();
    String getVersion();
    String getRelease();
    String getArch();
}
