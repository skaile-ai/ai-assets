package ai.skaile.mcpo.ppt.session;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SessionStore {
    private static final Logger LOG = LoggerFactory.getLogger(SessionStore.class);
    private final Map<String, PptDocumentSession> sessions = new ConcurrentHashMap<>();

    public PptDocumentSession create(XMLSlideShow show) {
        String id = "doc_" + UUID.randomUUID();
        PptDocumentSession session = new PptDocumentSession(id, show, null);
        sessions.put(id, session);
        return session;
    }

    public void put(PptDocumentSession session) {
        sessions.put(session.getId(), session);
    }

    public Optional<PptDocumentSession> get(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public boolean close(String id) throws IOException {
        PptDocumentSession removed = sessions.remove(id);
        if (removed == null) {
            return false;
        }
        removed.getSlideShow().close();
        return true;
    }

    public int size() {
        return sessions.size();
    }

    public int closeAll() {
        int closed = 0;
        int failed = 0;
        for (String id : List.copyOf(sessions.keySet())) {
            try {
                if (close(id)) {
                    closed++;
                }
            } catch (IOException ex) {
                failed++;
                LOG.warn("Failed to close session {} during shutdown: {}", id, ex.toString(), ex);
            }
        }
        if (failed > 0) {
            LOG.warn("closeAll() completed with {} closed and {} failures", closed, failed);
        }
        return closed;
    }
}
