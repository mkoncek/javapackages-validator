package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javapackages.validator.config.AllowedFiles;

public class FilesCheck {
    static Collection<String> checkFiles(Path path, String packageName, AllowedFiles config) throws IOException {
        var result = new ArrayList<String>(0);

        Path rpmFilePath = path.getFileName();

        if (rpmFilePath == null) {
            throw new IllegalArgumentException("Invalid RPM name: " + path.toString());
        }

        String rpmName = rpmFilePath.toString();

        try (var is = new RpmArchiveInputStream(path)) {
            boolean ok = true;
            for (CpioArchiveEntry rpmEntry; ((rpmEntry = is.getNextEntry()) != null);) {
                var entryName = rpmEntry.getName().substring(1);
                if (!config.allowedFile(packageName, rpmName, entryName)) {
                    ok = false;
                    result.add(MessageFormat.format("[FAIL] {0}: Illegal file: {1}",
                            rpmName, entryName));
                }
            }

            if (ok) {
                System.err.println(MessageFormat.format("[INFO] {0}: ok", rpmName));
            }
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        int exitcode = 0;

        var configClass = Class.forName("org.fedoraproject.javapackages.validator.config.AllowedFilesConfig");
        var config = (AllowedFiles) configClass.getConstructor().newInstance();

        for (int i = 1; i != args.length; ++i) {
            for (var message : checkFiles(Paths.get(args[i]).resolve(".").toAbsolutePath().normalize(), args[0], config)) {
                exitcode = 1;
                System.out.println(message);
            }
        }

        System.exit(exitcode);
    }
}