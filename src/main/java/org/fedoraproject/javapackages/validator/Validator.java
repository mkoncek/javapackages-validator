package org.fedoraproject.javapackages.validator;

import org.fedoraproject.javadeptools.rpm.RpmFile;

public interface Validator {
    /**
     * The test name used when the validator is invoked in the TMT mode.
     * Must start with '/'.
     * @return the test name of the TMT test.
     */
    String getTestName();

    /**
     * Validate the RPM files and produce the result describing what was checked.
     * This function should wrap any thrown exceptions in the result as the `error` LogEvent.
     * @param rpms the RPM files to validate.
     * @param args optional arguments passed to the validator, may be null.
     * @return the result of the validation.
     */
    Result validate(Iterable<RpmFile> rpms, String[] args);
}
