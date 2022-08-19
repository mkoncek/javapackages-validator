package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface BytecodeVersionConfig {
    /**
     * Return the allowed version range for a given class.
     * @param rpmPackage RpmPackage.
     * @param jarName The full path to the .jar archive that is being inspected.
     * @param className The full relative path to the .class file inside the .jar archive.
     * @param version The bytecode version of the class.
     * @return Whether or not given class is allowed to have the version.
     */
    boolean allowedVersion(RpmInfo rpm, String jarName, String className, int version);
}
