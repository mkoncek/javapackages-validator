package org.fedoraproject.javapackages.validator;

import java.util.Arrays;

public record LogEntry(LogEvent kind, String pattern, Decorated... objects) {
    public LogEntry(LogEvent kind, String pattern, Decorated... objects) {
        this.kind = kind;
        this.pattern = pattern;
        this.objects = Arrays.copyOf(objects, objects.length);
    }

    @Override
    public Decorated[] objects() {
        return Arrays.copyOf(objects, objects.length);
    }

    public static LogEntry debug(String pattern, Decorated... arguments) {
        return new LogEntry(LogEvent.debug, pattern, arguments);
    }

    public static LogEntry info(String pattern, Decorated... arguments) {
        return new LogEntry(LogEvent.info, pattern, arguments);
    }

    public static LogEntry pass(String pattern, Decorated... arguments) {
        return new LogEntry(LogEvent.pass, pattern, arguments);
    }

    public static LogEntry fail(String pattern, Decorated... arguments) {
        return new LogEntry(LogEvent.fail, pattern, arguments);
    }

    public static LogEntry error(String pattern, Decorated... arguments) {
        return new LogEntry(LogEvent.error, pattern, arguments);
    }
}
