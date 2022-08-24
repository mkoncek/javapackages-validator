package org.fedoraproject.javapackages.validator.validators;

import static org.fedoraproject.javapackages.validator.TestCommon.assertFailOne;
import static org.fedoraproject.javapackages.validator.TestCommon.assertPass;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Stream;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.TestCommon;
import org.fedoraproject.javapackages.validator.validators.DuplicateFileValidator.DuplicateFileValidatorDefault;
import org.junit.jupiter.api.Test;

public class DuplicateFileValidatorTest {
    private static final Path DUPLICATE_FILE1_RPM = TestCommon.RPM_PATH_PREFIX.resolve(Paths.get("noarch/duplicate-file1-1-1.noarch.rpm"));
    private static final Path DUPLICATE_FILE2_RPM = TestCommon.RPM_PATH_PREFIX.resolve(Paths.get("noarch/duplicate-file2-1-1.noarch.rpm"));

    @Test
    void testIllegalDuplicateFile() throws IOException {
        var validator = new DuplicateFileValidatorDefault() {
            @Override
            public boolean allowedDuplicateFile(Path path, Collection<? extends RpmInfo> providerRpms) throws IOException {
                return false;
            }
        };
        validator.validate(TestCommon.iteratorFrom(Stream.of(
                DUPLICATE_FILE1_RPM, DUPLICATE_FILE2_RPM)));
        assertFailOne(validator);
    }

    @Test
    void testAllowedDuplicateFile() throws IOException {
        var validator = new DuplicateFileValidatorDefault() {
            @Override
            public boolean allowedDuplicateFile(Path path, Collection<? extends RpmInfo> providerRpms) throws IOException {
                return true;
            }
        };
        validator.validate(TestCommon.iteratorFrom(Stream.of(DUPLICATE_FILE1_RPM, DUPLICATE_FILE2_RPM)));
        assertPass(validator);
    }
}
