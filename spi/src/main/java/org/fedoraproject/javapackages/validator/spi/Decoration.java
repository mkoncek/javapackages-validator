package org.fedoraproject.javapackages.validator.spi;

import java.util.Arrays;
import java.util.Optional;

/**
 * A record representing possible decorations applied to an object.
 * @param color An optional color.
 * @param modifiers Zero or more modifiers.
 *
 * @author Marián Konček
 */
public record Decoration(Optional<Color> color, Modifier... modifiers) {
    /**
     * Construct a decoration using a color and optional modifiers.
     * @param color The color to use, must not be {@code null}.
     * @param modifiers Modifiers to use.
     */
    public Decoration(Color color, Modifier... modifiers) {
        this(Optional.of(color), Arrays.copyOf(modifiers, modifiers.length));
    }

    /**
     * Construct a decoration using the default color and optional modifiers.
     * @param modifiers Modifiers to use.
     */
    public Decoration(Modifier... modifiers) {
        this(Optional.empty(), modifiers);
    }

    /**
     * Get the modifiers of this instance.
     * @return The modifiers of this instance.
     */
    @Override
    public Modifier[] modifiers() {
        return Arrays.copyOf(modifiers, modifiers.length);
    }

    /**
     * A decoration instance which specifies no decorations.
     */
    public static final Decoration NONE = new Decoration();

    /**
     * A color used for decorating text.
     */
    public static enum Color {
        /**
         * Black color.
         */
        black,

        /**
         * Red color.
         */
        red,

        /**
         * Green color.
         */
        green,

        /**
         * Yellow color.
         */
        yellow,

        /**
         * Blue color.
         */
        blue,

        /**
         * Magenta color.
         */
        magenta,

        /**
         * Cyan color.
         */
        cyan,

        /**
         * White color.
         */
        white,
    };

    /**
     * Modifiers to used for decorating text. Multiple modifiers can be
     * combined.
     */
    public static enum Modifier {
        /**
         * Bold modifier.
         */
        bold,

        /**
         * Underline modifier.
         */
        underline,

        /**
         * Bright modifier.
         */
        bright,
    }
}
