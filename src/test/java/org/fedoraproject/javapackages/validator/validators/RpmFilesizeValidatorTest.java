package org.fedoraproject.javapackages.validator.validators;

public class RpmFilesizeValidatorTest {
    /*
    private RpmPathInfo rpmPathInfo;
    private RpmFilesizeValidator validator;

    @BeforeEach
    public void setUp() throws Exception {
        rpmPathInfo = new RpmPathInfo(
                TestCommon.RPM_PATH_PREFIX.resolve(Paths.get("noarch/duplicate-file1-1-1.noarch.rpm")));
        validator = createStrictMock(RpmFilesizeValidator.class);
    }

    private void runTest() throws Exception {
        replay(validator);
        validator.validate(rpmPathInfo);
        verify(validator);
    }

    @Test
    public void testAllowedFileSize() throws Exception {
        expect(validator.allowedFilesize(rpmPathInfo, 6488L)).andReturn(true);
        runTest();
        assertPass(validator);
    }

    @Test
    public void testDisallowedFileSize() throws Exception {
        expect(validator.allowedFilesize(rpmPathInfo, 6488L)).andReturn(false);
        runTest();
        assertFailOne(validator);
    }
    */
}
