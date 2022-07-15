package org.fedoraproject.javapackages.validator.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

import org.fedoraproject.javapackages.validator.Common;

public interface SymlinkConfig {
    /**
     * @param target The link target. Is always absolute.
     * @return Where the target was found or null if it was not found.
     */
    String targetLocation(Path target);

    public static class Envroot implements SymlinkConfig {
        Path envroot;

        public Envroot(Path envroot) {
            this.envroot = envroot.normalize();
        }

        @Override
        public String targetLocation(Path target) {
            Path result = envroot.resolve(Paths.get("." + target)).toAbsolutePath().normalize();

            if (Files.exists(result)) {
                return result.toString();
            }

            return null;
        }
    }

    public static class RpmSet implements SymlinkConfig {
        Map<Path, String> files = new TreeMap<Path, String>();

        public RpmSet(Path topdir) {
            try {
                for (Path rmpPath : Files.find(topdir, Integer.MAX_VALUE, ((path, attributes) ->
                        attributes.isRegularFile() && path.toString().endsWith(".rpm"))).toArray(Path[]::new)) {
                    rmpPath = rmpPath.normalize();
                    for (var entry : Common.rpmFilesAndSymlinks(rmpPath).keySet()) {
                        files.put(Paths.get(entry.getName().substring(1)), rmpPath.toString());
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
