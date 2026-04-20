package ai.skaile.mcpo.ppt.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;

class SessionStoreTest {

    @Test
    void createAssignsUniqueIdAndStoresSession() throws Exception {
        SessionStore store = new SessionStore();
        PptDocumentSession a;
        PptDocumentSession b;
        try (XMLSlideShow show1 = new XMLSlideShow(); XMLSlideShow show2 = new XMLSlideShow()) {
            a = store.create(show1);
            b = store.create(show2);

            assertNotNull(a.getLock());
            assertNotSame(a.getLock(), b.getLock());
            assertNotSame(a.getId(), b.getId());
            assertTrue(a.getId().startsWith("doc_"));
            assertEquals(2, store.size());

            Optional<PptDocumentSession> fetched = store.get(a.getId());
            assertTrue(fetched.isPresent());
            assertSame(a, fetched.get());
        } finally {
            store.closeAll();
        }
    }

    @Test
    void closeRemovesSessionAndReturnsFalseForUnknown() throws Exception {
        SessionStore store = new SessionStore();
        try (XMLSlideShow show = new XMLSlideShow()) {
            PptDocumentSession session = store.create(show);
            assertTrue(store.close(session.getId()));
            assertFalse(store.close(session.getId()));
            assertFalse(store.close("doc_nonexistent"));
            assertEquals(0, store.size());
            assertTrue(store.get(session.getId()).isEmpty());
        }
    }

    @Test
    void closeAllClosesEveryOpenSession() {
        SessionStore store = new SessionStore();
        store.create(new XMLSlideShow());
        store.create(new XMLSlideShow());
        store.create(new XMLSlideShow());

        assertEquals(3, store.size());
        assertEquals(3, store.closeAll());
        assertEquals(0, store.size());
        // Calling closeAll on an already-empty store is a no-op.
        assertEquals(0, store.closeAll());
    }

    @Test
    void putOverwritesSameIdAndIsRecoverable() throws Exception {
        SessionStore store = new SessionStore();
        try (XMLSlideShow show = new XMLSlideShow()) {
            PptDocumentSession original = store.create(show);
            PptDocumentSession replacement =
                    new PptDocumentSession(original.getId(), show, null);
            store.put(replacement);
            assertSame(replacement, store.get(original.getId()).orElseThrow());
        } finally {
            store.closeAll();
        }
    }
}
