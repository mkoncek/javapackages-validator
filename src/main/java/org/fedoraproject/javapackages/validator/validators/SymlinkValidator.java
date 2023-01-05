package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
    private Path envroot = Paths.get("/");

    @Override
    public void arguments(String[] args) {
        if (args.length > 0 && args[0].equals("-e")) {
            envroot = Paths.get(args[1]);
        }
    }

    public SymlinkValidator() {
        super(RpmInfo::isBinaryPackage);
    }

    @Override
    public void validate(RpmInfoURI rpm) throws IOException {
        for (var entry : Common.rpmFilesAndSymlinks(rpm.getURI()).entrySet()) {
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
