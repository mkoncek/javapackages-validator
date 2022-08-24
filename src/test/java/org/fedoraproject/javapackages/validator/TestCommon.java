package org.fedoraproject.javapackages.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

import org.fedoraproject.javapackages.validator.validators.Validator;

public class TestCommon {
    public static final Path RPMBUILD_PATH_PREFIX = Paths.get("src/test/resources/rpmbuild");
    public static final Path RPM_PATH_PREFIX = RPMBUILD_PATH_PREFIX.resolve(Paths.get("RPMS"));
    public static final Path SRPM_PATH_PREFIX = RPMBUILD_PATH_PREFIX.resolve(Paths.get("SRPMS"));

    public static Iterator<RpmPathInfo> iteratorFrom(Stream<Path> paths) {
        return paths.map(path -> {
            try {
                return new RpmPathInfo(path);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }).iterator();
    }

    public static void assertPass(Validator validator) {
        assertFalse(validator.failed(), "expected passed result, but it actually failed");
    }

    public static void assertFailOne(Validator validator) {
        assertTrue(validator.failed(), "expected failed result, but it actually passed");
        assertEquals(1, validator.getFailMessages().size());
    }
}
