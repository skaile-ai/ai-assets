package ai.skaile.mcpo.ppt.tooling.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import org.junit.jupiter.api.Test;

class ColorParserTest {

    @Test
    void parseHexAcceptsLeadingHash() {
        assertEquals(new Color(0xFF, 0x00, 0x00), ColorParser.parseHex("#FF0000"));
    }

    @Test
    void parseHexAcceptsWithoutLeadingHash() {
        assertEquals(new Color(0x12, 0x34, 0x56), ColorParser.parseHex("123456"));
    }

    @Test
    void parseHexIsCaseInsensitiveAndTrimsWhitespace() {
        assertEquals(new Color(0xab, 0xcd, 0xef), ColorParser.parseHex("  #abCDef "));
    }

    @Test
    void parseHexThrowsInvalidColorExceptionOnShortInput() {
        assertThrows(ColorParser.InvalidColorException.class, () -> ColorParser.parseHex("#FFF"));
    }

    @Test
    void parseHexThrowsInvalidColorExceptionOnNonHex() {
        assertThrows(ColorParser.InvalidColorException.class, () -> ColorParser.parseHex("#ZZZZZZ"));
    }

    @Test
    void parseHexThrowsInvalidColorExceptionOnNull() {
        assertThrows(ColorParser.InvalidColorException.class, () -> ColorParser.parseHex(null));
    }

    @Test
    void tryParseHexReturnsEmptyInsteadOfThrowing() {
        assertTrue(ColorParser.tryParseHex("nope").isEmpty());
        assertTrue(ColorParser.tryParseHex(null).isEmpty());
    }

    @Test
    void toHexProducesUppercaseSixDigitForm() {
        assertEquals("#0A1B2C", ColorParser.toHex(new Color(10, 27, 44)));
    }

    @Test
    void roundTrip() {
        Color original = new Color(0x42, 0x24, 0x88);
        assertEquals(original, ColorParser.parseHex(ColorParser.toHex(original)));
    }

    @Test
    void invalidColorExceptionIsAnIllegalArgumentException() {
        assertTrue(new ColorParser.InvalidColorException("x") instanceof IllegalArgumentException);
    }
}
