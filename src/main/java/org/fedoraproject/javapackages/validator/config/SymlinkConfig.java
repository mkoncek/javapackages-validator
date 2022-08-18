package org.fedoraproject.javapackages.validator.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.fedoraproject.javapackages.validator.Common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public interface SymlinkConfig {
    /**
     * @param target The link target. Is always absolute.
     * @return The textual representation of the location where the link target
     * was found. Can be a file on the file system or an rpm file. Returning
     * {@code null} means the target was not found.
     */
    String targetLocation(Path target);

    /**
     * Default implementation of {@link org.fedoraproject.javapackages.validator.config.SymlinkConfig}
     * which resolves symbolic link targets against files present on the file
     * system with specified environment root.
     */
    public static class EnvrootImpl implements SymlinkConfig {
        private Path envroot;

        public EnvrootImpl(Path envroot) {
            this.envroot = envroot;
        }

        @Override
        @SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
        public String targetLocation(Path target) {
            Path result = envroot.resolve(Paths.get("/").relativize(target));

            if (Files.exists(result)) {
                return result.toString();
            }

            return null;
        }
    }

    /**
     * Default implementation of {@link org.fedoraproject.javapackages.validator.config.SymlinkConfig}
     * which resolves symbolic link targets against files present inside rpm
     * files.
     */
    public static class RpmSetImpl implements SymlinkConfig {
        private Map<Path, List<String>> files = new TreeMap<>();

        public RpmSetImpl(Iterator<Path> rpms) {
            try {
                while (rpms.hasNext()) {
                    Path rpmPath = rpms.next();
                    for (var entry : Common.rpmFilesAndSymlinks(rpmPath).keySet()) {
                        files.computeIfAbsent(Common.getEntryPath(entry),
                                (path) -> new ArrayList<>()).add(rpmPath.toString());
                    }
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        @Override
        public String targetLocation(Path target) {
            var result = files.get(target.normalize());

            if (result != null) {
                return result.toString();
            }

            return null;
        }
    }
}
