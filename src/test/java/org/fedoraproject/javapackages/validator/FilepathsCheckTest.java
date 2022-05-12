package org.fedoraproject.javapackages.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class FilepathsCheckTest {
    private static final Path DANGLING_SYMLINK_RPM = Paths.get(Common.RPM_PATH_PREFIX + "/dangling-symlink-1-1.noarch.rpm");
    private static final Path VALID_SYMLINK_RPM = Paths.get(Common.RPM_PATH_PREFIX + "/valid-symlink-1-1.noarch.rpm");
    private static final Path DUPLICATE_FILE1_RPM = Paths.get(Common.RPM_PATH_PREFIX + "/duplicate-file1-1-1.noarch.rpm");
    private static final Path DUPLICATE_FILE2_RPM = Paths.get(Common.RPM_PATH_PREFIX + "/duplicate-file2-1-1.noarch.rpm");

    @Test
    void testDanglingSymlink() throws IOException {
        var result = FilepathsCheck.checkSymlinks(null, null, Paths.get("/"), Arrays.asList(DANGLING_SYMLINK_RPM));
        assertEquals(1, result.size());
    }

    @Test
    void testValidSymlink() throws IOException {
        var result = FilepathsCheck.checkSymlinks(null, null, Paths.get("/"), Arrays.asList(VALID_SYMLINK_RPM));
        assertEquals(0, result.size());
    }

    @Test
    void testIllegalDuplicateFile() throws IOException {
        var result = FilepathsCheck.checkSymlinks(null, null, Paths.get("/"), Arrays.asList(DUPLICATE_FILE1_RPM, DUPLICATE_FILE2_RPM));
        assertEquals(1, result.size());
    }

    @Test
    void testAllowedDuplicateFile() throws IOException {
        var result = FilepathsCheck.checkSymlinks(null, (packageName, fileName, providerRpms) -> true, Paths.get("/"), Arrays.asList(DUPLICATE_FILE1_RPM, DUPLICATE_FILE2_RPM));
        assertEquals(0, result.size());
    }
}
