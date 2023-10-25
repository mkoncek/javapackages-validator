package org.fedoraproject.javapackages.validator;

import java.util.List;
import java.util.Objects;

import org.fedoraproject.javadeptools.rpm.RpmFile;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public class Decorated {
    private static final Decoration DECORATION_RPM = Decoration.bright_red;
    private static final Decoration DECORATION_EXPECTED = Decoration.bright_cyan;
    private static final Decoration DECORATION_ACTUAL = Decoration.bright_magenta;
    private static final Decoration DECORATION_CLASS = Decoration.bright_yellow;

    /**
     * Decoration used for describing objects which hold inspected values
     */
    private static final Decoration DECORATION_OUTER = Decoration.bright_blue;

    private final String object;
    private final Decoration[] decorations;

    private Decorated(Object object, Decoration... decorations) {
        if (object instanceof Decorated) {
            throw new IllegalArgumentException("Object is already decorated");
        }
        this.object = Objects.toString(object);
        this.decorations = decorations;
    }

    public static Decorated rpm(RpmFile rpm) {
        return new Decorated(rpm, DECORATION_RPM);
    }

    public static Decorated actual(Object actual) {
        return new Decorated(actual, DECORATION_ACTUAL);
    }

    public static Decorated struct(Object struct) {
        return new Decorated(struct, DECORATION_CLASS);
    }

    public static Decorated expected(Object expected) {
        return new Decorated(expected, DECORATION_EXPECTED);
    }

    public static Decorated outer(Object outer) {
        return new Decorated(outer, DECORATION_OUTER);
    }

    public static Decorated list(List<?> list) {
        return new Decorated(list.toString());
    }

    public static Decorated plain(Object obj) {
        return new Decorated(obj);
    }

    public static Decorated custom(Object obj, Decoration... decorations) {
        return new Decorated(obj, decorations);
    }

    Object getObject() {
        return object;
    }

    Decoration[] getDecorations() {
        return decorations;
    }

    @Override
    public String toString() {
        return Main.getDecorator().decorate(this);
    }
}
