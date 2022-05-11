package org.fedoraproject.javapackages.validator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.fedoraproject.javapackages.validator.config.RpmFilesize;

public class RpmFilesizeCheck {
    public static void main(String[] args) throws Exception {
        int exitcode = 0;

        var configClass = Class.forName("org.fedoraproject.javapackages.validator.config.RpmFilesizeConfig");
        var config = (RpmFilesize) configClass.getConstructor().newInstance();

        var messages = new ArrayList<String>();
        var numberFormat = NumberFormat.getInstance(Locale.ENGLISH);

        for (int i = 0; i != args.length; ++i) {
            var filepath = Paths.get(args[i]);
            var filename = filepath.getFileName();
            if (filename == null) {
                throw new IllegalArgumentException("Invalid file path: " + args[i]);
            }

            long filesize = Files.size(filepath);
            String formattedFilesize = numberFormat.format(filesize);

            if (!config.allowedFilesize(Common.getPackageName(filepath), filename.toString(), filesize)) {
                messages.add(MessageFormat.format("[FAIL] {0}: file size is: {1} bytes", filepath, formattedFilesize));
            } else {
                System.err.println(MessageFormat.format("[INFO] {0}: file size is: {1} bytes", filepath, formattedFilesize));
            }
        }

        for (var message : messages) {
            exitcode = 1;
            System.out.println(message);
        }

        System.exit(exitcode);
    }
}
