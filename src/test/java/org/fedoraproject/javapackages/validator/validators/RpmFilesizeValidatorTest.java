package org.fedoraproject.javapackages.validator.validators;

import static org.fedoraproject.javapackages.validator.TestCommon.assertFailOne;
import static org.fedoraproject.javapackages.validator.TestCommon.assertPass;

import java.nio.file.Paths;

import org.easymock.EasyMock;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.TestCommon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class RpmFilesizeValidatorTest {
    private RpmInfoURI rpmPathInfo;
    private RpmFilesizeValidator validator;

    @BeforeEach
    public void setUp() throws Exception {
        rpmPathInfo = new RpmInfoURI(
                TestCommon.RPM_PATH_PREFIX.resolve(Paths.get("noarch/duplicate-file1-1-1.noarch.rpm")).toUri());
        validator = EasyMock.createStrictMock(RpmFilesizeValidator.class);
    }

    private void runTest() throws Exception {
        EasyMock.replay(validator);
        validator.validate(rpmPathInfo);
        EasyMock.verify(validator);
    }

    @Test
    @Disabled
    public void testAllowedFileSize() throws Exception {
        EasyMock.expect(validator.allowedFilesize(rpmPathInfo, 6488L)).andReturn(true);
        runTest();
        assertPass(validator);
    }

    @Test
    @Disabled
    public void testDisallowedFileSize() throws Exception {
        EasyMock.expect(validator.allowedFilesize(rpmPathInfo, 6488L)).andReturn(false);
        runTest();
        assertFailOne(validator);
    }
}
