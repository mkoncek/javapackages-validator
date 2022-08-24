package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmPathInfo;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Ignores source rpms.
 */
public abstract class SymlinkValidator extends ElementwiseValidator {
    /**
     * @param target The link target. Is always absolute.
     * @return The textual representation of the location where the link target
     * was found. Can be a file on the file system or an rpm file. Returning
     * {@code null} means the target was not found.
     */
    public abstract String targetLocation(Path target);

    /**
     * Default implementation of SymlinkValidator which resolves symbolic link
     * targets against files present on the file system with specified environment
     * root.
     */
    public static class SymlinValidatorEnvroot extends SymlinkValidator {
        private Path envroot;

        public SymlinValidatorEnvroot(Path envroot) {
            this.envroot = envroot;
        }

        @Override
        @SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
        public String targetLocation(Path target) {
            Path result = envroot.resolve(Paths.get("/").relativize(target));

            if (Files.exists(result, LinkOption.NOFOLLOW_LINKS)) {
                return result.toString();
            }

            return null;
        }
    }

    public SymlinkValidator() {
        super(Predicate.not(RpmInfo::isSourcePackage));
    }

    @Override
    public void validate(RpmPathInfo rpm) throws IOException {
        for (var entry : Common.rpmFilesAndSymlinks(rpm.getPath()).entrySet()) {
            Path link = Common.getEntryPath(entry.getKey());
            Path target = entry.getValue();

            if (target != null) {
                // Resolve relative links of RPM entries
                target = link.resolveSibling(target);

                String location = targetLocation(target);

                if (location == null) {
                    fail("{0}: Link {1} points to {2} (normalized as {3}) which was not found",
                            Decorated.rpm(rpm),
                            Decorated.custom(link, Decoration.bright_cyan),
                            Decorated.custom(target, Decoration.bright_magenta),
                            Decorated.custom(target.normalize(), Decoration.magenta));
                } else {
                    pass("{0}: Link {1} points to {2} (normalized as {3}) located in {4}",
                            Decorated.rpm(rpm),
                            Decorated.custom(link, Decoration.bright_cyan),
                            Decorated.custom(target, Decoration.bright_magenta),
                            Decorated.custom(target.normalize(), Decoration.magenta),
                            Decorated.outer(location));
                }
            }
        }
    }
}
