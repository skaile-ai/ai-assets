package ai.skaile.mcpo.ppt.tooling.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.apache.poi.sl.usermodel.PictureData;

public final class PptPathResolver {
    private final Path allowedRoot;

    public PptPathResolver(Path allowedRoot) {
        this.allowedRoot = allowedRoot;
    }

    public Path allowedRoot() {
        return allowedRoot;
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
