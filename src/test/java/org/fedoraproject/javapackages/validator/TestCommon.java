package org.fedoraproject.javapackages.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.fedoraproject.javadeptools.rpm.RpmFile;

public class TestCommon {
    public static final Path RPMBUILD_PATH_PREFIX = Paths.get("src/test/resources/rpmbuild");
    public static final Path RPM_PATH_PREFIX = RPMBUILD_PATH_PREFIX.resolve(Paths.get("RPMS"));
    public static final Path SRPM_PATH_PREFIX = RPMBUILD_PATH_PREFIX.resolve(Paths.get("SRPMS"));

    public static Iterable<RpmFile> fromPaths(Path... paths) {
        var result = new ArrayList<RpmFile>(paths.length);
        for (var path : paths) {
            try {
                result.add(RpmFile.from(path));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return result;
    }

    public static void assertInfo(Validator validator) {
        assertEquals(TestResult.info, validator.getResult(), "expected result: INFO");
    }

    public static void assertPass(Validator validator) {
        assertEquals(TestResult.pass, validator.getResult(), "expected result: PASS");
    }

    public static void assertFailOne(Validator validator) {
        assertEquals(TestResult.fail, validator.getResult(), "expected result: FAIL");
        assertEquals(1, validator.getMessages().stream().filter(p -> LogEvent.fail.equals(p.kind())).count());
    }
}
