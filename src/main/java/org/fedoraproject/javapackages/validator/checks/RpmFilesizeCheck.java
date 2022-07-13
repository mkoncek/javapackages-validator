package org.fedoraproject.javapackages.validator.checks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.ElementwiseCheck;
import org.fedoraproject.javapackages.validator.RpmPackageImpl;
import org.fedoraproject.javapackages.validator.config.RpmFilesizeConfig;

public class RpmFilesizeCheck extends ElementwiseCheck<RpmFilesizeConfig> {
    @Override
    public Collection<String> check(Path rpmPath, RpmInfo rpmInfo, RpmFilesizeConfig config) throws IOException {
        var result = new ArrayList<String>(0);

        long filesize = Files.size(rpmPath);
        String formattedFilesize = NumberFormat.getInstance(Locale.ENGLISH).format(filesize);

        if (!config.allowedFilesize(new RpmPackageImpl(rpmInfo), filesize)) {
            result.add(MessageFormat.format("[FAIL] {0}: file size is: {1} bytes", rpmPath, formattedFilesize));
        } else {
            System.err.println(MessageFormat.format("[INFO] {0}: file size is: {1} bytes", rpmPath, formattedFilesize));
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        System.exit(new RpmFilesizeCheck().executeCheck(RpmFilesizeConfig.class, args));
    }
}
