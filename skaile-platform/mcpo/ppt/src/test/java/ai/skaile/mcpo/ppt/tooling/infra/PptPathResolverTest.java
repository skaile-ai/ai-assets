package ai.skaile.mcpo.ppt.tooling.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.sl.usermodel.PictureData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PptPathResolverTest {

    @Test
    void createSandboxTempFileRoutesUnderAllowedRoot(@TempDir Path root) throws IOException {
        PptPathResolver resolver = new PptPathResolver(root);
        Path tmp = resolver.createSandboxTempFile("unit-", ".pptx");
        try {
            assertTrue(tmp.startsWith(root.resolve(PptPathResolver.SANDBOX_TMP_SUBDIR)));
            assertTrue(Files.exists(tmp));
            assertTrue(tmp.getFileName().toString().endsWith(".pptx"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void cleanSandboxTmpDirRemovesStagedFiles(@TempDir Path root) throws IOException {
        PptPathResolver resolver = new PptPathResolver(root);
        Path tmp = resolver.createSandboxTempFile("unit-", ".bin");
        assertTrue(Files.exists(tmp));
        resolver.cleanSandboxTmpDir();
        assertFalse(Files.exists(tmp));
        assertFalse(Files.exists(root.resolve(PptPathResolver.SANDBOX_TMP_SUBDIR)));
    }

    @Test
    void cleanSandboxTmpDirIsNoOpWhenNoAllowedRoot() throws IOException {
        PptPathResolver resolver = new PptPathResolver(null);
        // Falls back to the system temp dir; cleanSandboxTmpDir must not throw.
        Path tmp = resolver.createSandboxTempFile("unit-", ".bin");
        assertTrue(Files.exists(tmp));
        resolver.cleanSandboxTmpDir();
        // The system-temp fallback is not managed by cleanSandboxTmpDir.
        assertTrue(Files.exists(tmp));
        Files.deleteIfExists(tmp);
    }

    @Test
    void resolvePathRejectsPathsOutsideAllowedRoot(@TempDir Path root) {
        PptPathResolver resolver = new PptPathResolver(root);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolvePath("/etc/hosts", false));
        assertTrue(ex.getMessage().contains("outside allowed root"));
    }

    @Test
    void resolvePathRejectsMissingFileInReadMode(@TempDir Path root) {
        PptPathResolver resolver = new PptPathResolver(root);
        Path missing = root.resolve("missing.pptx");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolvePath(missing.toString(), false));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void inferPictureTypeRecognizesCommonExtensions() {
        PptPathResolver resolver = new PptPathResolver(null);
        assertEquals(PictureData.PictureType.PNG,
                resolver.inferPictureType(Path.of("x.png")));
        assertEquals(PictureData.PictureType.JPEG,
                resolver.inferPictureType(Path.of("x.jpg")));
        assertEquals(PictureData.PictureType.GIF,
                resolver.inferPictureType(Path.of("x.gif")));
        assertEquals("jpg", resolver.inferImageFormat(Path.of("slide.jpeg")));
        assertEquals("png", resolver.inferImageFormat(Path.of("slide.bin")));
    }

    @Test
    void sanitizeTemplateNameRejectsPathsAndAppendsExtension() {
        PptPathResolver resolver = new PptPathResolver(null);
        assertEquals("plain.pptx", resolver.sanitizeTemplateName("plain", "pptx"));
        assertEquals("already.pptx", resolver.sanitizeTemplateName("already.pptx", "pptx"));
        assertThrows(IllegalArgumentException.class,
                () -> resolver.sanitizeTemplateName("  ", "pptx"));
        assertThrows(IllegalArgumentException.class,
                () -> resolver.sanitizeTemplateName("nested/name", "pptx"));
        assertThrows(IllegalArgumentException.class,
                () -> resolver.sanitizeTemplateName("..", "pptx"));
    }
}
