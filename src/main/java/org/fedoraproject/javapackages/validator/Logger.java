package org.fedoraproject.javapackages.validator;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.EnumMap;

class Logger {
    private EnumMap<LogEvent, PrintStream> streams = new EnumMap<>(LogEvent.class);

    public Logger() {
        setStream(LogEvent.debug, Main.getDebugOutputStream());
        setStream(LogEvent.info, System.err);
    }

    public void setStream(LogEvent logEvent, PrintStream stream) {
        streams.put(logEvent, stream);
    }

    private void log(LogEvent logEvent, String pattern, Decorated... arguments) {
        streams.get(logEvent).append("[" + logEvent.getDecoratedText() + "] ").println(MessageFormat.format(pattern, (Object[]) arguments));
    }

    public void debug(String pattern, Decorated... arguments) {
        log(LogEvent.debug, pattern, arguments);
    }

    public void info(String pattern, Decorated... arguments) {
        log(LogEvent.info, pattern, arguments);
    }
}
