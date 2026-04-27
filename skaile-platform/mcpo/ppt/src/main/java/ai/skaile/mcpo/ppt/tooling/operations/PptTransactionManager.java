package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;

/**
 * In-memory per-session transaction snapshots. Begin serializes the current
 * {@link XMLSlideShow} into a byte buffer; rollback replaces the live slides
 * with the snapshot's contents and restores {@code dirty} / {@code sourcePath}.
 * Only one snapshot per document is retained — a second {@code begin()} without
 * a commit overwrites the earlier snapshot, matching the single-level
 * transaction contract documented in CLAUDE.md.
 *
 * <p>Snapshots are process-local; there is no persistence. The per-session
 * lock held by {@code PptToolService} ensures all public methods here observe
 * a single caller at a time for a given document.
 */
public final class PptTransactionManager {

    private final Map<String, Snapshot> snapshots = new HashMap<>();

    /** Begin or replace a transaction snapshot for {@code session}. */
    public void begin(PptDocumentSession session) throws IOException {
        byte[] data = serialize(session.getSlideShow());
        snapshots.put(
                session.getId(),
                new Snapshot(data, session.isDirty(), session.getSourcePath()));
    }

    /** Discard the snapshot without modifying the session. Idempotent. */
    public void commit(PptDocumentSession session) {
        snapshots.remove(session.getId());
    }

    /**
     * Restore the session's slide show from its active snapshot. Returns
     * {@code true} on success; {@code false} if no snapshot exists (handler
     * should surface this as an error).
     */
    public boolean rollback(PptDocumentSession session) throws IOException {
        Snapshot snapshot = snapshots.get(session.getId());
        if (snapshot == null) {
            return false;
        }
        try (XMLSlideShow rollbackShow = new XMLSlideShow(new ByteArrayInputStream(snapshot.data))) {
            restoreShow(session.getSlideShow(), rollbackShow);
        }
        session.setDirty(snapshot.dirty);
        session.setSourcePath(snapshot.sourcePath);
        snapshots.remove(session.getId());
        return true;
    }

    /** Drop any snapshot for the given document id. Used by session close. */
    public void clear(String documentId) {
        snapshots.remove(documentId);
    }

    private static byte[] serialize(XMLSlideShow show) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            show.write(out);
            return out.toByteArray();
        }
    }

    private static void restoreShow(XMLSlideShow target, XMLSlideShow source) {
        for (int i = target.getSlides().size() - 1; i >= 0; i--) {
            target.removeSlide(i);
        }
        target.setPageSize(source.getPageSize());
        for (XSLFSlide sourceSlide : source.getSlides()) {
            XSLFSlide newSlide = target.createSlide();
            newSlide.importContent(sourceSlide);
        }
    }

    private record Snapshot(byte[] data, boolean dirty, Path sourcePath) {
    }
}
