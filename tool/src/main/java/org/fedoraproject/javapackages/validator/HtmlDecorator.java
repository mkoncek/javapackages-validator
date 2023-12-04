package org.fedoraproject.javapackages.validator;

import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;
import org.fedoraproject.javapackages.validator.spi.Decorated;
import org.fedoraproject.javapackages.validator.spi.Decoration;

class HtmlDecorator implements TextDecorator {
    public static final HtmlDecorator INSTANCE = new HtmlDecorator();

    private HtmlDecorator() {
        super();
    }

    private static String basic(Decoration.Color color) {
        return switch (color) {
            case black -> "color:black;";
            case red -> "color:darkred;";
            case green -> "color:darkgreen;";
            case yellow -> "color:gold;";
            case blue -> "color:darkblue;";
            case magenta -> "color:darkmagenta;";
            case cyan -> "color:darkcyan;";
            case white -> "color:ghostwhite;";
        };
    }

    private static String bright(Decoration.Color color) {
        return switch (color) {
            case black -> "color:darkgrey;";
            case red -> "color:red;";
            case green -> "color:green;";
            case yellow -> "color:goldenrod;";
            case blue -> "color:blue;";
            case magenta -> "color:magenta;";
            case cyan -> "color:#00e5ff;";
            case white -> "color:white;";
        };
    }

    @Override
    public String decorate(Decorated decorated) {
        var result = new StringBuilder("<text style=\"");
        var color = decorated.getDecoration().color().orElse(Decoration.Color.black);
        var colorString = basic(color);

        for (var modifier : decorated.getDecoration().modifiers()) {
            switch (modifier) {
                case bold -> {result.append("font-weight:bold;");}
                case underline -> {result.append("text-decoration:underline;");}
                case bright -> {colorString = bright(color);}
            }
        }

        result.append(colorString);

        result.append("\">");
        result.append(StringEscapeUtils.escapeHtml4(Objects.toString(decorated.getObject())).replace(System.lineSeparator(), "<br>"));
        result.append("</text>");

        return result.toString();
    }
}
