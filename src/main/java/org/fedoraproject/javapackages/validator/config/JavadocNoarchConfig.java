package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface JavadocNoarchConfig {
    /**
     * @param rpm The rpm package.
     * @return Whether or not the rpm package is a javadoc package.
     */
    boolean isJavadocRpm(RpmInfo rpm);

    public static class Default implements JavadocNoarchConfig {
        @Override
        public boolean isJavadocRpm(RpmInfo rpm) {
            return rpm.getName().equals(rpm.getPackageName() + "-javadoc");
        }
    }
}
