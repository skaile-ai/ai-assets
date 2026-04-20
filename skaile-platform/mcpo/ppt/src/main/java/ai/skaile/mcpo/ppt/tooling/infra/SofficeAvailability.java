package ai.skaile.mcpo.ppt.tooling.infra;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches the result of probing {@code soffice --headless --version} at startup. Tools that need
 * LibreOffice (PDF export today, high-fidelity render / non-PPTX export in later phases) read
 * this singleton instead of shelling out per call.
 *
 * <p>The probe tolerates missing binaries, non-zero exits, and timeouts — all produce an
 * {@code unavailable} result rather than throwing.
 */
public final class SofficeAvailability {
    private static final Logger LOG = LoggerFactory.getLogger(SofficeAvailability.class);
    private static final long PROBE_TIMEOUT_SECONDS = 5;
    private static volatile SofficeAvailability cached;

    private final boolean available;
    private final String version;
    private final String executable;
    private final String error;

    private SofficeAvailability(boolean available, String version, String executable, String error) {
        this.available = available;
        this.version = version;
        this.executable = executable;
        this.error = error;
    }

    public boolean available() {
        return available;
    }

    public String version() {
        return version;
    }

    public String executable() {
        return executable;
    }

    public String error() {
        return error;
    }

    public static SofficeAvailability get() {
        SofficeAvailability snapshot = cached;
        if (snapshot == null) {
            synchronized (SofficeAvailability.class) {
                snapshot = cached;
                if (snapshot == null) {
                    snapshot = probe(resolveExecutable());
                    cached = snapshot;
                }
            }
        }
        return snapshot;
    }

    /** Visible for tests and for explicit re-probing after environment changes. */
    public static synchronized void reset() {
        cached = null;
    }

    static SofficeAvailability probe(String executable) {
        ProcessBuilder pb = new ProcessBuilder(Arrays.asList(executable, "--headless", "--version"));
        pb.redirectErrorStream(true);
        Process process = null;
        try {
            process = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (out.length() > 0) {
                        out.append('\n');
                    }
                    out.append(line);
                }
            }
            boolean finished = process.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOG.warn("soffice version probe timed out after {}s", PROBE_TIMEOUT_SECONDS);
                return new SofficeAvailability(false, "", executable, "probe_timeout");
            }
            if (process.exitValue() != 0) {
                LOG.warn("soffice version probe exited with code {}: {}", process.exitValue(), out);
                return new SofficeAvailability(false, "", executable,
                        "exit_code_" + process.exitValue());
            }
            String version = out.toString().strip();
            LOG.info("soffice available: {}", version);
            return new SofficeAvailability(true, version, executable, null);
        } catch (IOException ex) {
            LOG.warn("soffice binary '{}' not runnable: {}", executable, ex.getMessage());
            return new SofficeAvailability(false, "", executable, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            return new SofficeAvailability(false, "", executable, "interrupted");
        }
    }

    private static String resolveExecutable() {
        String raw = System.getenv("SOFFICE_PATH");
        if (raw == null || raw.isBlank()) {
            return "soffice";
        }
        return raw;
    }
}
