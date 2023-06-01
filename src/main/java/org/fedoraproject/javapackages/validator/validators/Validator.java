package org.fedoraproject.javapackages.validator.validators;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.tuple.Pair;
import org.fedoraproject.javapackages.validator.Decorated;
import org.fedoraproject.javapackages.validator.LogEvent;
import org.fedoraproject.javapackages.validator.RpmInfoURI;
import org.fedoraproject.javapackages.validator.TestResult;

public abstract class Validator {
    protected List<Pair<LogEvent, String>> log = new ArrayList<>();
    protected TestResult testResult = TestResult.info;

    /**
     * Handle arguments passed on CLI. Executed once before the execution of the validator,
     * @param args Arguments. Never null.
     */
    public void arguments(String[] args) {
        throw new IllegalArgumentException(getClass().getSimpleName() + " does not recognize optional arguments");
    }

    public TestResult getResult() {
        return testResult;
    }

    public final List<Pair<LogEvent, String>> getMessages() {
        return new UnmodifiableList<>(log);
    }

    private final void addLog(LogEvent kind, String pattern, Decorated... arguments) {
        log.add(Pair.of(kind, MessageFormat.format(pattern, (Object[]) arguments)));
    }

    protected final void fail(String pattern, Decorated... arguments) {
        addLog(LogEvent.fail, pattern, arguments);
        if (TestResult.fail.compareTo(testResult) > 0) {
            testResult = TestResult.fail;
        }
    }

    protected final void pass(String pattern, Decorated... arguments) {
        addLog(LogEvent.pass, pattern, arguments);
        if (TestResult.pass.compareTo(testResult) > 0) {
            testResult = TestResult.pass;
        }
    }

    protected final void debug(String pattern, Decorated... arguments) {
        addLog(LogEvent.debug, pattern, arguments);
    }

    protected final void info(String pattern, Decorated... arguments) {
        addLog(LogEvent.info, pattern, arguments);
    }

    public final void pubvalidate(Iterator<RpmInfoURI> rpmIt) {
        try {
            validate(rpmIt);
        } catch (Exception ex) {
            var stackTrace = new ByteArrayOutputStream();
            ex.printStackTrace(new PrintStream(stackTrace, false, StandardCharsets.UTF_8));
            addLog(LogEvent.error, "An exception occured:{0}{1}",
                    Decorated.plain(System.lineSeparator()),
                    Decorated.plain(new String(stackTrace.toByteArray(), StandardCharsets.UTF_8)));
            testResult = TestResult.error;
        }
    }

    protected abstract void validate(Iterator<RpmInfoURI> rpmIt) throws Exception;
}
