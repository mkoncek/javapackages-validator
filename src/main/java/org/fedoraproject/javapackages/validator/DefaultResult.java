package org.fedoraproject.javapackages.validator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultResult implements Result {
    private List<LogEntry> log = new ArrayList<>();
    private TestResult result = TestResult.info;

    @Override
    public TestResult getResult() {
        return result;
    }

    @Override
    public Iterator<LogEntry> iterator() {
        return log.iterator();
    }

    protected void addResult(TestResult result) {
        if (result.compareTo(getResult()) > 0) {
            this.result = result;
        }
    }

    protected void addLog(LogEntry logEntry) {
        log.add(logEntry);
    }

    public void debug(String pattern, Decorated... arguments) {
        addLog(LogEntry.debug(pattern, arguments));
    }

    public void info() {
        addResult(TestResult.info);
    }

    public void info(String pattern, Decorated... arguments) {
        info();
        addLog(LogEntry.info(pattern, arguments));
    }

    public void pass() {
        addResult(TestResult.pass);
    }

    public void pass(String pattern, Decorated... arguments) {
        pass();
        addLog(LogEntry.pass(pattern, arguments));
    }

    public void fail() {
        addResult(TestResult.fail);
    }

    public void fail(String pattern, Decorated... arguments) {
        fail();
        addLog(LogEntry.fail(pattern, arguments));
    }

    public void error() {
        addResult(TestResult.error);
    }

    public void error(String pattern, Decorated... arguments) {
        error();
        addLog(LogEntry.error(pattern, arguments));
    }
}
