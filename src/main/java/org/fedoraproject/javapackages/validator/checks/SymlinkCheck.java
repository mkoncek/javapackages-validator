package org.fedoraproject.javapackages.validator.checks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.ElementwiseCheck;
import org.fedoraproject.javapackages.validator.RpmFiles;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class SymlinkCheck extends ElementwiseCheck<Void> {
    private final Path envroot;

    public SymlinkCheck(Path envroot) {
        super();
        this.envroot = envroot;
    }

    @Override
    protected Collection<String> check(Path rpmPath, RpmInfo rpmInfo, Void config) throws IOException {
        var result = new ArrayList<String>(0);

        for (var entry : RpmFiles.filesAndSymlinks(rpmPath).entrySet()) {
            Path linkPath = Paths.get(entry.getKey().getName().substring(1));
            Path target = entry.getValue();

            if (target != null) {
                Path parent = linkPath.getParent();

                // Silence Spotbugs
                if (parent == null) {
                    throw new IllegalStateException("Path::getParent of " + entry.getKey().getName() + " returned null");
                }

                // Resolve relative links of RPM
                target = parent.resolve(target).normalize();

                // Resolve absolute paths of RPM against envroot
                target = envroot.resolve(Paths.get("." + target)).toAbsolutePath().normalize();

                if (!Files.exists(target)) {
                    result.add(MessageFormat.format("[FAIL] {0}: Link {1} points to {2} which is not present on the filesystem",
                            rpmPath, linkPath, target));
                } else {
                    System.err.println(MessageFormat.format("[INFO] {0}: Link {1} points to file {2} which is present on the filesystem",
                            rpmPath, linkPath, target));
                }
            }
        }

        return result;
    }

    @SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
    public static void main(String[] args) throws Exception {
        Path envroot = Paths.get("/");

        var argList = new ArrayList<String>();

        for (int i = 0; i != args.length; ++i) {
            if (args[i].equals("--envroot")) {
                ++i;
                envroot = Paths.get(args[i]).resolve(".").toAbsolutePath().normalize();
            } else {
                argList.add(args[i]);
            }
        }

        System.exit(new SymlinkCheck(envroot).executeCheck(Void.class, argList.toArray(String[]::new)));
    }
}
