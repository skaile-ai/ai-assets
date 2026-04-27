package ai.skaile.mcpo.ppt.tooling.infra;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.apache.poi.xslf.usermodel.XSLFSlide;

/**
 * Central safety limits enforced across operations classes. Each {@code enforce*} method
 * returns {@code null} when within budget and a structured error {@link ToolCallResult}
 * (built via {@link ToolResponseFactory}) when the limit has been reached. Limit constants
 * stay {@code public static final} so {@code ppt.capabilities} can surface them verbatim.
 */
public final class PptLimits {

    public static final int MAX_SLIDES = 2000;
    public static final int MAX_SHAPES_PER_SLIDE = 500;
    public static final long MAX_IMAGE_BYTES = 50L * 1024 * 1024;
    public static final int MAX_RENDER_DIMENSION = 10_000;

    private final ToolResponseFactory responseFactory;
    private final PptShapeFinder shapeFinder;

    public PptLimits(ToolResponseFactory responseFactory, PptShapeFinder shapeFinder) {
        this.responseFactory = responseFactory;
        this.shapeFinder = shapeFinder;
    }

    public ToolCallResult enforceSlideLimit(PptDocumentSession session) {
        int current = session.getSlideShow().getSlides().size();
        if (current >= MAX_SLIDES) {
            return responseFactory.error("LIMIT_MAX_SLIDES",
                    "Slide count limit reached (" + MAX_SLIDES + ")", false);
        }
        return null;
    }

    public ToolCallResult enforceShapeLimit(JsonNode args) {
        Optional<PptDocumentSession> session = shapeFinder.findSession(args);
        if (session.isEmpty()) {
            return null;
        }
        Optional<XSLFSlide> slide = shapeFinder.findSlide(
                session.get(), args.path("slide_index").asInt(-1));
        if (slide.isEmpty()) {
            return null;
        }
        return enforceShapeLimitForSlide(slide.get());
    }

    public ToolCallResult enforceShapeLimitForSlide(XSLFSlide slide) {
        if (slide.getShapes().size() >= MAX_SHAPES_PER_SLIDE) {
            return responseFactory.error("LIMIT_MAX_SHAPES",
                    "Slide has reached the shape limit (" + MAX_SHAPES_PER_SLIDE + ")",
                    false);
        }
        return null;
    }

    public ToolCallResult enforceImageBytesLimit(long imageBytes) {
        if (imageBytes > MAX_IMAGE_BYTES) {
            return responseFactory.error("LIMIT_MAX_IMAGE_BYTES",
                    "Image exceeds the " + MAX_IMAGE_BYTES + "-byte limit: " + imageBytes,
                    false);
        }
        return null;
    }

    public ToolCallResult enforceRenderDimensionLimit(int width, int height) {
        if (width > MAX_RENDER_DIMENSION || height > MAX_RENDER_DIMENSION) {
            return responseFactory.error("LIMIT_MAX_RENDER_DIMENSION",
                    "Render dimensions exceed " + MAX_RENDER_DIMENSION + "px: "
                            + width + "x" + height,
                    false);
        }
        return null;
    }
}
