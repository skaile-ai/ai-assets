package ai.skaile.mcpo.ppt.tooling.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SofficeAvailabilityTest {

    @BeforeEach
    @AfterEach
    void reset() {
        SofficeAvailability.reset();
    }

    @Test
    void probeReturnsUnavailableForMissingExecutable() {
        SofficeAvailability result = SofficeAvailability.probe(
                "/definitely/not/a/real/soffice-binary-path");
        assertFalse(result.available());
        assertEquals("", result.version());
        assertNotNull(result.error());
    }

    @Test
    void probeReturnsUnavailableWhenCommandExitsNonZero() {
        // `false` is a POSIX util that exits 1 with no output. Its exit code drives our
        // "not available" branch; the call must not throw.
        SofficeAvailability result = SofficeAvailability.probe("false");
        assertFalse(result.available());
        assertTrue(result.error().startsWith("exit_code_"));
    }

    @Test
    void probeReturnsAvailableWhenCommandSucceeds() {
        // `true` exits 0 with no output. The probe treats that as success with empty version.
        SofficeAvailability result = SofficeAvailability.probe("true");
        assertTrue(result.available());
        assertEquals("true", result.executable());
        assertEquals("", result.version());
    }

    @Test
    void getReturnsCachedSingleton() {
        SofficeAvailability first = SofficeAvailability.get();
        SofficeAvailability second = SofficeAvailability.get();
        assertEquals(first, second);
    }
}
