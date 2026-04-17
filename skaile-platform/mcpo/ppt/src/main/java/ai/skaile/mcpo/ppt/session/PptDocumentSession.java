package ai.skaile.mcpo.ppt.session;

import java.nio.file.Path;
import java.time.Instant;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

public final class PptDocumentSession {
    private final String id;
    private final XMLSlideShow slideShow;
    private final Instant openedAt;
    private Instant updatedAt;
    private boolean dirty;
    private Path sourcePath;

    public PptDocumentSession(String id, XMLSlideShow slideShow, Path sourcePath) {
        this.id = id;
        this.slideShow = slideShow;
        this.sourcePath = sourcePath;
        this.openedAt = Instant.now();
        this.updatedAt = this.openedAt;
        this.dirty = false;
    }

    public String getId() {
        return id;
    }

    public XMLSlideShow getSlideShow() {
        return slideShow;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch(boolean markDirty) {
        this.updatedAt = Instant.now();
        if (markDirty) {
            this.dirty = true;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        this.updatedAt = Instant.now();
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(Path sourcePath) {
        this.sourcePath = sourcePath;
        this.updatedAt = Instant.now();
    }
}
