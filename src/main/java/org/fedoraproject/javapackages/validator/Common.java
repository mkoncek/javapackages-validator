package org.fedoraproject.javapackages.validator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.kojan.javadeptools.rpm.RpmArchiveInputStream;
import io.kojan.javadeptools.rpm.RpmInfo;
import io.kojan.javadeptools.rpm.RpmPackage;

public class Common {
    public static final IOException INCOMPLETE_READ = new IOException("Incomplete read in RPM stream");

    @SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
    public static Path getEntryPath(CpioArchiveEntry entry) {
        return Paths.get("/").resolve(Paths.get("/").relativize(Paths.get("/").resolve(Paths.get(entry.getName()))));
    }

    public static String getPackageName(RpmInfo rpm) {
        if (rpm.isSourcePackage()) {
            return rpm.getName();
        } else {
            String result = rpm.getSourceRPM();
            result = result.substring(0, result.lastIndexOf('-'));
            result = result.substring(0, result.lastIndexOf('-'));

            if (result.isEmpty()) {
                throw new RuntimeException("Could not read package name for source RPM: " + rpm.getSourceRPM());
            }

            return result;
        }
    }

    /**
     * @param rpmPath The rpm file to inspect.
     * @return A map of file paths mapped to either the target of the symlink
     * or null, if the file path is not a symlink.
     * @throws IOException
     */
    public static SortedMap<CpioArchiveEntry, Path> rpmFilesAndSymlinks(RpmPackage rpm) throws IOException {
        var result = new TreeMap<CpioArchiveEntry, Path>((lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));

        try (var is = new RpmArchiveInputStream(rpm.getPath())) {
            for (CpioArchiveEntry rpmEntry; (rpmEntry = is.getNextEntry()) != null;) {
                Path target = null;

                if (rpmEntry.isSymbolicLink()) {
                    var content = new byte[(int) rpmEntry.getSize()];

                    if (is.read(content) != content.length) {
                        throw Common.INCOMPLETE_READ;
                    }

                    target = Paths.get(new String(content, StandardCharsets.UTF_8));
                }

                result.put(rpmEntry, target);
            }
        }

        return result;
    }

    public static LogEntry logException(Exception ex) {
        var stackTrace = new ByteArrayOutputStream();
        ex.printStackTrace(new PrintStream(stackTrace, false, StandardCharsets.UTF_8));
        return LogEntry.error("An exception occured:{0}{1}",
                Decorated.plain(System.lineSeparator()),
                Decorated.plain(new String(stackTrace.toByteArray(), StandardCharsets.UTF_8)));
    }
}
