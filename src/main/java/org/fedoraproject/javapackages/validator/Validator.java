package org.fedoraproject.javapackages.validator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.fedoraproject.javadeptools.rpm.RpmFile;

public abstract class Validator {
    record LogEntry(LogEvent kind, String pattern, Decorated... objects) {
        String toString(TextDecorator decorator) {
            return MessageFormat.format(pattern, Stream.of(objects).map(a -> a.toString(decorator)).toArray());
        }
    }

    private List<LogEntry> log = new ArrayList<>();
    private TestResult testResult = TestResult.info;
    private LocalDateTime startTime = null;
    private LocalDateTime endTime = null;

    /**
     * Handle arguments passed on CLI. This method is executed once before the
     * execution of the validator. It is not executed if optional arguments were
     * not specified.
     * @param args Arguments. Never null.
     */
    public void arguments(String[] args) throws Exception {
        throw new IllegalArgumentException(getClass().getSimpleName() + " does not recognize optional arguments");
    }

    private static String toDashCase(String string) {
        var result = new StringBuilder();

        boolean wasLowerCase = false;

        for (int i = 0; i != string.length(); ++i) {
            if (Character.isLowerCase(string.charAt(i))) {
                wasLowerCase = true;
            } else if (Character.isUpperCase(string.charAt(i))) {
                if (wasLowerCase) {
                    result.append('-');
                }
                wasLowerCase = false;
            }

            result.append(Character.toLowerCase(string.charAt(i)));
        }

        return result.toString();
    }

    protected String getTestName() {
        var annotation = getClass().getAnnotation(TmtTest.class);
        if (annotation != null) {
            return annotation.value();
        } else {
            return toDashCase(getClass().getSimpleName());
        }
    }

    public TestResult getResult() {
        return testResult;
    }

    public final List<LogEntry> getMessages() {
        return Collections.unmodifiableList(log);
    }

    private void addLog(LogEvent kind, String pattern, Decorated... arguments) {
        log.add(new LogEntry(kind, pattern, arguments));
    }

    protected final void fail() {
        if (TestResult.fail.compareTo(testResult) > 0) {
            testResult = TestResult.fail;
        }
    }

    protected final void fail(String pattern, Decorated... arguments) {
        addLog(LogEvent.fail, pattern, arguments);
        fail();
    }

    protected final void pass() {
        if (TestResult.pass.compareTo(testResult) > 0) {
            testResult = TestResult.pass;
        }
    }

    protected final void pass(String pattern, Decorated... arguments) {
        addLog(LogEvent.pass, pattern, arguments);
        pass();
    }

    protected final void error() {
        if (TestResult.error.compareTo(testResult) > 0) {
            testResult = TestResult.error;
        }
    }

    protected final void error(String pattern, Decorated... arguments) {
        addLog(LogEvent.error, pattern, arguments);
        error();
    }

    protected final void debug(String pattern, Decorated... arguments) {
        addLog(LogEvent.debug, pattern, arguments);
    }

    protected final void info(String pattern, Decorated... arguments) {
        addLog(LogEvent.info, pattern, arguments);
    }

    final Validator pubvalidate(Iterable<RpmFile> rpms) {
        startTime = LocalDateTime.now(Clock.systemUTC());
        try {
            if (!testResult.equals(TestResult.error)) {
                validate(rpms);
            }
        } catch (Exception ex) {
            var stackTrace = new ByteArrayOutputStream();
            ex.printStackTrace(new PrintStream(stackTrace, false, StandardCharsets.UTF_8));
            error("An exception occured:{0}{1}",
                    Decorated.plain(System.lineSeparator()),
                    Decorated.plain(new String(stackTrace.toByteArray(), StandardCharsets.UTF_8)));
        }

        endTime = LocalDateTime.now(Clock.systemUTC());
        return this;
    }

    public final LocalDateTime getStartTime() {
        return startTime;
    }

    public final LocalDateTime getEndTime() {
        return endTime;
    }

    public final String getFormattedDuration() {
        var duration = Duration.between(startTime, endTime);
        return String.format("%02d:%02d:%02d.%d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart());
    }

    protected abstract void validate(Iterable<RpmFile> rpms) throws Exception;
}
