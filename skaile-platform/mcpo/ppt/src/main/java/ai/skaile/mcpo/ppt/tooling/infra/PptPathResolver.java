package ai.skaile.mcpo.ppt.tooling.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.apache.poi.sl.usermodel.PictureData;

public final class PptPathResolver {
    /** Subdirectory under the allowed root used for short-lived temp files. */
    public static final String SANDBOX_TMP_SUBDIR = ".mcpo-ppt/tmp";

    private final Path allowedRoot;

    public PptPathResolver(Path allowedRoot) {
        this.allowedRoot = allowedRoot;
    }

    public Path allowedRoot() {
        return allowedRoot;
    }

    /**
     * Creates a temporary file under {@code ${MCPO_ALLOWED_ROOT}/.mcpo-ppt/tmp/} when a sandbox
     * root is configured, falling back to the system temp directory when it is not. Using the
     * sandbox keeps exporter workspaces (e.g. LibreOffice PDF staging) inside the mount that the
     * server is authorized to read and write.
     */
    public Path createSandboxTempFile(String prefix, String suffix) throws IOException {
        if (allowedRoot == null) {
            return Files.createTempFile(prefix, suffix);
        }
        Path tmpDir = allowedRoot.resolve(SANDBOX_TMP_SUBDIR);
        Files.createDirectories(tmpDir);
        return Files.createTempFile(tmpDir, prefix, suffix);
    }

    /** Removes the sandbox temp directory and everything under it. Best-effort. */
    public void cleanSandboxTmpDir() throws IOException {
        if (allowedRoot == null) {
            return;
        }
        Path tmpDir = allowedRoot.resolve(SANDBOX_TMP_SUBDIR);
        if (!Files.exists(tmpDir)) {
            return;
        }
        try (var stream = Files.walk(tmpDir)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // Leave behind rather than blocking shutdown.
                }
            });
        }
    }

    public Path resolvePath(String rawPath, boolean forWrite) {
        Path path = Path.of(rawPath).toAbsolutePath().normalize();
        if (allowedRoot != null && !path.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("Path is outside allowed root: " + path);
        }
        if (!forWrite && !Files.exists(path)) {
            throw new IllegalArgumentException("Path does not exist: " + path);
        }
        return path;
    }

    public void createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    public String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public String inferImageFormat(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "jpg";
        }
        return "png";
    }

    public PictureData.PictureType inferPictureType(Path imagePath) {
        String extension = extensionOf(imagePath.getFileName().toString());
        return switch (extension) {
            case "png" -> PictureData.PictureType.PNG;
            case "jpg", "jpeg" -> PictureData.PictureType.JPEG;
            case "gif" -> PictureData.PictureType.GIF;
            case "bmp" -> PictureData.PictureType.BMP;
            case "tif", "tiff" -> PictureData.PictureType.TIFF;
            case "wmf" -> PictureData.PictureType.WMF;
            case "emf" -> PictureData.PictureType.EMF;
            default -> throw new IllegalArgumentException("Unsupported image format: " + extension);
        };
    }

    public String sanitizeTemplateName(String templateName, String extension) {
        String normalized = templateName.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("template_name cannot be blank");
        }
        if (normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("template_name must be a file name, not a path");
        }

        Path fileNamePath = Path.of(normalized).getFileName();
        if (fileNamePath == null) {
            throw new IllegalArgumentException("Invalid template_name");
        }

        String fileName = fileNamePath.toString();
        if (!fileName.equals(normalized)) {
            throw new IllegalArgumentException("template_name must be a file name, not a path");
        }
        if (".".equals(fileName) || "..".equals(fileName)) {
            throw new IllegalArgumentException("Invalid template_name");
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith("." + extension)) {
            fileName = fileName + "." + extension;
        }
        return fileName;
    }
}
