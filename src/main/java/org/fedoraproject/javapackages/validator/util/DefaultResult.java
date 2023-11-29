package org.fedoraproject.javapackages.validator.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.fedoraproject.javapackages.validator.spi.Decorated;
import org.fedoraproject.javapackages.validator.spi.LogEntry;
import org.fedoraproject.javapackages.validator.spi.Result;
import org.fedoraproject.javapackages.validator.spi.TestResult;

public class DefaultResult implements Result {
    private List<LogEntry> log = new ArrayList<>();
    private TestResult result = TestResult.skip;

    @Override
    public TestResult getResult() {
        return result;
    }

    @Override
    public Iterator<LogEntry> iterator() {
        return log.iterator();
    }

    public void addResult(TestResult result) {
        if (result.compareTo(getResult()) > 0) {
            this.result = result;
        }
    }

    public void addLog(LogEntry logEntry) {
        log.add(logEntry);
    }

    public static LogEntry logException(Exception ex) {
        var stackTrace = new ByteArrayOutputStream();
        ex.printStackTrace(new PrintStream(stackTrace, false, StandardCharsets.UTF_8));
        return LogEntry.error("An exception occured:{0}{1}",
                Decorated.plain(System.lineSeparator()),
                Decorated.plain(new String(stackTrace.toByteArray(), StandardCharsets.UTF_8)));
    }

    public void debug(String pattern, Decorated... arguments) {
        addLog(LogEntry.debug(pattern, arguments));
    }

    public void skip() {
        addResult(TestResult.skip);
    }

    public void skip(String pattern, Decorated... arguments) {
        skip();
        addLog(LogEntry.skip(pattern, arguments));
    }

    public void pass() {
        addResult(TestResult.pass);
    }

    public void pass(String pattern, Decorated... arguments) {
        pass();
        addLog(LogEntry.pass(pattern, arguments));
    }

    public void info() {
        addResult(TestResult.info);
    }

    public void info(String pattern, Decorated... arguments) {
        info();
        addLog(LogEntry.info(pattern, arguments));
    }

    public void warn() {
        addResult(TestResult.warn);
    }

    public void warn(String pattern, Decorated... arguments) {
        warn();
        addLog(LogEntry.warn(pattern, arguments));
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
