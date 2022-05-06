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

    /**
     * Return the allowed version range for a given class.
     * @param packageName Package name.
     * @param rpmName The base file name of the RPM file that is being inspected.
     * @param jarName The full path to the .jar archive that is being inspected.
     * @param className The full relative path to the .class file inside the .jar archive
     * @return Allowed version range for @param className.
     */
    VersionRange versionRangeOf(String packageName, String rpmName, String jarName, String className);
}
