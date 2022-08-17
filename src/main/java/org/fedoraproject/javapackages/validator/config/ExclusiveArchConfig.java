package org.fedoraproject.javapackages.validator.config;

import java.util.List;

import org.fedoraproject.javadeptools.rpm.RpmInfo;

public interface ExclusiveArchConfig {
    /**
     * @param rpm The rpm package.
     * @param values The values of exclusive arch field.
     * @return Whether the rpm package has passed with given values of exclusive arch.
     */
    boolean allowedExclusiveArch(RpmInfo rpm, List<String> values);

    public static class Default implements ExclusiveArchConfig {
        @Override
        public boolean allowedExclusiveArch(RpmInfo rpm, List<String> values) {
            boolean buildNoarch = rpm.getBuildArchs().contains("noarch");
            boolean exclusiveNoarch = values.contains("noarch");
            return buildNoarch == exclusiveNoarch;
        }
    }
}
