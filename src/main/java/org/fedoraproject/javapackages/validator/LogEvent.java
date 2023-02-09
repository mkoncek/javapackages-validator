package org.fedoraproject.javapackages.validator;

import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public enum LogEvent {
    debug("DEBUG", Decoration.bright_magenta, Decoration.bold),
    info("INFO", Decoration.cyan, Decoration.bold),
    pass("PASS", Decoration.green, Decoration.bold),
    fail("FAIL", Decoration.red, Decoration.bold),
    error("ERROR", Decoration.bright_red, Decoration.bold),
    ;

    private final Decorated decoratedText;

    private LogEvent(String text, Decoration... decorations) {
        this.decoratedText = Decorated.custom(text, decorations);
    }

    public Decorated getDecoratedText() {
        return decoratedText;
    }
}
