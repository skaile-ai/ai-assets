package ai.skaile.mcpo.ppt.tooling.infra;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.session.SessionStore;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

/**
 * Centralizes the repeated "resolve (document_id, slide_index, shape_index) → shape"
 * pattern from tool handlers. Each method throws a specific
 * {@link IllegalArgumentException} subtype so {@code PptToolService.call()} can
 * surface a structured error code:
 *
 * <ul>
 *   <li>{@link DocumentNotFoundException} → {@code DOCUMENT_NOT_FOUND}</li>
 *   <li>{@link SlideIndexOutOfRangeException} → {@code SLIDE_INDEX_OUT_OF_RANGE}</li>
 *   <li>{@link ShapeIndexOutOfRangeException} → {@code SHAPE_INDEX_OUT_OF_RANGE}</li>
 * </ul>
 *
 * <p>Handlers that want to emit their own error response without this
 * dispatcher translation can still use the {@code find*} variants that return
 * {@link Optional}.
 */
public final class PptShapeFinder {

    private final SessionStore store;

    public PptShapeFinder(SessionStore store) {
        this.store = store;
    }

    // ---------- Throwing variants ----------

    public PptDocumentSession requireSession(JsonNode args) {
        String id = args.path("document_id").asText("");
        return store.get(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }

    public XSLFSlide requireSlide(PptDocumentSession session, int slideIndex) {
        XMLSlideShow show = session.getSlideShow();
        List<XSLFSlide> slides = show.getSlides();
        if (slideIndex < 0 || slideIndex >= slides.size()) {
            throw new SlideIndexOutOfRangeException(slideIndex, slides.size());
        }
        return slides.get(slideIndex);
    }

    public XSLFSlide requireSlideFromArgs(PptDocumentSession session, JsonNode args) {
        return requireSlide(session, args.path("slide_index").asInt(-1));
    }

    public XSLFShape requireShape(XSLFSlide slide, int shapeIndex) {
        List<XSLFShape> shapes = slide.getShapes();
        if (shapeIndex < 0 || shapeIndex >= shapes.size()) {
            throw new ShapeIndexOutOfRangeException(shapeIndex, shapes.size());
        }
        return shapes.get(shapeIndex);
    }

    public XSLFShape requireShapeFromArgs(XSLFSlide slide, JsonNode args) {
        return requireShape(slide, args.path("shape_index").asInt(-1));
    }

    /** Convenience: resolve all three in one call. */
    public Resolved requireResolved(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = requireSlide(session, slideIndex);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFShape shape = requireShape(slide, shapeIndex);
        return new Resolved(session, slide, shape, slideIndex, shapeIndex);
    }

    // ---------- Optional variants ----------

    public Optional<PptDocumentSession> findSession(JsonNode args) {
        return store.get(args.path("document_id").asText(""));
    }

    public Optional<XSLFSlide> findSlide(PptDocumentSession session, int slideIndex) {
        if (session == null) {
            return Optional.empty();
        }
        List<XSLFSlide> slides = session.getSlideShow().getSlides();
        if (slideIndex < 0 || slideIndex >= slides.size()) {
            return Optional.empty();
        }
        return Optional.of(slides.get(slideIndex));
    }

    public Optional<XSLFShape> findShape(XSLFSlide slide, int shapeIndex) {
        if (slide == null) {
            return Optional.empty();
        }
        List<XSLFShape> shapes = slide.getShapes();
        if (shapeIndex < 0 || shapeIndex >= shapes.size()) {
            return Optional.empty();
        }
        return Optional.of(shapes.get(shapeIndex));
    }

    public record Resolved(
            PptDocumentSession session,
            XSLFSlide slide,
            XSLFShape shape,
            int slideIndex,
            int shapeIndex) {
    }

    // ---------- Exceptions ----------

    public static final class DocumentNotFoundException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        public DocumentNotFoundException(String documentId) {
            super("Unknown document_id: " + (documentId == null ? "" : documentId));
        }
    }

    public static final class SlideIndexOutOfRangeException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        public SlideIndexOutOfRangeException(int index, int size) {
            super("slide_index out of range: " + index + " (deck has " + size + " slide(s))");
        }
    }

    public static final class ShapeIndexOutOfRangeException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        public ShapeIndexOutOfRangeException(int index, int size) {
            super("shape_index out of range: " + index + " (slide has " + size + " shape(s))");
        }
    }
}
