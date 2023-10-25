package org.fedoraproject.javapackages.validator;

import java.time.LocalDateTime;
import java.util.Iterator;

class NamedResult implements Result {
    private Result delegate;
    private String testName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    NamedResult(Result delegate, String testName, LocalDateTime startTime, LocalDateTime endTime) {
        this.delegate = delegate;
        this.testName = testName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    NamedResult(Result delegate, String testName) {
        this(delegate, testName, null, null);
    }

    @Override
    public Iterator<LogEntry> iterator() {
        return delegate.iterator();
    }

    @Override
    public TestResult getResult() {
        return delegate.getResult();
    }

    public String getTestName() {
        return testName;
    }

    LocalDateTime getStartTime() {
        return startTime;
    }

    LocalDateTime getEndTime() {
        return endTime;
    }
}
