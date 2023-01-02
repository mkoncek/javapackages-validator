package org.fedoraproject.javapackages.validator.validators;

import static org.fedoraproject.javapackages.validator.TestCommon.assertFailOne;
import static org.fedoraproject.javapackages.validator.TestCommon.assertPass;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.fedoraproject.javapackages.validator.TestCommon;
import org.fedoraproject.javapackages.validator.validators.SymlinkValidator.SymlinkValidatorEnvroot;
import org.junit.jupiter.api.Test;

public class SymlinkValidatorTest {
    private static final Path DANGLING_SYMLINK_RPM = TestCommon.RPM_PATH_PREFIX.resolve(Paths.get("noarch/dangling-symlink-1-1.noarch.rpm"));
    private static final Path VALID_SYMLINK_RPM = TestCommon.RPM_PATH_PREFIX.resolve(Paths.get("noarch/valid-symlink-1-1.noarch.rpm"));

    @Test
    void testDanglingSymlink() throws IOException {
        var validator = new SymlinkValidatorEnvroot(Paths.get("/"));
        validator.validate(TestCommon.iteratorFrom(Stream.of(DANGLING_SYMLINK_RPM)));
        assertFailOne(validator);
    }

    @Test
    void testValidSymlink() throws IOException {
        var validator = new SymlinkValidatorEnvroot(Paths.get("/"));
        validator.validate(TestCommon.iteratorFrom(Stream.of(VALID_SYMLINK_RPM)));
        assertPass(validator);
    }
}
