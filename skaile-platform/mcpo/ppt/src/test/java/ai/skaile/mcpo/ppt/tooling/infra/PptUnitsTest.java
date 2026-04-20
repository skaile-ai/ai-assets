package ai.skaile.mcpo.ppt.tooling.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PptUnitsTest {

    @Test
    void pointsRoundTripThroughEmu() {
        assertEquals(36.0, PptUnits.emuToPoints(PptUnits.pointsToEmu(36.0)), 1e-9);
    }

    @Test
    void pointToEmuMatchesKnownConstant() {
        // 1 inch = 72 points = 914_400 EMU
        assertEquals(914_400L, PptUnits.pointsToEmu(72.0));
    }

    @Test
    void inchesRoundTripThroughEmu() {
        assertEquals(2.5, PptUnits.emuToInches(PptUnits.inchesToEmu(2.5)), 1e-9);
    }

    @Test
    void pixelsAt96DpiAndPointsAt72DpiAreRelatedByFourThirds() {
        // 72 pt * (96/72) = 96 px
        assertEquals(96.0, PptUnits.pointsToPixels(72.0, 96.0), 1e-9);
        // Inverse
        assertEquals(72.0, PptUnits.pixelsToPoints(96.0, 96.0), 1e-9);
    }

    @Test
    void rejectsNonPositiveDpi() {
        assertThrows(IllegalArgumentException.class,
                () -> PptUnits.pointsToPixels(10, 0));
        assertThrows(IllegalArgumentException.class,
                () -> PptUnits.pixelsToPoints(10, -5));
    }
}
