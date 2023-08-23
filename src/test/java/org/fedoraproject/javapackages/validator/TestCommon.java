package org.fedoraproject.javapackages.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

public class TestCommon {
    public static final Path RPMBUILD_PATH_PREFIX = Paths.get("src/test/resources/rpmbuild");
    public static final Path RPM_PATH_PREFIX = RPMBUILD_PATH_PREFIX.resolve(Paths.get("RPMS"));
    public static final Path SRPM_PATH_PREFIX = RPMBUILD_PATH_PREFIX.resolve(Paths.get("SRPMS"));

    public static Iterator<RpmInfoURI> iteratorFrom(Stream<Path> paths) {
        return paths.map(path -> RpmInfoURI.create(path.toUri())).iterator();
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
