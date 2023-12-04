package org.fedoraproject.javapackages.validator.spi;

import java.util.Arrays;

/**
 * A logging event that occurred during validation.
 * @param kind The kind of this log entry.
 * @param pattern A string pattern of this log entry's message in the same form
 * as in {@link java.text.MessageFormat}.
 * @param objects Objects to fill the {@code pattern} with.
 *
 * @author Marián Konček
 */
public record LogEntry(LogEvent kind, String pattern, Decorated... objects) {
    /**
     * Create a log entry of type {@code kind}. {@code pattern} is a string
     * pattern in the same format as would be used in {@link java.text.MessageFormat}
     * together with the decorated {@code objects}.
     * @param kind The kind of this log entry.
     * @param pattern A string pattern of this log entry's message.
     * @param objects Objects to fill the {@code pattern} with.
     */
    public LogEntry(LogEvent kind, String pattern, Decorated... objects) {
        this.kind = kind;
        this.pattern = pattern;
        this.objects = Arrays.copyOf(objects, objects.length);
    }

    /**
     * Get the objects used to fill the message pattern of this instance.
     * @return The objects used to fill the message pattern of this instance.
     */
    @Override
    public Decorated[] objects() {
        return Arrays.copyOf(objects, objects.length);
    }

    /**
     * Produce a message of {@link org.fedoraproject.javapackages.validator.spi.LogEvent#debug}
     * kind with {@code pattern} and {@code objects} used as the log message.
     * @param pattern A string pattern of the message.
     * @param objects Objects to fill the {@code pattern} with.
     * @return The resulting {@code LogEntry}.
     */
    public static LogEntry debug(String pattern, Decorated... objects) {
        return new LogEntry(LogEvent.debug, pattern, objects);
    }

    /**
     * Produce a message of {@link org.fedoraproject.javapackages.validator.spi.LogEvent#skip}
     * kind with {@code pattern} and {@code objects} used as the log message.
     * @param pattern A string pattern of the message.
     * @param objects Objects to fill the {@code pattern} with.
     * @return The resulting {@code LogEntry}.
     */
    public static LogEntry skip(String pattern, Decorated... objects) {
        return new LogEntry(LogEvent.skip, pattern, objects);
    }

    /**
     * Produce a message of {@link org.fedoraproject.javapackages.validator.spi.LogEvent#pass}
     * kind with {@code pattern} and {@code objects} used as the log message.
     * @param pattern A string pattern of the message.
     * @param objects Objects to fill the {@code pattern} with.
     * @return The resulting {@code LogEntry}.
     */
    public static LogEntry pass(String pattern, Decorated... objects) {
        return new LogEntry(LogEvent.pass, pattern, objects);
    }

    /**
     * Produce a message of {@link org.fedoraproject.javapackages.validator.spi.LogEvent#info}
     * kind with {@code pattern} and {@code objects} used as the log message.
     * @param pattern A string pattern of the message.
     * @param objects Objects to fill the {@code pattern} with.
     * @return The resulting {@code LogEntry}.
     */
    public static LogEntry info(String pattern, Decorated... objects) {
        return new LogEntry(LogEvent.info, pattern, objects);
    }

    /**
     * Produce a message of {@link org.fedoraproject.javapackages.validator.spi.LogEvent#warn}
     * kind with {@code pattern} and {@code objects} used as the log message.
     * @param pattern A string pattern of the message.
     * @param objects Objects to fill the {@code pattern} with.
     * @return The resulting {@code LogEntry}.
     */
    public static LogEntry warn(String pattern, Decorated... objects) {
        return new LogEntry(LogEvent.warn, pattern, objects);
    }

    /**
     * Produce a message of {@link org.fedoraproject.javapackages.validator.spi.LogEvent#fail}
     * kind with {@code pattern} and {@code objects} used as the log message.
     * @param pattern A string pattern of the message.
     * @param objects Objects to fill the {@code pattern} with.
     * @return The resulting {@code LogEntry}.
     */
    public static LogEntry fail(String pattern, Decorated... objects) {
        return new LogEntry(LogEvent.fail, pattern, objects);
    }

    /**
     * Produce a message of {@link org.fedoraproject.javapackages.validator.spi.LogEvent#error}
     * kind with {@code pattern} and {@code objects} used as the log message.
     * @param pattern A string pattern of the message.
     * @param objects Objects to fill the {@code pattern} with.
     * @return The resulting {@code LogEntry}.
     */
    public static LogEntry error(String pattern, Decorated... objects) {
        return new LogEntry(LogEvent.error, pattern, objects);
    }
}
