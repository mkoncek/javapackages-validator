package org.fedoraproject.javapackages.validator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.fedoraproject.javapackages.validator.config.RpmFilesize;

public class RpmFilesizeCheck {
    static Collection<String> checkRpmFilesize(Path path, RpmFilesize config) throws Exception {
        var result = new ArrayList<String>(0);

        var filename = path.getFileName();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid file path: " + path);
        }

        long filesize = Files.size(path);
        String formattedFilesize = NumberFormat.getInstance(Locale.ENGLISH).format(filesize);

        if (!config.allowedFilesize(Common.getPackageName(path), filename.toString(), filesize)) {
            result.add(MessageFormat.format("[FAIL] {0}: file size is: {1} bytes", path, formattedFilesize));
        } else {
            System.err.println(MessageFormat.format("[INFO] {0}: file size is: {1} bytes", path, formattedFilesize));
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        int exitcode = 0;

        var configClass = Class.forName("org.fedoraproject.javapackages.validator.config.RpmFilesizeConfig");
        var config = (RpmFilesize) configClass.getConstructor().newInstance();

        for (int i = 0; i != args.length; ++i) {
            for (var message : checkRpmFilesize(Paths.get(args[i]).resolve(".").toAbsolutePath().normalize(), config)) {
                exitcode = 1;
                System.out.println(message);
            }
        }

        System.exit(exitcode);
    }
}
