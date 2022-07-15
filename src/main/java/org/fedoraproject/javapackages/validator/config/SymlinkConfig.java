package org.fedoraproject.javapackages.validator.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

import org.fedoraproject.javapackages.validator.Common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public interface SymlinkConfig {
    /**
     * @param target The link target. Is always absolute.
     * @return Where the target was found or null if it was not found.
     */
    String targetLocation(Path target);

    public static class Envroot implements SymlinkConfig {
        private Path envroot;

        public Envroot(Path envroot) {
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

    public static class RpmSet implements SymlinkConfig {
        private Map<Path, String> files = new TreeMap<Path, String>();

        public RpmSet(Path topdir) {
            try {
                for (Path rmpPath : Files.find(topdir, Integer.MAX_VALUE, ((path, attributes) ->
                        attributes.isRegularFile() && path.toString().endsWith(".rpm"))).toArray(Path[]::new)) {
                    for (var entry : Common.rpmFilesAndSymlinks(rmpPath).keySet()) {
                        files.put(Common.getEntryPath(entry), rmpPath.toString());
                    }
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        @Override
        public String targetLocation(Path target) {
            return files.get(target);
        }
    }
}
