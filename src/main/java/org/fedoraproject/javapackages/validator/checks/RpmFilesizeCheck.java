package org.fedoraproject.javapackages.validator.checks;

import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.fedoraproject.javapackages.validator.ElementwiseCheck;
import org.fedoraproject.javapackages.validator.RpmPathInfo;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;
import org.fedoraproject.javapackages.validator.config.RpmFilesizeConfig;

public class RpmFilesizeCheck extends ElementwiseCheck<RpmFilesizeConfig> {
    public RpmFilesizeCheck() {
        super(RpmFilesizeConfig.class);
    }

    @Override
    public Collection<String> check(RpmFilesizeConfig config, RpmPathInfo rpm) throws IOException {
        var result = new ArrayList<String>(0);

        long filesize = Files.size(rpm.getPath());
        String formattedFilesize = NumberFormat.getInstance(Locale.ENGLISH).format(filesize);

        if (!config.allowedFilesize(rpm, filesize)) {
            result.add(failMessage("{0}: file size is: {1} bytes",
                    textDecorate(rpm.getPath(), Decoration.bright_red),
                    textDecorate(formattedFilesize, Decoration.bright_cyan)));
        } else {
            getLogger().pass("{0}: file size is: {1} bytes",
                    textDecorate(rpm.getPath(), Decoration.bright_red),
                    textDecorate(formattedFilesize, Decoration.bright_cyan));
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        System.exit(new RpmFilesizeCheck().executeCheck(args));
    }
}
