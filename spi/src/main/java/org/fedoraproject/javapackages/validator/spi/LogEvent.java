package org.fedoraproject.javapackages.validator.spi;

/**
 * Enum used for distinguishing logging messages.
 *
 * @author Marián Konček
 */
public enum LogEvent {
    /**
     * A debug message that is useful when debugging the validator.
     */
    debug("DEBUG", new Decoration(Decoration.Color.magenta, Decoration.Modifier.bold, Decoration.Modifier.bright)),

    /**
     * A message type used for describing why the execution was skipped.
     */
    skip("SKIP", new Decoration(Decoration.Color.black, Decoration.Modifier.bold, Decoration.Modifier.bright)),

    /**
     * A message type used for messages about passing checks.
     */
    pass("PASS", new Decoration(Decoration.Color.green, Decoration.Modifier.bold)),

    /**
     * A message type used for messages about checks which are are considered
     * neither successful nor failing.
     */
    info("INFO", new Decoration(Decoration.Color.cyan, Decoration.Modifier.bold)),

    /**
     * A message type used for warning messages.
     */
    warn("WARN", new Decoration(Decoration.Color.yellow, Decoration.Modifier.bold)),

    /**
     * A message type used for messages about failing checks.
     */
    fail("FAIL", new Decoration(Decoration.Color.red, Decoration.Modifier.bold)),

    /**
     * A message type used for error messages because of which the validator
     * could not finish execution.
     */
    error("ERROR", new Decoration(Decoration.Color.red, Decoration.Modifier.bold, Decoration.Modifier.bright)),
    ;

    private final Decorated decorated;

    private LogEvent(String text, Decoration decoration) {
        this.decorated = Decorated.custom(text, decoration);
    }

    /**
     * Get the decorated instance of the enum field's text.
     * @return The decorated instance of the enum field's text.
     */
    public Decorated getDecorated() {
        return decorated;
    }
}
