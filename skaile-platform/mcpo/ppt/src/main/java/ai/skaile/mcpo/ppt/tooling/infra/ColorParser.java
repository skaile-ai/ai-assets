package ai.skaile.mcpo.ppt.tooling.infra;

import java.awt.Color;
import java.util.Optional;

/**
 * Central hex-color parsing. All tool handlers that accept a color argument
 * must route through here so that malformed inputs surface as a uniform
 * {@code INVALID_COLOR} response code and the accepted format stays consistent
 * across the tool surface.
 *
 * <p>Accepted input: {@code #RRGGBB} or {@code RRGGBB} — exactly six hex digits,
 * optional leading {@code #}, leading/trailing whitespace ignored, case
 * insensitive.
 */
public final class ColorParser {

    private ColorParser() {
    }

    /**
     * Thrown when a color string cannot be parsed. Subclasses {@link IllegalArgumentException}
     * so existing {@code call()} dispatch paths continue to surface it as a validation error,
     * but callers that want the uniform {@code INVALID_COLOR} code can catch this specific
     * type and translate it.
     */
    public static final class InvalidColorException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        public InvalidColorException(String message) {
            super(message);
        }
    }

    /** Parse {@code #RRGGBB}. Throws {@link InvalidColorException} on any malformed input. */
    public static Color parseHex(String input) {
        return tryParseHex(input).orElseThrow(
                () -> new InvalidColorException(
                        "color must be in #RRGGBB format: " + (input == null ? "null" : input)));
    }

    /** Non-throwing variant suitable for handler validation paths. */
    public static Optional<Color> tryParseHex(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String value = input.strip();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return Optional.empty();
        }
        try {
            int rgb = Integer.parseInt(value, 16);
            return Optional.of(new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /** Render a {@link Color} as {@code #RRGGBB}. Alpha is dropped. */
    public static String toHex(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("color must not be null");
        }
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
