package org.fedoraproject.javapackages.validator.validators;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;
import org.fedoraproject.javapackages.validator.helpers.ElementwiseValidator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.kojan.javadeptools.rpm.RpmInfo;
import io.kojan.javadeptools.rpm.RpmPackage;

/**
 * Validator which resolves symbolic link targets against files present
 * on the file system with specified environment root.
 *
 * Optional arguments:
 *     -e <envroot> -- Environment root (default is /)
 *
 * Ignores source rpms.
 */
@SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
public class SymlinkValidator extends ElementwiseValidator {
    @Override
    public String getTestName() {
        return "/symlinks";
    }

    public SymlinkValidator() {
        super(Predicate.not(RpmInfo::isSourcePackage));
    }

    @Override
    public void validate(RpmPackage rpm) throws Exception {
        var envroot = Paths.get("/");
        var args = getArgs();
        if (args != null && args.size() == 2 && args.get(0).equals("-e")) {
            envroot = Paths.get(args.get(1));
        }

        for (var entry : Common.rpmFilesAndSymlinks(rpm).entrySet()) {
            Path link = Common.getEntryPath(entry.getKey());
            Path target = entry.getValue();

            if (target != null) {
                // Resolve relative links of RPM entries
                target = link.resolveSibling(target);
                target = envroot.resolve(Paths.get("/").relativize(target));

                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                    pass("{0}: Link {1} points to {2} (normalized as {3}) exists as {4}",
                            Decorated.rpm(rpm),
                            Decorated.custom(link, Decoration.bright_cyan),
                            Decorated.custom(target, Decoration.bright_magenta),
                            Decorated.custom(target.normalize(), Decoration.magenta),
                            Decorated.outer(target));
                } else {
                    fail("{0}: Link {1} points to {2} (normalized as {3}) does not exist as {4}",
                            Decorated.rpm(rpm),
                            Decorated.custom(link, Decoration.bright_cyan),
                            Decorated.custom(target, Decoration.bright_magenta),
                            Decorated.custom(target.normalize(), Decoration.magenta),
                            Decorated.outer(target));
                }
            }
        }
    }
}
