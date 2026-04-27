package ai.skaile.mcpo.ppt.tooling.infra;

import java.nio.file.Path;

/**
 * Immutable configuration derived from environment variables at server startup.
 * This is the <strong>only</strong> place that reads {@code System.getenv} for
 * server-wide settings — every other component receives this value via
 * constructor injection.
 *
 * <p>Mutable per-instance state (the loaded default-template path, transaction
 * snapshots, session cache) is tracked separately.
 */
public record PptServerConfig(
        Path allowedRoot,
        Path templatesDir,
        Path defaultTemplateConfigPath,
        int maxOpenDocs,
        String sofficePath,
        String javaVersion) {

    public static final int DEFAULT_MAX_OPEN_DOCS = 100;
    public static final String DEFAULT_TEMPLATE_CONFIG_FILENAME = ".mcpo-ppt-default-template.json";
    public static final String DEFAULT_SOFFICE_PATH = "soffice";

    public static PptServerConfig fromEnvironment() {
        Path allowedRoot = parseAllowedRoot();
        return new PptServerConfig(
                allowedRoot,
                parseTemplatesDir(allowedRoot),
                parseDefaultTemplateConfigPath(allowedRoot),
                parseMaxOpenDocs(),
                parseSofficePath(),
                System.getProperty("java.version", "unknown"));
    }

    private static Path parseAllowedRoot() {
        String raw = System.getenv("MCPO_ALLOWED_ROOT");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Path.of(raw).toAbsolutePath().normalize();
    }

    private static Path parseTemplatesDir(Path allowedRoot) {
        String raw = System.getenv("MCPO_TEMPLATE_DIR");
        Path root;
        if (raw == null || raw.isBlank()) {
            root = Path.of(System.getProperty("user.home"), ".mcpo-ppt", "templates");
        } else {
            root = Path.of(raw);
        }
        Path normalized = root.toAbsolutePath().normalize();
        if (allowedRoot != null && !normalized.startsWith(allowedRoot)) {
            throw new IllegalArgumentException(
                    "Template directory is outside allowed root: " + normalized);
        }
        return normalized;
    }

    private static Path parseDefaultTemplateConfigPath(Path allowedRoot) {
        String raw = System.getenv("MCPO_DEFAULT_TEMPLATE_CONFIG");
        Path resolved;
        if (raw == null || raw.isBlank()) {
            if (allowedRoot != null) {
                resolved = allowedRoot.resolve(DEFAULT_TEMPLATE_CONFIG_FILENAME)
                        .toAbsolutePath().normalize();
            } else {
                resolved = Path.of(System.getProperty("user.home"), DEFAULT_TEMPLATE_CONFIG_FILENAME)
                        .toAbsolutePath().normalize();
            }
        } else {
            resolved = Path.of(raw).toAbsolutePath().normalize();
        }
        if (allowedRoot != null && !resolved.startsWith(allowedRoot)) {
            throw new IllegalArgumentException(
                    "Default template config path is outside allowed root: " + resolved);
        }
        return resolved;
    }

    private static int parseMaxOpenDocs() {
        String raw = System.getenv("MCPO_MAX_OPEN_DOCS");
        if (raw == null || raw.isBlank()) {
            return DEFAULT_MAX_OPEN_DOCS;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : DEFAULT_MAX_OPEN_DOCS;
        } catch (NumberFormatException ex) {
            return DEFAULT_MAX_OPEN_DOCS;
        }
    }

    private static String parseSofficePath() {
        String raw = System.getenv("SOFFICE_PATH");
        if (raw == null || raw.isBlank()) {
            return DEFAULT_SOFFICE_PATH;
        }
        return raw;
    }
}
