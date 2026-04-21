package ai.skaile.mcpo.ppt.tooling;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.session.SessionStore;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolDefinition;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.ColorParser;
import ai.skaile.mcpo.ppt.tooling.infra.PptLimits;
import ai.skaile.mcpo.ppt.tooling.infra.PptPathResolver;
import ai.skaile.mcpo.ppt.tooling.infra.PptServerConfig;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeFinder;
import ai.skaile.mcpo.ppt.tooling.infra.ToolArgumentValidator;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import ai.skaile.mcpo.ppt.tooling.operations.PptCapabilitiesOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptChartOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptDocumentOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptPageOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptRenderOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptShapeMutationOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptSlideOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptTableOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptTemplateOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptTextOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptTransactionManager;
import ai.skaile.mcpo.ppt.tooling.operations.SofficeRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatch facade for the PPT MCP tool surface. Holds no tool business logic —
 * every tool handler lives in an operations class under
 * {@code tooling.operations.*}. This class owns:
 *
 * <ul>
 *   <li>{@link #call(String, JsonNode)} — the tool-invocation entry point, argument
 *       validation, and typed-exception-to-error-code translation.</li>
 *   <li>{@link #invokeWithSessionLock(JsonNode, ToolHandler)} — POI DOM is not
 *       thread-safe, so every handler targeting an existing {@code document_id}
 *       runs under that session's lock.</li>
 *   <li>The tool registry — built at construction by merging every operations
 *       class's {@code handlers()} map.</li>
 *   <li>{@link #closeAllSessions()} — shutdown hook invoked by {@code McpServer}.</li>
 * </ul>
 *
 * <p>Adding a new tool is a two-file edit: declare the schema in
 * {@link PptToolDefinitions}, then add a handler method + a {@code handlers()}
 * map entry in the relevant operations class.
 */
public final class PptToolService {
    private static final Logger LOG = LoggerFactory.getLogger(PptToolService.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final SessionStore store = new SessionStore();
    private final PptServerConfig config;
    private final PptPathResolver pathResolver;
    private final ToolResponseFactory responseFactory;
    private final ToolArgumentValidator argumentValidator;
    private final PptShapeFinder shapeFinder;
    private final PptLimits limits;
    private final PptTransactionManager transactions = new PptTransactionManager();

    private final PptTemplateOperations templateOperations;
    private final PptDocumentOperations documentOperations;
    private final PptSlideOperations slideOperations;
    private final PptShapeMutationOperations shapeOperations;
    private final PptTableOperations tableOperations;
    private final PptTextOperations textOperations;
    private final PptPageOperations pageOperations;
    private final PptRenderOperations renderOperations;
    private final PptChartOperations chartOperations;
    private final PptCapabilitiesOperations capabilitiesOperations;

    private final List<ToolDefinition> tools;
    private final Map<String, ToolDefinition> toolsByName;
    private final Map<String, ToolHandler> toolHandlers;

    public PptToolService() {
        this(PptServerConfig.fromEnvironment());
    }

    public PptToolService(PptServerConfig config) {
        this.config = config;
        this.pathResolver = new PptPathResolver(config.allowedRoot());
        this.responseFactory = new ToolResponseFactory(mapper);
        this.argumentValidator = new ToolArgumentValidator(mapper);
        this.shapeFinder = new PptShapeFinder(store);
        this.limits = new PptLimits(responseFactory, shapeFinder);

        SofficeRenderer sofficeRenderer = new SofficeRenderer(pathResolver, config);

        this.templateOperations = new PptTemplateOperations(
                store, mapper, argumentValidator, responseFactory,
                pathResolver, shapeFinder, config, limits);
        this.documentOperations = new PptDocumentOperations(
                store, argumentValidator, responseFactory, pathResolver,
                shapeFinder, limits, transactions, templateOperations, sofficeRenderer, config);
        this.slideOperations = new PptSlideOperations(
                argumentValidator, responseFactory, shapeFinder, limits);
        this.shapeOperations = new PptShapeMutationOperations(
                argumentValidator, responseFactory, pathResolver, shapeFinder, limits);
        this.tableOperations = new PptTableOperations(
                mapper, argumentValidator, responseFactory, shapeFinder, limits);
        this.textOperations = new PptTextOperations(responseFactory, shapeFinder);
        this.pageOperations = new PptPageOperations(
                argumentValidator, responseFactory, shapeFinder);
        this.renderOperations = new PptRenderOperations(
                mapper, argumentValidator, responseFactory, pathResolver,
                shapeFinder, limits, sofficeRenderer);
        this.chartOperations = new PptChartOperations(mapper, responseFactory, shapeFinder);
        this.capabilitiesOperations = new PptCapabilitiesOperations(responseFactory, config);

        this.tools = PptToolDefinitions.create(mapper);
        this.toolsByName = new HashMap<>();
        for (ToolDefinition definition : tools) {
            toolsByName.put(definition.name(), definition);
        }
        this.toolHandlers = buildHandlerRegistry();
    }

    private Map<String, ToolHandler> buildHandlerRegistry() {
        Map<String, ToolHandler> handlers = new HashMap<>();
        handlers.putAll(documentOperations.handlers());
        handlers.putAll(slideOperations.handlers());
        handlers.putAll(shapeOperations.handlers());
        handlers.putAll(tableOperations.handlers());
        handlers.putAll(textOperations.handlers());
        handlers.putAll(pageOperations.handlers());
        handlers.putAll(renderOperations.handlers());
        handlers.putAll(templateOperations.handlers());
        handlers.putAll(chartOperations.handlers());
        handlers.putAll(capabilitiesOperations.handlers());
        return Map.copyOf(handlers);
    }

    public List<ToolDefinition> listTools() {
        return tools;
    }

    public int closeAllSessions() {
        int closed = store.closeAll();
        try {
            pathResolver.cleanSandboxTmpDir();
        } catch (IOException ex) {
            LOG.warn("Failed to clean sandbox tmp dir on shutdown: {}", ex.toString(), ex);
        }
        return closed;
    }

    public ToolCallResult call(String name, JsonNode arguments) {
        try {
            ToolDefinition definition = toolsByName.get(name);
            if (definition == null) {
                return responseFactory.error("Unknown tool: " + name);
            }
            JsonNode safeArguments = argumentValidator.normalizeArguments(arguments);
            argumentValidator.validateArguments(definition, safeArguments);
            ToolHandler handler = toolHandlers.get(name);
            if (handler == null) {
                return responseFactory.error("Unknown tool: " + name);
            }
            return invokeWithSessionLock(safeArguments, handler);
        } catch (PptPathResolver.PathNotAllowedException e) {
            return responseFactory.error("PATH_NOT_ALLOWED", e.getMessage(), false);
        } catch (ColorParser.InvalidColorException e) {
            return responseFactory.error("INVALID_COLOR", e.getMessage(), false);
        } catch (PptShapeFinder.DocumentNotFoundException e) {
            return responseFactory.error("DOCUMENT_NOT_FOUND", e.getMessage(), false);
        } catch (PptShapeFinder.SlideIndexOutOfRangeException e) {
            return responseFactory.error("SLIDE_INDEX_OUT_OF_RANGE", e.getMessage(), false);
        } catch (PptShapeFinder.ShapeIndexOutOfRangeException e) {
            return responseFactory.error("SHAPE_INDEX_OUT_OF_RANGE", e.getMessage(), false);
        } catch (IllegalArgumentException e) {
            return responseFactory.error("VALIDATION_ERROR", e.getMessage(), false);
        } catch (Exception e) {
            return responseFactory.error("TOOL_EXECUTION_ERROR",
                    "Tool execution failed: " + e.getMessage(), false);
        }
    }

    /**
     * POI's {@code XMLSlideShow} DOM is not thread-safe even for read-only traversal, so every
     * tool invocation that targets an existing session must serialize against that session's
     * lock. Tools without a {@code document_id} argument (document creation, templating, etc.)
     * bypass the lock — they either create a new session (already serialized inside the store)
     * or touch only process-wide template state.
     */
    private ToolCallResult invokeWithSessionLock(JsonNode arguments, ToolHandler handler)
            throws Exception {
        JsonNode idNode = arguments.path("document_id");
        if (!idNode.isTextual() || idNode.asText().isBlank()) {
            return handler.handle(arguments);
        }
        Optional<PptDocumentSession> maybe = store.get(idNode.asText());
        if (maybe.isEmpty()) {
            return handler.handle(arguments);
        }
        ReentrantLock lock = maybe.get().getLock();
        lock.lock();
        try {
            return handler.handle(arguments);
        } finally {
            lock.unlock();
        }
    }
}
