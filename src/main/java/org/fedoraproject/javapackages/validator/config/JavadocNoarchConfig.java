package org.fedoraproject.javapackages.validator.config;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Common;

public interface JavadocNoarchConfig {
    /**
     * @param rpm The rpm package.
     * @return Whether or not the rpm package is a javadoc package.
     */
    boolean isJavadocRpm(RpmInfo rpm);

    public static class Default implements JavadocNoarchConfig {
        @Override
        public boolean isJavadocRpm(RpmInfo rpm) {
            return rpm.getName().equals(Common.getPackageName(rpm) + "-javadoc");
        }
    }
}
