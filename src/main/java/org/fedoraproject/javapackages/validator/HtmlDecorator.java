package org.fedoraproject.javapackages.validator;

import java.util.EnumMap;
import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;

public class HtmlDecorator implements TextDecorator {
    public static final HtmlDecorator INSTANCE = new HtmlDecorator();

    private HtmlDecorator() {
        super();
    }

    private static final EnumMap<Decoration, String> DECORATIONS = new EnumMap<>(Decoration.class);
    static {
        DECORATIONS.put(Decoration.bold, "font-weight:bold;");
        DECORATIONS.put(Decoration.underline, "text-decoration:underline;");
        DECORATIONS.put(Decoration.black, "color:black;");
        DECORATIONS.put(Decoration.red, "color:darkred;");
        DECORATIONS.put(Decoration.green, "color:darkgreen;");
        DECORATIONS.put(Decoration.yellow, "color:gold;");
        DECORATIONS.put(Decoration.blue, "color:darkblue;");
        DECORATIONS.put(Decoration.magenta, "color:darkmagenta;");
        DECORATIONS.put(Decoration.cyan, "color:darkcyan;");
        DECORATIONS.put(Decoration.white, "color:ghostwhite;");

        DECORATIONS.put(Decoration.bright_black, "color:darkgrey;");
        DECORATIONS.put(Decoration.bright_red, "color:red;");
        DECORATIONS.put(Decoration.bright_green, "color:green;");
        DECORATIONS.put(Decoration.bright_yellow, "color:goldenrod;");
        DECORATIONS.put(Decoration.bright_blue, "color:blue;");
        DECORATIONS.put(Decoration.bright_magenta, "color:magenta;");
        DECORATIONS.put(Decoration.bright_cyan, "color:#00e5ff;");
        DECORATIONS.put(Decoration.bright_white, "color:white;");
    }

    @Override
    public String decorate(Object object, Decoration... decorations) {
        Decoration color = null;

        for (var decoration : decorations) {
            if (decoration != Decoration.bold && decoration != Decoration.underline) {
                if (color != null) {
                    throw new IllegalArgumentException("Multiple colors specified");
                } else {
                    color = decoration;
                }
            }
        }

        if (color == null) {
            color = Decoration.black;
        }

        var result = new StringBuilder("<text style=\"");

        for (var decoration : decorations) {
            switch (decoration) {
            case bold:
            case underline:
                result.append(DECORATIONS.get(decoration));
                break;
            default:
                continue;
            }
        }

        switch (color) {
        case black:
        case red:
        case green:
        case yellow:
        case blue:
        case magenta:
        case cyan:
        case white:
        case bright_black:
        case bright_red:
        case bright_green:
        case bright_yellow:
        case bright_blue:
        case bright_magenta:
        case bright_cyan:
        case bright_white:
            result.append(DECORATIONS.get(color));
            break;
        default:
            break;
        }

        result.append("\">");
        result.append(StringEscapeUtils.escapeHtml4(Objects.toString(object)).replace(System.lineSeparator(), "<br>"));
        result.append("</text>");

        return result.toString();
    }
}
