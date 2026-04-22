package ai.skaile.mcpo.ppt.tooling.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.session.SessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Rectangle;
import java.util.Optional;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PptShapeFinderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SessionStore store;
    private PptShapeFinder finder;
    private PptDocumentSession session;
    private XSLFSlide slide;
    private XSLFShape shape;

    @BeforeEach
    void setUp() {
        store = new SessionStore();
        finder = new PptShapeFinder(store);
        session = store.create(new XMLSlideShow());
        slide = session.getSlideShow().createSlide();
        XSLFTextBox textBox = slide.createTextBox();
        textBox.setAnchor(new Rectangle(10, 20, 300, 100));
        textBox.setText("hello");
        shape = textBox;
    }

    @AfterEach
    void tearDown() {
        store.closeAll();
    }

    private ObjectNode args(String documentId, Integer slideIndex, Integer shapeIndex) {
        ObjectNode node = MAPPER.createObjectNode();
        if (documentId != null) {
            node.put("document_id", documentId);
        }
        if (slideIndex != null) {
            node.put("slide_index", slideIndex);
        }
        if (shapeIndex != null) {
            node.put("shape_index", shapeIndex);
        }
        return node;
    }

    @Test
    void requireSessionReturnsSessionWhenPresent() {
        assertSame(session, finder.requireSession(args(session.getId(), null, null)));
    }

    @Test
    void requireSessionThrowsDocumentNotFoundForUnknownId() {
        PptShapeFinder.DocumentNotFoundException ex = assertThrows(
                PptShapeFinder.DocumentNotFoundException.class,
                () -> finder.requireSession(args("doc_missing", null, null)));
        assertTrue(ex.getMessage().contains("doc_missing"));
    }

    @Test
    void requireSessionHandlesMissingDocumentIdField() {
        assertThrows(
                PptShapeFinder.DocumentNotFoundException.class,
                () -> finder.requireSession(MAPPER.createObjectNode()));
    }

    @Test
    void requireSlideReturnsSlideForValidIndex() {
        assertSame(slide, finder.requireSlide(session, 0));
    }

    @Test
    void requireSlideThrowsForNegativeIndex() {
        PptShapeFinder.SlideIndexOutOfRangeException ex = assertThrows(
                PptShapeFinder.SlideIndexOutOfRangeException.class,
                () -> finder.requireSlide(session, -1));
        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    void requireSlideThrowsForOutOfRangeIndex() {
        assertThrows(
                PptShapeFinder.SlideIndexOutOfRangeException.class,
                () -> finder.requireSlide(session, 5));
    }

    @Test
    void requireSlideFromArgsResolvesFromJson() {
        assertSame(slide, finder.requireSlideFromArgs(session, args(null, 0, null)));
    }

    @Test
    void requireSlideFromArgsMissingSlideIndexThrows() {
        assertThrows(
                PptShapeFinder.SlideIndexOutOfRangeException.class,
                () -> finder.requireSlideFromArgs(session, MAPPER.createObjectNode()));
    }

    @Test
    void requireShapeReturnsShapeForValidIndex() {
        assertSame(shape, finder.requireShape(slide, 0));
    }

    @Test
    void requireShapeThrowsForNegativeIndex() {
        PptShapeFinder.ShapeIndexOutOfRangeException ex = assertThrows(
                PptShapeFinder.ShapeIndexOutOfRangeException.class,
                () -> finder.requireShape(slide, -1));
        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    void requireShapeThrowsForOutOfRangeIndex() {
        assertThrows(
                PptShapeFinder.ShapeIndexOutOfRangeException.class,
                () -> finder.requireShape(slide, 99));
    }

    @Test
    void requireShapeFromArgsResolvesFromJson() {
        assertSame(shape, finder.requireShapeFromArgs(slide, args(null, null, 0)));
    }

    @Test
    void requireResolvedReturnsAllThree() {
        PptShapeFinder.Resolved resolved =
                finder.requireResolved(args(session.getId(), 0, 0));
        assertSame(session, resolved.session());
        assertSame(slide, resolved.slide());
        assertSame(shape, resolved.shape());
        assertEquals(0, resolved.slideIndex());
        assertEquals(0, resolved.shapeIndex());
    }

    @Test
    void requireResolvedPropagatesDocumentNotFound() {
        assertThrows(
                PptShapeFinder.DocumentNotFoundException.class,
                () -> finder.requireResolved(args("doc_missing", 0, 0)));
    }

    @Test
    void requireResolvedPropagatesSlideOutOfRange() {
        assertThrows(
                PptShapeFinder.SlideIndexOutOfRangeException.class,
                () -> finder.requireResolved(args(session.getId(), 9, 0)));
    }

    @Test
    void requireResolvedPropagatesShapeOutOfRange() {
        assertThrows(
                PptShapeFinder.ShapeIndexOutOfRangeException.class,
                () -> finder.requireResolved(args(session.getId(), 0, 9)));
    }

    @Test
    void findSessionReturnsPresentOptionalForKnownId() {
        Optional<PptDocumentSession> found =
                finder.findSession(args(session.getId(), null, null));
        assertTrue(found.isPresent());
        assertSame(session, found.get());
    }

    @Test
    void findSessionReturnsEmptyForUnknownId() {
        assertTrue(finder.findSession(args("doc_missing", null, null)).isEmpty());
    }

    @Test
    void findSlideReturnsEmptyForNullSession() {
        assertTrue(finder.findSlide(null, 0).isEmpty());
    }

    @Test
    void findSlideReturnsEmptyForOutOfRangeIndex() {
        assertTrue(finder.findSlide(session, -1).isEmpty());
        assertTrue(finder.findSlide(session, 42).isEmpty());
    }

    @Test
    void findSlideReturnsSlideWhenPresent() {
        Optional<XSLFSlide> found = finder.findSlide(session, 0);
        assertTrue(found.isPresent());
        assertSame(slide, found.get());
    }

    @Test
    void findShapeReturnsEmptyForNullSlide() {
        assertTrue(finder.findShape(null, 0).isEmpty());
    }

    @Test
    void findShapeReturnsEmptyForOutOfRangeIndex() {
        assertTrue(finder.findShape(slide, -1).isEmpty());
        assertTrue(finder.findShape(slide, 42).isEmpty());
    }

    @Test
    void findShapeReturnsShapeWhenPresent() {
        Optional<XSLFShape> found = finder.findShape(slide, 0);
        assertTrue(found.isPresent());
        assertSame(shape, found.get());
    }

    @Test
    void exceptionsAreIllegalArgumentExceptions() {
        assertTrue(new PptShapeFinder.DocumentNotFoundException("x")
                instanceof IllegalArgumentException);
        assertTrue(new PptShapeFinder.SlideIndexOutOfRangeException(0, 0)
                instanceof IllegalArgumentException);
        assertTrue(new PptShapeFinder.ShapeIndexOutOfRangeException(0, 0)
                instanceof IllegalArgumentException);
    }

    @Test
    void documentNotFoundExceptionHandlesNullId() {
        PptShapeFinder.DocumentNotFoundException ex =
                new PptShapeFinder.DocumentNotFoundException(null);
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().contains("null"));
    }
}
