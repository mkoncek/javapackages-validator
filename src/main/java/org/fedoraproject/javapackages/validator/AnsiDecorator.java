package org.fedoraproject.javapackages.validator;

import java.util.Objects;

import org.fedoraproject.javapackages.validator.spi.Decorated;

class AnsiDecorator implements TextDecorator {
    public static final AnsiDecorator INSTANCE = new AnsiDecorator();

    private AnsiDecorator() {
        super();
    }

    @Override
    public String decorate(Decorated decorated) {
        var result = new StringBuilder("\033[");

        var colorCode = decorated.getDecoration().color().map(color -> switch (color) {
            case black -> 30;
            case red -> 31;
            case green -> 32;
            case yellow -> 33;
            case blue -> 34;
            case magenta -> 35;
            case cyan -> 36;
            case white -> 37;
        }).orElse(37);

        for (var modifier : decorated.getDecoration().modifiers()) {
            switch (modifier) {
                case bold -> {result.append("1;");}
                case underline -> {result.append("4;");}
                case bright -> {colorCode += 60;}
            }
        }

        result.append(colorCode.toString());
        result.append("m");

        result.append(Objects.toString(decorated.getObject()));
        result.append("\033[0m");

        return result.toString();
    }
}
