package org.fedoraproject.javapackages.validator.validators;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.fedoraproject.javapackages.validator.Main;
import org.fedoraproject.javapackages.validator.RpmPathInfo;
import org.fedoraproject.javapackages.validator.TextDecorator.Decoration;

public abstract class Validator {
    protected Collection<String> failMessages = new ArrayList<>(0);
    protected Collection<String> passMessages = new ArrayList<>(0);

    public boolean failed() {
        return !failMessages.isEmpty();
    }

    protected static final Decoration DECORATION_RPM = Decoration.bright_red;
    protected static final Decoration DECORATION_ACTUAL = Decoration.bright_cyan;
    protected static final Decoration DECORATION_EXPECTED = Decoration.bright_magenta;

    protected static String textDecorate(Object object, Decoration... decorations) {
        return Main.getDecorator().decorate(object, decorations);
    }

    protected static String listDecorate(Collection<?> list, Decoration... decorations) {
        return "[" + list.stream().map(obj -> textDecorate(obj, decorations)).collect(Collectors.joining(", ")) + "]";
    }

    protected final void fail(String pattern, Object... arguments) {
        StringBuffer result = new StringBuffer(256);
        result.append('[');
        result.append(textDecorate("FAIL", Decoration.red, Decoration.bold));
        result.append("] ");
        result.append(MessageFormat.format(pattern, arguments));
        failMessages.add(result.toString());
    }

    protected final void pass(String pattern, Object... arguments) {
        StringBuffer result = new StringBuffer(256);
        result.append('[');
        result.append(textDecorate("PASS", Decoration.green, Decoration.bold));
        result.append("] ");
        result.append(MessageFormat.format(pattern, arguments));
        failMessages.add(result.toString());
    }

    protected final void warn(String pattern, Object... arguments) {
        // TODO
    }

    protected final void info(String pattern, Object... arguments) {
        // TODO
    }

    protected final void debug(String pattern, Object... arguments) {
        // TODO
    }

    abstract public void validate(Iterator<RpmPathInfo> rpmIt) throws IOException;
}
