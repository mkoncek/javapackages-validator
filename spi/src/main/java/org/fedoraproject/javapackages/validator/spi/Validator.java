package org.fedoraproject.javapackages.validator.spi;

import java.util.List;

import io.kojan.javadeptools.rpm.RpmPackage;

/**
 * An interface used for validating RPM files.
 *
 * @author Marián Konček
 */
public interface Validator {
    /**
     * The test name used when the validator is invoked in the TMT mode.
     * Must start with '/'.
     * @return The test name of the TMT test.
     */
    String getTestName();

    /**
     * Validate the RPM files and produce the result describing what was checked.
     * This function should wrap any thrown exceptions in the returned result as
     * the {@link org.fedoraproject.javapackages.validator.spi.LogEvent#error}
     * event.
     * @param rpms The RPM files to validate.
     * @param args Optional arguments passed to the validator, may be {@code null}.
     * @return The result of the validation.
     */
    Result validate(Iterable<RpmPackage> rpms, List<String> args);
}
