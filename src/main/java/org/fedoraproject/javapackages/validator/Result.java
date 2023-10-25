package org.fedoraproject.javapackages.validator;

import java.util.Iterator;

public interface Result extends Iterable<LogEntry> {
    /**
     * Returns the result of the validation as a single enum.
     * @return the final result.
     */
    TestResult getResult();

    /**
     * Returns an iterator over the messages contained in the result.
     * @return an iterator over the log messages.
     */
    @Override
    Iterator<LogEntry> iterator();
}
