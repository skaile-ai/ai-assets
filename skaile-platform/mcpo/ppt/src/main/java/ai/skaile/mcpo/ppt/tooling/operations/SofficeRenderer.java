package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.tooling.infra.PptPathResolver;
import ai.skaile.mcpo.ppt.tooling.infra.PptServerConfig;
import ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps LibreOffice (`soffice --headless --convert-to ...`) for high-fidelity
 * rendering and non-PPTX export. All soffice invocations across the process
 * serialize through a single {@link Semaphore} because headless LibreOffice is
 * not safe to run concurrently within one user profile.
 *
 * <p>Callers that need per-slide output (render_slide, *_batch) split the
 * in-memory {@code XMLSlideShow} into a single-slide temporary deck before
 * shelling out, which avoids post-filtering N converted files and also works
 * around the fact that {@code --convert-to <image>} only emits the first slide
 * for most LibreOffice builds.
 *
 * <p>Temp files live under {@link PptPathResolver#createSandboxTempFile} and
 * are always cleaned up, even on failure.
 */
public final class SofficeRenderer {
    private static final Logger LOG = LoggerFactory.getLogger(SofficeRenderer.class);
    private static final long SOFFICE_TIMEOUT_SECONDS = 90;
    private static final Semaphore SOFFICE_SEMAPHORE = new Semaphore(1);

    /**
     * Writable home directory handed to every soffice child process. Lazily created at first
     * use and reused process-wide. soffice stores its user profile under {@code $HOME/.config};
     * when the container runs as a uid with no {@code /etc/passwd} entry (e.g. the documented
     * {@code --user 1000:1000}), {@code HOME} defaults to {@code "/"} which is not writable
     * and soffice exits 77 before doing anything. Pointing {@code HOME} at a temp dir keeps
     * soffice working regardless of runtime uid.
     */
    private static volatile Path SOFFICE_HOME;
    private static final Object SOFFICE_HOME_LOCK = new Object();

    private final PptPathResolver pathResolver;
    private final PptServerConfig config;

    public SofficeRenderer(PptPathResolver pathResolver, PptServerConfig config) {
        this.pathResolver = pathResolver;
        this.config = config;
    }

    private static Path ensureSofficeHome() throws IOException {
        Path existing = SOFFICE_HOME;
        if (existing != null) {
            return existing;
        }
        synchronized (SOFFICE_HOME_LOCK) {
            if (SOFFICE_HOME == null) {
                Path dir = Files.createTempDirectory("mcpo-lo-home-");
                dir.toFile().deleteOnExit();
                SOFFICE_HOME = dir;
            }
            return SOFFICE_HOME;
        }
    }

    /**
     * Writes the whole slideshow to a temp pptx, invokes soffice with
     * {@code --convert-to <sofficeFormat>}, and moves the resulting file to
     * {@code outputFile}. Intended for single-file formats (pdf, html).
     */
    public void exportWholeDeck(XMLSlideShow show, Path outputFile, String sofficeFormat,
            String outputExtension) throws IOException {
        Path tempPptx = writeSlideshowToSandbox(show, "mcpo-export-");
        Path tempOutDir = null;
        try {
            tempOutDir = createSandboxSubdir("mcpo-export-out-");
            runSoffice(tempPptx, tempOutDir, sofficeFormat);
            Path produced = locateProducedFile(tempOutDir, tempPptx, outputExtension);
            pathResolver.createParentDirectories(outputFile);
            Files.move(produced, outputFile, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            cleanupQuietly(tempPptx);
            cleanupDirQuietly(tempOutDir);
        }
    }

    /**
     * Extracts a single slide from the source slideshow into a temporary
     * single-slide deck, renders it via soffice, and moves the result to
     * {@code outputFile}. Used by high-fidelity {@code render_slide}.
     */
    public void renderSingleSlide(XMLSlideShow show, int slideIndex, Path outputFile,
            String sofficeFormat, String outputExtension) throws IOException {
        Path tempPptx = writeSingleSlideDeckToSandbox(show, slideIndex, "mcpo-render-");
        Path tempOutDir = null;
        try {
            tempOutDir = createSandboxSubdir("mcpo-render-out-");
            runSoffice(tempPptx, tempOutDir, sofficeFormat);
            Path produced = locateProducedFile(tempOutDir, tempPptx, outputExtension);
            pathResolver.createParentDirectories(outputFile);
            Files.move(produced, outputFile, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            cleanupQuietly(tempPptx);
            cleanupDirQuietly(tempOutDir);
        }
    }

    /**
     * Renders every slide in {@code show} into {@code outputDir} using the
     * given soffice format. Each slide produces one output file named from
     * {@code fileNamePattern} (a {@link String#format} template taking the
     * 1-based slide index). Returns the absolute paths of the produced files
     * in slide order.
     */
    public List<Path> renderAllSlides(XMLSlideShow show, Path outputDir, String sofficeFormat,
            String outputExtension, String fileNamePattern) throws IOException {
        Files.createDirectories(outputDir);
        int slideCount = show.getSlides().size();
        List<Path> outputs = new ArrayList<>(slideCount);
        for (int i = 0; i < slideCount; i++) {
            String fileName = String.format(Locale.ROOT, fileNamePattern, i + 1);
            if (!fileName.toLowerCase(Locale.ROOT).endsWith("." + outputExtension)) {
                fileName = fileName + "." + outputExtension;
            }
            Path target = outputDir.resolve(fileName).toAbsolutePath().normalize();
            renderSingleSlide(show, i, target, sofficeFormat, outputExtension);
            outputs.add(target);
        }
        return outputs;
    }

    // ---------------- Internals ----------------

    private Path writeSlideshowToSandbox(XMLSlideShow show, String prefix) throws IOException {
        Path temp = pathResolver.createSandboxTempFile(prefix, ".pptx");
        try (FileOutputStream out = new FileOutputStream(temp.toFile())) {
            show.write(out);
        }
        return temp;
    }

    /**
     * Serializes the source slideshow and reloads a copy, then deletes every
     * slide except {@code slideIndex}. Keeps the original in-memory show
     * untouched.
     */
    private Path writeSingleSlideDeckToSandbox(XMLSlideShow source, int slideIndex, String prefix)
            throws IOException {
        int sourceCount = source.getSlides().size();
        if (slideIndex < 0 || slideIndex >= sourceCount) {
            throw new IOException("slide_index out of range for single-slide extract: "
                    + slideIndex + " (deck has " + sourceCount + " slide(s))");
        }
        byte[] bytes;
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            source.write(buf);
            bytes = buf.toByteArray();
        }
        Path temp = pathResolver.createSandboxTempFile(prefix, ".pptx");
        try (XMLSlideShow copy = new XMLSlideShow(new ByteArrayInputStream(bytes))) {
            int count = copy.getSlides().size();
            for (int i = count - 1; i >= 0; i--) {
                if (i != slideIndex) {
                    copy.removeSlide(i);
                }
            }
            try (FileOutputStream out = new FileOutputStream(temp.toFile())) {
                copy.write(out);
            }
        } catch (RuntimeException | IOException ex) {
            cleanupQuietly(temp);
            throw ex;
        }
        return temp;
    }

    private Path createSandboxSubdir(String prefix) throws IOException {
        Path marker = pathResolver.createSandboxTempFile(prefix, ".dir");
        Files.deleteIfExists(marker);
        Files.createDirectories(marker);
        return marker;
    }

    private void runSoffice(Path inputPptx, Path outputDir, String sofficeFormat)
            throws IOException {
        SofficeAvailability availability = SofficeAvailability.get();
        if (!availability.available()) {
            throw new SofficeUnavailableException(availability.error());
        }

        try {
            SOFFICE_SEMAPHORE.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("soffice semaphore acquisition interrupted", e);
        }

        try {
            Path sofficeHome = ensureSofficeHome();
            // -env:UserInstallation makes soffice materialize its per-user profile at a
            // known writable path. Setting HOME alone is not enough: when the container
            // runs with a uid that has no /etc/passwd entry (documented `--user 1000:1000`),
            // soffice's profile-discovery ignores HOME in some code paths and falls back to
            // "bootstrap failed" / exit 77. The file:/// URI is required by LibreOffice.
            String userInstallationUri = sofficeHome.toUri().toString();
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList(
                    config.sofficePath(),
                    "--headless",
                    "-env:UserInstallation=" + userInstallationUri,
                    "--convert-to",
                    sofficeFormat,
                    "--outdir",
                    outputDir.toString(),
                    inputPptx.toString()));
            pb.environment().put("HOME", sofficeHome.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder captured = new StringBuilder();
            Thread drain = new Thread(() -> drainOutput(process, captured), "soffice-output-drain");
            drain.setDaemon(true);
            drain.start();

            boolean finished;
            try {
                finished = process.waitFor(SOFFICE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
                throw new IOException("soffice conversion interrupted", e);
            }

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("soffice conversion timed out after "
                        + SOFFICE_TIMEOUT_SECONDS + "s (format=" + sofficeFormat + ")");
            }

            try {
                drain.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (process.exitValue() != 0) {
                String output;
                synchronized (captured) {
                    output = captured.toString().strip();
                }
                // Exit 77 is LibreOffice's "bootstrap failed" signal: the process never got
                // as far as rendering, usually because the user installation could not be
                // materialized (unwritable HOME, missing profile dir, etc.). Translate to
                // SofficeUnavailableException so the outer handler emits SOFFICE_UNAVAILABLE
                // rather than a raw TOOL_EXECUTION_ERROR with the exit code leaked into the
                // message. Other non-zero exits are genuine conversion failures and keep the
                // plain IOException path.
                if (process.exitValue() == 77) {
                    throw new SofficeUnavailableException(
                            "soffice bootstrap failed (exit 77) — LibreOffice user profile could not be initialized"
                            + (output.isEmpty() ? "" : ": " + output));
                }
                throw new IOException("soffice conversion failed with exit code "
                        + process.exitValue() + " (format=" + sofficeFormat + ")"
                        + (output.isEmpty() ? "" : ": " + output));
            }
        } finally {
            SOFFICE_SEMAPHORE.release();
        }
    }

    private void drainOutput(Process process, StringBuilder captured) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (captured) {
                    if (captured.length() < 8192) {
                        if (captured.length() > 0) {
                            captured.append('\n');
                        }
                        captured.append(line);
                    }
                }
            }
        } catch (IOException ignored) {
            // Process exit races with stream close; ignore.
        }
    }

    private Path locateProducedFile(Path outputDir, Path inputPptx, String expectedExtension)
            throws IOException {
        String base = inputPptx.getFileName().toString();
        int dot = base.lastIndexOf('.');
        String stem = dot > 0 ? base.substring(0, dot) : base;
        Path expected = outputDir.resolve(stem + "." + expectedExtension);
        if (Files.exists(expected)) {
            return expected;
        }
        // Fall back to the first file in outdir with the right extension.
        try (var stream = Files.list(outputDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)
                            .endsWith("." + expectedExtension))
                    .findFirst()
                    .orElseThrow(() -> new IOException(
                            "soffice produced no ." + expectedExtension + " file in " + outputDir));
        }
    }

    private void cleanupQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            LOG.debug("Could not delete temp file {}: {}", path, ex.toString());
        }
    }

    private void cleanupDirQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // Best-effort cleanup; leaving a few bytes behind is preferable to throwing.
                }
            });
        } catch (IOException ex) {
            LOG.debug("Could not clean temp dir {}: {}", dir, ex.toString());
        }
    }

    /**
     * Thrown when soffice is probed as unavailable at call time. Callers are
     * expected to catch and translate to {@code SOFFICE_UNAVAILABLE}.
     */
    public static final class SofficeUnavailableException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public SofficeUnavailableException(String detail) {
            super(detail == null || detail.isBlank()
                    ? "LibreOffice (soffice) is not available on this host"
                    : "LibreOffice (soffice) is not available on this host: " + detail);
        }
    }
}
