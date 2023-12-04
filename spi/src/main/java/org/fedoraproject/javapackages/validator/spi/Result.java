package org.fedoraproject.javapackages.validator.spi;

import java.util.Iterator;

/**
 * The result of validation. Contains the result state and its own log.
 *
 * @author Marián Konček
 */
public interface Result extends Iterable<LogEntry> {
    /**
     * Returns the result state of the validation.
     * @return The final result.
     */
    TestResult getResult();

    /**
     * Returns an iterator over the messages contained in the result.
     * @return An iterator over the log messages.
     */
    @Override
    Iterator<LogEntry> iterator();
}
