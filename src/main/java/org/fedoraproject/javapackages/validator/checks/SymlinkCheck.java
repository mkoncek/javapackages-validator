package org.fedoraproject.javapackages.validator.checks;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Common;
import org.fedoraproject.javapackages.validator.ElementwiseCheck;
import org.fedoraproject.javapackages.validator.config.SymlinkConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Ignores source rpms.
 */
public class SymlinkCheck extends ElementwiseCheck<SymlinkConfig> {
    public SymlinkCheck() {
        super();
    }

    public SymlinkCheck(SymlinkConfig config) {
        super(config);
    }

    @Override
    protected Collection<String> check(Path rpmPath, RpmInfo rpmInfo) throws IOException {
        if (rpmInfo.isSourcePackage()) {
            return Collections.emptyList();
        }

        var result = new ArrayList<String>(0);

        for (var entry : Common.rpmFilesAndSymlinks(rpmPath).entrySet()) {
            Path link = Paths.get(entry.getKey().getName().substring(1));
            Path target = entry.getValue();

            if (target != null) {
                Path parent = link.getParent();

                // Silence Spotbugs
                if (parent == null) {
                    throw new IllegalStateException("Path::getParent of " + entry.getKey().getName() + " returned null");
                }

                // Resolve relative links of RPM
                target = parent.resolve(target).normalize();

                // Resolve absolute paths of RPM against envroot
                // target = getConfig().getEnvroot().resolve(Paths.get("." + target)).toAbsolutePath().normalize();

                String location = getConfig().targetLocation(target);

                if (location == null) {
                    result.add(MessageFormat.format("[FAIL] {0}: Link {1} points to {2} which was not found",
                            rpmPath, link, target));
                } else {
                    System.err.println(MessageFormat.format("[INFO] {0}: Link {1} points to file {2} located in " + location,
                            rpmPath, link, target));
                }
            }
        }

        return result;
    }

    @SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
    public static void main(String[] args) throws Exception {
        System.exit(new SymlinkCheck().executeCheck(SymlinkConfig.class, args));
    }
}
