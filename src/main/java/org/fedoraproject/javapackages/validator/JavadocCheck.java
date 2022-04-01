package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;

public class JavadocCheck {
    static Collection<String> checkJavadocRpm(String packageName, Path path) throws IOException {
        final var javadocPattern = Pattern.compile("/usr/share/javadoc/" + packageName + "(:?/.*)?");

        var result = new ArrayList<String>(0);

        try (var is = new RpmArchiveInputStream(path)) {
            for (CpioArchiveEntry rpmEntry; (rpmEntry = is.getNextEntry()) != null;) {
                var rpmEntryName = rpmEntry.getName().substring(1);
                if (!javadocPattern.matcher(rpmEntryName).matches() && !rpmEntryName.startsWith("/usr/share/licenses")) {
                    result.add(MessageFormat.format("[FAIL] {0}: File {1} should not be present in a javadoc RPM",
                            path, rpmEntryName));
                }
            }
        }

        return result;
    }

    static Collection<String> checkNonJavadocRpm(String packageName, Path path) throws IOException {
        var result = new ArrayList<String>(0);

        try (var is = new RpmArchiveInputStream(path)) {
            for (CpioArchiveEntry rpmEntry; (rpmEntry = is.getNextEntry()) != null;) {
                var rpmEntryName = rpmEntry.getName().substring(1);
                if (rpmEntryName.startsWith("/usr/share/javadoc")) {
                    result.add(MessageFormat.format("[FAIL] {0}: File {1} should not be present in a non-javadoc RPM",
                            path, rpmEntryName));
                }
            }
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        int exitcode = 0;

        String packageName = args[0];
        String javadocRpm = null;
        var otherRpms = new ArrayList<String>();

        for (int i = 1; i != args.length; ++i) {
            var filepath = Paths.get(args[i]).getFileName();
            if (filepath == null) {
                throw new IllegalArgumentException("Invalid file path: " + args[i]);
            }

            if (filepath.toString().startsWith(packageName + "-javadoc")) {
                javadocRpm = args[i];
            } else {
                otherRpms.add(args[i]);
            }
        }

        var messages = new ArrayList<String>();

        if (javadocRpm != null) {
            var result = checkJavadocRpm(packageName, Paths.get(javadocRpm));
            if (result.isEmpty()) {
                System.err.println("[INFO] (javadoc RPM) " + javadocRpm + " - ok");
            }
            messages.addAll(result);
        }

        for (String rpmName : otherRpms) {
            var result = checkNonJavadocRpm(packageName, Paths.get(rpmName));;
            if (result.isEmpty()) {
                System.err.println("[INFO] (non-javadoc RPM) " + rpmName + " - ok");
            }
            messages.addAll(result);
        }

        for (var message : messages) {
            exitcode = 1;
            System.out.println(message);
        }

        System.exit(exitcode);
    }
}
