package org.fedoraproject.javapackages.validator.spi;

/**
 * Enum representing the state of the result of validation.
 *
 * @author Marián Konček
 */
public enum TestResult {
    /**
     * Validation was expectedly skipped because the validator determined so.
     */
    skip,

    /**
     * Validation was run successfully and all the checks passed.
     */
    pass,

    /**
     * Validation was run successfully but the validator found neither the
     * expected values nor violations and only produced information about its
     * run.
     */
    info,

    /**
     * Validation was run successfully but the validator only produced warning
     * information.
     */
    warn,

    /**
     * Validation was run successfully but some of the checks failed.
     */
    fail,

    /**
     * An error occurred during the validation because of which it did not finish
     * successfully.
     */
    error,
    ;
}
