package org.fedoraproject.javapackages.validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.fedoraproject.javapackages.validator.config.RpmFilesize;
import org.fedoraproject.javapackages.validator.util.Common;

public class RpmFilesizeCheck extends Check<RpmFilesize> {
    @Override
    public Collection<String> check(String packageName, Path rpmPath, RpmFilesize config) throws IOException {
        var result = new ArrayList<String>(0);

        var filename = rpmPath.getFileName();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid file path: " + rpmPath);
        }

        long filesize = Files.size(rpmPath);
        String formattedFilesize = NumberFormat.getInstance(Locale.ENGLISH).format(filesize);

        if (!config.allowedFilesize(Common.getPackageName(rpmPath), filename.toString(), filesize)) {
            result.add(MessageFormat.format("[FAIL] {0}: file size is: {1} bytes", rpmPath, formattedFilesize));
        } else {
            System.err.println(MessageFormat.format("[INFO] {0}: file size is: {1} bytes", rpmPath, formattedFilesize));
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        System.exit(new RpmFilesizeCheck().executeCheck(RpmFilesize.class, args));
    }
}
