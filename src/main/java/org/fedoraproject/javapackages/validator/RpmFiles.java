package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;

public class RpmFiles {
    /**
     * @param rpmPath The rpm file to inspect.
     * @return A map of file paths mapped to either the target of the symlink
     * or null, if the file path is not a symlink.
     * @throws IOException
     */
    public static SortedMap<CpioArchiveEntry, Path> filesAndSymlinks(Path rpmPath) throws IOException {
        var result = new TreeMap<CpioArchiveEntry, Path>((lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));

        try (var is = new RpmArchiveInputStream(rpmPath)) {
            for (CpioArchiveEntry rpmEntry; (rpmEntry = is.getNextEntry()) != null;) {
                if (rpmEntry.isDirectory()) {
                    continue;
                }

                var content = new byte[(int) rpmEntry.getSize()];

                if (is.read(content) != content.length) {
                    throw Common.INCOMPLETE_READ;
                }

                Path target = null;

                if (rpmEntry.isSymbolicLink()) {
                    target = Paths.get(new String(content, StandardCharsets.UTF_8));
                }

                result.put(rpmEntry, target);
            }
        }

        return result;
    }
}
