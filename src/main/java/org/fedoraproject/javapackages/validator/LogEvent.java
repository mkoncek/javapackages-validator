package org.fedoraproject.javapackages.validator;

import java.text.MessageFormat;

import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public enum LogEvent {
    debug("DEBUG", Decoration.bright_magenta, Decoration.bold),
    info("INFO", Decoration.cyan, Decoration.bold),
    pass("PASS", Decoration.green, Decoration.bold),
    fail("FAIL", Decoration.red, Decoration.bold),
    ;

    private final Decorated decoratedText;

    private LogEvent(String text, Decoration... decorations) {
        this.decoratedText = Decorated.custom(text, decorations);
    }

    public Decorated getDecoratedText() {
        return decoratedText;
    }

    public String withFormat(String pattern, Decorated... arguments) {
        return "[" + getDecoratedText() + "] " + MessageFormat.format(pattern, (Object[]) arguments);
    }
}
