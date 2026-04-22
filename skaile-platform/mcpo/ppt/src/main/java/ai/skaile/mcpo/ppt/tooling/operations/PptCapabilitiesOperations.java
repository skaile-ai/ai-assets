package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.PptLimits;
import ai.skaile.mcpo.ppt.tooling.infra.PptServerConfig;
import ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code ppt.capabilities} self-describe handler. Reports server/POI/java
 * versions, LibreOffice availability, the installed-font list, feature flags,
 * and the safety-limit constants from {@link PptLimits}. Isolated into its own
 * class so {@code PptToolService} stays focused on dispatch.
 */
public final class PptCapabilitiesOperations {
    private static final Logger LOG = LoggerFactory.getLogger(PptCapabilitiesOperations.class);
    private static final String SERVER_VERSION = "0.1.0";

    private final ToolResponseFactory responseFactory;
    private final PptServerConfig config;

    private static volatile List<String> cachedFontFamilies;

    public PptCapabilitiesOperations(ToolResponseFactory responseFactory, PptServerConfig config) {
        this.responseFactory = responseFactory;
        this.config = config;
    }

    public Map<String, ToolHandler> handlers() {
        return Map.of("ppt.capabilities", this::capabilities);
    }

    ToolCallResult capabilities(JsonNode args) {
        SofficeAvailability soffice = SofficeAvailability.get();
        ObjectNode payload = responseFactory.okPayload();
        payload.put("server_version", SERVER_VERSION);
        payload.put("poi_version", resolvePoiVersion());
        payload.put("soffice_available", soffice.available());
        if (soffice.available() && soffice.version() != null && !soffice.version().isBlank()) {
            payload.put("soffice_version", soffice.version());
        }
        payload.put("java_version", config.javaVersion());
        if (config.allowedRoot() != null) {
            payload.put("allowed_root", config.allowedRoot().toString());
        }

        ArrayNode inputFormats = payload.putArray("supported_input_formats");
        inputFormats.add("pptx");
        inputFormats.add("pptm");

        ArrayNode exportFormats = payload.putArray("supported_export_formats");
        for (String fmt : new String[] {
                "pptx", "pdf", "html", "png_batch", "jpg_batch", "svg_batch", "outline_text"}) {
            exportFormats.add(fmt);
        }

        ArrayNode renderFormats = payload.putArray("supported_render_formats");
        renderFormats.add("png");
        renderFormats.add("jpg");
        renderFormats.add("svg");

        ArrayNode fonts = payload.putArray("installed_fonts");
        for (String family : getInstalledFontFamilies()) {
            fonts.add(family);
        }

        ObjectNode flags = payload.putObject("feature_flags");
        flags.put("charts_update", true);
        // high_fidelity_render is wired end-to-end; the flag reports whether the host can
        // actually serve it — soffice may be missing on dev machines.
        flags.put("high_fidelity_render", soffice.available());
        flags.put("gradients", true);
        flags.put("picture_effects", true);
        flags.put("table_borders", true);
        flags.put("table_merge", true);

        ObjectNode limitsNode = payload.putObject("limits");
        limitsNode.put("max_open_docs", config.maxOpenDocs());
        limitsNode.put("max_slides_per_deck", PptLimits.MAX_SLIDES);
        limitsNode.put("max_shapes_per_slide", PptLimits.MAX_SHAPES_PER_SLIDE);
        limitsNode.put("max_image_bytes", PptLimits.MAX_IMAGE_BYTES);
        limitsNode.put("max_render_dimension", PptLimits.MAX_RENDER_DIMENSION);

        return responseFactory.success(payload);
    }

    private static String resolvePoiVersion() {
        String v = org.apache.poi.Version.getVersion();
        return v == null || v.isBlank() ? "unknown" : v;
    }

    private List<String> getInstalledFontFamilies() {
        List<String> snapshot = cachedFontFamilies;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (PptCapabilitiesOperations.class) {
            if (cachedFontFamilies != null) {
                return cachedFontFamilies;
            }
            cachedFontFamilies = probeFontFamilies();
            return cachedFontFamilies;
        }
    }

    private List<String> probeFontFamilies() {
        // fc-list is the lowest-dependency way to learn what fonts the runtime has available.
        // On hosts without fontconfig we fall back to the JDK's local graphics environment.
        try {
            Process process = new ProcessBuilder("fc-list", ":family")
                    .redirectErrorStream(true)
                    .start();
            List<String> families = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && families.size() < 50) {
                    String first = line.split(",", 2)[0].strip();
                    if (!first.isBlank() && !families.contains(first)) {
                        families.add(first);
                    }
                }
            }
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            } else if (process.exitValue() == 0 && !families.isEmpty()) {
                return List.copyOf(families);
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.debug("fc-list probe failed, falling back to AWT: {}", ex.toString());
        }
        try {
            String[] awt = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();
            List<String> families = new ArrayList<>();
            for (int i = 0; i < awt.length && families.size() < 50; i++) {
                families.add(awt[i]);
            }
            return List.copyOf(families);
        } catch (Exception ex) {
            return List.of();
        }
    }
}
