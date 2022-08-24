package org.fedoraproject.javapackages.validator.validators.jp;

import java.nio.file.Paths;

import org.fedoraproject.javapackages.validator.validators.SymlinkValidator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
public class SymlinkValidatorJP extends SymlinkValidator.SymlinValidatorEnvroot {
    public SymlinkValidatorJP() {
        super(Paths.get("/mnt/envroot"));
    }
}
