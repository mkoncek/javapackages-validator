package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javapackages.validator.config.AllowedFiles;

public class AllowedFilesCheck extends Check<AllowedFiles> {
    @Override
    public Collection<String> check(String packageName, Path rpmPath, AllowedFiles config) throws IOException {
        var result = new ArrayList<String>(0);

        Path rpmFilePath = rpmPath.getFileName();

        if (rpmFilePath == null) {
            throw new IllegalArgumentException("Invalid RPM name: " + rpmPath.toString());
        }

        String rpmName = rpmFilePath.toString();

        try (var is = new RpmArchiveInputStream(rpmPath)) {
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
        System.exit(new AllowedFilesCheck().executeCheck(AllowedFiles.class, args));
    }
}
