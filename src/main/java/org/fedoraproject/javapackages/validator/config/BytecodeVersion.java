package org.fedoraproject.javapackages.validator.config;

public interface BytecodeVersion {
    public static final class VersionRange {
        public final int min;
        public final int max;

        public VersionRange(int min, int max) {
            if (min > max) {
                throw new IllegalArgumentException("Parameter `min` is larger than parameter `max`");
            }

            this.min = min;
            this.max = max;
        }

        public boolean contains(int value) {
            return min <= value && value <= max;
        }
    }

    VersionRange versionRangeOf(String packageName, String rpmName, String jarName, String className);
}
