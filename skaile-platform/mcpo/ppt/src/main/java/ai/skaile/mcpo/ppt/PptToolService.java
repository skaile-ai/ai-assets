package ai.skaile.mcpo.ppt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public final class PptToolService {
    private static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final int DEFAULT_MAX_OPEN_DOCS = 100;
    private static final long SOFFICE_TIMEOUT_SECONDS = 90;
    private static final String DEFAULT_TEMPLATE_CONFIG = ".mcpo-ppt-default-template.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private final SessionStore store = new SessionStore();
    private final List<ToolDefinition> tools;
    private final Map<String, ToolDefinition> toolsByName;
    private final int maxOpenDocs;
    private final Path allowedRoot;
    private final Path templatesDir;
    private final Path defaultTemplateConfigPath;
    private Path defaultTemplatePath;

    public PptToolService() {
        this.maxOpenDocs = parseMaxOpenDocs();
        this.allowedRoot = parseAllowedRoot();
        this.templatesDir = parseTemplatesDir();
        this.defaultTemplateConfigPath = parseDefaultTemplateConfigPath();
        this.defaultTemplatePath = loadDefaultTemplatePath();
        tools = List.of(
                tool("ppt.create_document", "Create a new in-memory PowerPoint document and return a document handle.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                                                        "title": {"type": "string"},
                                                                        "template_path": {"type": "string"}
                                  },
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.open_document", "Open an existing PPTX file into memory and return a document handle.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "path": {"type": "string"}
                                  },
                                  "required": ["path"],
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.close_document", "Close an open document handle and release memory.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"}
                                  },
                                  "required": ["document_id"],
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.get_document_info", "Get high-level metadata for an open document.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"}
                                  },
                                  "required": ["document_id"],
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.list_slides", "List all slides and quick text previews.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"}
                                  },
                                  "required": ["document_id"],
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.get_slide_content", "Get textual content and shape metadata for one slide.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"},
                                    "slide_index": {"type": "integer", "minimum": 0}
                                  },
                                  "required": ["document_id", "slide_index"],
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.add_slide", "Add a slide to a document and optionally initialize title text.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"},
                                    "title": {"type": "string"}
                                  },
                                  "required": ["document_id"],
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.update_text", "Replace text in a specific slide.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"},
                                    "slide_index": {"type": "integer", "minimum": 0},
                                    "old_text": {"type": "string"},
                                    "new_text": {"type": "string"},
                                    "occurrence": {"type": "integer", "minimum": 1, "default": 1}
                                  },
                                  "required": ["document_id", "slide_index", "old_text", "new_text"],
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.add_textbox", "Add a text box to a slide at a specific position.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"},
                                    "slide_index": {"type": "integer", "minimum": 0},
                                    "text": {"type": "string"},
                                    "x": {"type": "number"},
                                    "y": {"type": "number"},
                                    "width": {"type": "number"},
                                    "height": {"type": "number"},
                                    "font_size": {"type": "number"}
                                  },
                                  "required": ["document_id", "slide_index", "text", "x", "y", "width", "height"],
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.insert_image", "Insert an image into a slide at a specific position.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "image_path": {"type": "string"},
                                        "x": {"type": "number"},
                                        "y": {"type": "number"},
                                        "width": {"type": "number"},
                                        "height": {"type": "number"}
                                    },
                                    "required": ["document_id", "slide_index", "image_path", "x", "y", "width", "height"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.get_slide_notes", "Get speaker notes text for one slide.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0}
                                    },
                                    "required": ["document_id", "slide_index"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.set_slide_notes", "Set speaker notes text for one slide.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "notes_text": {"type": "string"}
                                    },
                                    "required": ["document_id", "slide_index", "notes_text"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.add_table", "Add a table to a slide.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "rows": {"type": "integer", "minimum": 1},
                                        "cols": {"type": "integer", "minimum": 1},
                                        "x": {"type": "number"},
                                        "y": {"type": "number"},
                                        "width": {"type": "number"},
                                        "height": {"type": "number"}
                                    },
                                    "required": ["document_id", "slide_index", "rows", "cols", "x", "y", "width", "height"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.get_table_cell", "Get text from a table cell.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "row_index": {"type": "integer", "minimum": 0},
                                        "col_index": {"type": "integer", "minimum": 0}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index", "row_index", "col_index"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.set_table_cell", "Set text in a table cell.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "row_index": {"type": "integer", "minimum": 0},
                                        "col_index": {"type": "integer", "minimum": 0},
                                        "text": {"type": "string"}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index", "row_index", "col_index", "text"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.set_text_style", "Apply style updates to text in a shape.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "bold": {"type": "boolean"},
                                        "italic": {"type": "boolean"},
                                        "underline": {"type": "boolean"},
                                        "font_size": {"type": "number", "minimum": 1},
                                        "font_color": {"type": "string"}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.save_document", "Write an open in-memory document to disk.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"},
                                    "output_path": {"type": "string"},
                                                                        "format": {"type": "string", "enum": ["pptx", "pdf"], "default": "pptx"}
                                  },
                                  "required": ["document_id"],
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.render_slide_image", "Render a slide to an image file (PNG/JPG).",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"},
                                    "slide_index": {"type": "integer", "minimum": 0},
                                    "output_path": {"type": "string"},
                                    "width": {"type": "integer", "minimum": 1},
                                    "height": {"type": "integer", "minimum": 1}
                                  },
                                  "required": ["document_id", "slide_index", "output_path"],
                                  "additionalProperties": false
                                }
                                """),
                tool("ppt.render_slide_svg", "Render a slide to an SVG file.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"},
                                    "slide_index": {"type": "integer", "minimum": 0},
                                    "output_path": {"type": "string"},
                                    "width": {"type": "integer", "minimum": 1},
                                    "height": {"type": "integer", "minimum": 1}
                                  },
                                  "required": ["document_id", "slide_index", "output_path"],
                                  "additionalProperties": false
                                }
                                                                """),
                tool("ppt.find_text", "Find text occurrences across all slides in an open presentation.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "query": {"type": "string"},
                                        "case_sensitive": {"type": "boolean", "default": false}
                                    },
                                    "required": ["document_id", "query"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.render_selection_image", "Render selected shapes from a slide to an image file (PNG/JPG).",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_indices": {
                                            "type": "array",
                                            "items": {"type": "integer", "minimum": 0},
                                            "minItems": 1
                                        },
                                        "output_path": {"type": "string"},
                                        "width": {"type": "integer", "minimum": 1},
                                        "height": {"type": "integer", "minimum": 1}
                                    },
                                    "required": ["document_id", "slide_index", "shape_indices", "output_path"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.render_selection_svg", "Render selected shapes from a slide to an SVG file.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_indices": {
                                            "type": "array",
                                            "items": {"type": "integer", "minimum": 0},
                                            "minItems": 1
                                        },
                                        "output_path": {"type": "string"},
                                        "width": {"type": "integer", "minimum": 1},
                                        "height": {"type": "integer", "minimum": 1}
                                    },
                                    "required": ["document_id", "slide_index", "shape_indices", "output_path"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.upload_template",
                        "Upload/copy a PPT template to the server template store and optionally set as default.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "source_path": {"type": "string"},
                                        "template_name": {"type": "string"},
                                        "make_default": {"type": "boolean", "default": true}
                                    },
                                    "required": ["source_path"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.set_default_template",
                        "Set the default PPT template path used when create/generate is called without template_path.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "template_path": {"type": "string"}
                                    },
                                    "required": ["template_path"],
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.get_default_template", "Return the currently configured default template.",
                        """
                                {
                                    "type": "object",
                                    "properties": {},
                                    "additionalProperties": false
                                }
                                """),
                tool("ppt.generate_presentation",
                        "Generate a new presentation from optional slide titles and template (or default template).",
                        """
                                                                {
                                                                    "type": "object",
                                                                    "properties": {
                                                                        "title": {"type": "string"},
                                                                        "slide_titles": {
                                                                            "type": "array",
                                                                            "items": {"type": "string"}
                                                                        },
                                                                        "template_path": {"type": "string"},
                                                                        "output_path": {"type": "string"}
                                                                    },
                                                                    "additionalProperties": false
                                                                }
                                """));
        this.toolsByName = new HashMap<>();
        for (ToolDefinition definition : tools) {
            toolsByName.put(definition.name(), definition);
        }
    }

    public List<ToolDefinition> listTools() {
        return tools;
    }

    public int closeAllSessions() {
        return store.closeAll();
    }

    public ToolCallResult call(String name, JsonNode arguments) {
        try {
            ToolDefinition definition = toolsByName.get(name);
            if (definition == null) {
                return error("Unknown tool: " + name);
            }
            JsonNode safeArguments = normalizeArguments(arguments);
            validateArguments(definition, safeArguments);

            return switch (name) {
                case "ppt.create_document" -> createDocument(safeArguments);
                case "ppt.open_document" -> openDocument(safeArguments);
                case "ppt.close_document" -> closeDocument(safeArguments);
                case "ppt.get_document_info" -> getDocumentInfo(safeArguments);
                case "ppt.list_slides" -> listSlides(safeArguments);
                case "ppt.get_slide_content" -> getSlideContent(safeArguments);
                case "ppt.add_slide" -> addSlide(safeArguments);
                case "ppt.update_text" -> updateText(safeArguments);
                case "ppt.add_textbox" -> addTextBox(safeArguments);
                case "ppt.insert_image" -> insertImage(safeArguments);
                case "ppt.get_slide_notes" -> getSlideNotes(safeArguments);
                case "ppt.set_slide_notes" -> setSlideNotes(safeArguments);
                case "ppt.add_table" -> addTable(safeArguments);
                case "ppt.get_table_cell" -> getTableCell(safeArguments);
                case "ppt.set_table_cell" -> setTableCell(safeArguments);
                case "ppt.set_text_style" -> setTextStyle(safeArguments);
                case "ppt.save_document" -> saveDocument(safeArguments);
                case "ppt.render_slide_image" -> renderSlideImage(safeArguments);
                case "ppt.render_slide_svg" -> renderSlideSvg(safeArguments);
                case "ppt.find_text" -> findText(safeArguments);
                case "ppt.render_selection_image" -> renderSelectionImage(safeArguments);
                case "ppt.render_selection_svg" -> renderSelectionSvg(safeArguments);
                case "ppt.upload_template" -> uploadTemplate(safeArguments);
                case "ppt.set_default_template" -> setDefaultTemplate(safeArguments);
                case "ppt.get_default_template" -> getDefaultTemplate(safeArguments);
                case "ppt.generate_presentation" -> generatePresentation(safeArguments);
                default -> error("Unknown tool: " + name);
            };
        } catch (IllegalArgumentException e) {
            return error("VALIDATION_ERROR", e.getMessage(), false);
        } catch (Exception e) {
            return error("TOOL_EXECUTION_ERROR", "Tool execution failed: " + e.getMessage(), false);
        }
    }

    private ToolCallResult createDocument(JsonNode args) {
        if (store.size() >= maxOpenDocs) {
            return error("Open document limit reached (" + maxOpenDocs + ")");
        }

        String title = args.path("title").asText("");
        XMLSlideShow show = loadTemplateOrBlank(args.path("template_path").asText(""));
        XSLFSlide slide = ensureFirstSlide(show);
        if (!title.isBlank()) {
            setSlideTitle(slide, title);
        }

        PptDocumentSession session = store.create(show);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", show.getSlides().size());
        payload.put("template_path", currentTemplatePathAsString(args.path("template_path").asText("")));
        payload.put("message", "Created new presentation in memory");
        return success(payload);
    }

    private ToolCallResult openDocument(JsonNode args) throws IOException {
        if (store.size() >= maxOpenDocs) {
            return error("Open document limit reached (" + maxOpenDocs + ")");
        }

        String pathRaw = requiredString(args, "path");
        Path path = resolvePath(pathRaw, false);
        if (!Files.exists(path)) {
            return error("File does not exist: " + path);
        }

        XMLSlideShow show;
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            show = new XMLSlideShow(in);
        }

        PptDocumentSession session = store.create(show);
        session.setSourcePath(path);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("source_path", path.toString());
        payload.put("slide_count", show.getSlides().size());
        payload.put("message", "Opened presentation in memory");
        return success(payload);
    }

    private ToolCallResult closeDocument(JsonNode args) throws IOException {
        String id = requiredString(args, "document_id");
        boolean closed = store.close(id);
        if (!closed) {
            return error("Unknown document_id: " + id);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", id);
        payload.put("message", "Document closed");
        return success(payload);
    }

    private ToolCallResult getDocumentInfo(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        XMLSlideShow show = session.getSlideShow();
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", show.getSlides().size());
        payload.put("dirty", session.isDirty());
        payload.put("opened_at", session.getOpenedAt().toString());
        payload.put("updated_at", session.getUpdatedAt().toString());
        payload.put("source_path", session.getSourcePath() == null ? "" : session.getSourcePath().toString());
        Dimension pageSize = show.getPageSize();
        payload.put("page_width", pageSize.width);
        payload.put("page_height", pageSize.height);
        return success(payload);
    }

    private ToolCallResult listSlides(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        ArrayNode slides = payload.putArray("slides");

        List<XSLFSlide> allSlides = session.getSlideShow().getSlides();
        for (int i = 0; i < allSlides.size(); i++) {
            XSLFSlide slide = allSlides.get(i);
            ObjectNode entry = slides.addObject();
            entry.put("slide_index", i);
            String text = collectSlideText(slide);
            entry.put("text_preview", text.length() > 240 ? text.substring(0, 240) + "..." : text);
            entry.put("shape_count", slide.getShapes().size());
        }

        return success(payload);
    }

    private ToolCallResult getSlideContent(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("text", collectSlideText(slide));

        ArrayNode shapes = payload.putArray("shapes");
        int i = 0;
        for (XSLFShape shape : slide.getShapes()) {
            ObjectNode shapeNode = shapes.addObject();
            shapeNode.put("shape_index", i++);
            shapeNode.put("shape_type", shape.getClass().getSimpleName());
            if (shape instanceof XSLFTextShape textShape) {
                shapeNode.put("text", textShape.getText());
            }
            if (shape instanceof XSLFTable table) {
                shapeNode.put("rows", table.getNumberOfRows());
                shapeNode.put("cols", table.getNumberOfColumns());
            }
        }

        return success(payload);
    }

    private ToolCallResult addSlide(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        String title = args.path("title").asText("");
        XSLFSlide slide = createDefaultSlide(session.getSlideShow());
        if (!title.isBlank()) {
            XSLFTextShape titleShape = slide.createTextBox();
            titleShape.setAnchor(new Rectangle2D.Double(40, 30, 840, 80));
            titleShape.setText(title);
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", session.getSlideShow().getSlides().size() - 1);
        payload.put("slide_count", session.getSlideShow().getSlides().size());
        return success(payload);
    }

    private ToolCallResult updateText(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        String oldText = requiredString(args, "old_text");
        String newText = requiredString(args, "new_text");
        int occurrence = args.path("occurrence").asInt(1);
        if (occurrence < 1) {
            return error("occurrence must be >= 1");
        }

        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        int seen = 0;
        for (XSLFShape shape : slide.getShapes()) {
            if (!(shape instanceof XSLFTextShape textShape)) {
                continue;
            }
            String current = textShape.getText();
            if (current == null || current.isEmpty()) {
                continue;
            }
            int idx = current.indexOf(oldText);
            while (idx >= 0) {
                seen++;
                if (seen == occurrence) {
                    String replaced = current.substring(0, idx) + newText + current.substring(idx + oldText.length());
                    textShape.clearText();
                    textShape.setText(replaced);
                    session.touch(true);

                    ObjectNode payload = okPayload();
                    payload.put("document_id", session.getId());
                    payload.put("slide_index", slideIndex);
                    payload.put("message", "Text updated");
                    payload.put("replaced_occurrence", occurrence);
                    return success(payload);
                }
                idx = current.indexOf(oldText, idx + oldText.length());
            }
        }

        return error("Could not find the requested text occurrence");
    }

    private ToolCallResult addTextBox(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        String text = requiredString(args, "text");
        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (!isValidRect(x, y, width, height)) {
            return error("x, y, width, height must be valid positive numbers");
        }

        XSLFTextShape box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(x, y, width, height));
        var paragraph = box.addNewTextParagraph();
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text);
        if (args.has("font_size")) {
            run.setFontSize(args.path("font_size").asDouble());
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("message", "Text box added");
        return success(payload);
    }

    private ToolCallResult insertImage(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        Path imagePath = resolvePath(requiredString(args, "image_path"), false);
        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (!isValidRect(x, y, width, height)) {
            return error("x, y, width, height must be valid positive numbers");
        }

        PictureData.PictureType pictureType = inferPictureType(imagePath);
        byte[] imageBytes = Files.readAllBytes(imagePath);
        XSLFPictureData pictureData = session.getSlideShow().addPicture(imageBytes, pictureType);
        XSLFPictureShape picture = slide.createPicture(pictureData);
        picture.setAnchor(new Rectangle2D.Double(x, y, width, height));

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", slide.getShapes().size() - 1);
        payload.put("image_path", imagePath.toString());
        payload.put("message", "Image inserted");
        return success(payload);
    }

    private ToolCallResult getSlideNotes(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        XSLFNotes notes = slide.getNotes();
        String notesText = notes == null ? "" : collectSlideText(notes);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("notes_text", notesText);
        return success(payload);
    }

    private ToolCallResult setSlideNotes(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        String notesText = requiredString(args, "notes_text");
        XSLFNotes notes = slide.getNotes();
        if (notes == null) {
            return error("Slide does not have a notes section");
        }

        XSLFTextShape targetShape = null;
        for (XSLFShape shape : notes.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                targetShape = textShape;
                break;
            }
        }
        if (targetShape == null) {
            targetShape = notes.createTextBox();
            targetShape.setAnchor(new Rectangle2D.Double(30, 30, 900, 120));
        }
        targetShape.clearText();
        targetShape.setText(notesText);

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("notes_text", notesText);
        payload.put("message", "Slide notes updated");
        return success(payload);
    }

    private ToolCallResult addTable(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        int rows = args.path("rows").asInt(0);
        int cols = args.path("cols").asInt(0);
        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (rows < 1 || cols < 1) {
            return error("rows and cols must be >= 1");
        }
        if (!isValidRect(x, y, width, height)) {
            return error("x, y, width, height must be valid positive numbers");
        }

        XSLFTable table = slide.createTable();
        table.setAnchor(new Rectangle2D.Double(x, y, width, height));
        double rowHeight = height / rows;
        double colWidth = width / cols;
        for (int r = 0; r < rows; r++) {
            XSLFTableRow row = table.addRow();
            row.setHeight(rowHeight);
            for (int c = 0; c < cols; c++) {
                XSLFTableCell cell = row.addCell();
                cell.setText("");
                table.setColumnWidth(c, colWidth);
            }
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", slide.getShapes().size() - 1);
        payload.put("rows", rows);
        payload.put("cols", cols);
        payload.put("message", "Table added");
        return success(payload);
    }

    private ToolCallResult getTableCell(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        int rowIndex = args.path("row_index").asInt(-1);
        int colIndex = args.path("col_index").asInt(-1);

        XSLFTable table = resolveTableShape(session.getSlideShow(), slideIndex, shapeIndex);
        if (table == null) {
            return error("shape_index does not point to a table on the selected slide");
        }

        XSLFTableCell cell = getTableCell(table, rowIndex, colIndex);
        if (cell == null) {
            return error("Invalid table cell coordinates");
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("row_index", rowIndex);
        payload.put("col_index", colIndex);
        payload.put("text", cell.getText());
        return success(payload);
    }

    private ToolCallResult setTableCell(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        int rowIndex = args.path("row_index").asInt(-1);
        int colIndex = args.path("col_index").asInt(-1);
        String text = requiredString(args, "text");

        XSLFTable table = resolveTableShape(session.getSlideShow(), slideIndex, shapeIndex);
        if (table == null) {
            return error("shape_index does not point to a table on the selected slide");
        }

        XSLFTableCell cell = getTableCell(table, rowIndex, colIndex);
        if (cell == null) {
            return error("Invalid table cell coordinates");
        }

        cell.setText(text);
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("row_index", rowIndex);
        payload.put("col_index", colIndex);
        payload.put("text", text);
        payload.put("message", "Table cell updated");
        return success(payload);
    }

    private ToolCallResult setTextStyle(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape shape = slide.getShapes().get(shapeIndex);
        if (!(shape instanceof XSLFTextShape textShape)) {
            return error("shape_index does not point to a text-capable shape");
        }

        List<XSLFTextRun> runs = collectTextRuns(textShape);
        if (runs.isEmpty()) {
            var paragraph = textShape.addNewTextParagraph();
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText("");
            runs.add(run);
        }

        Double fontSize = args.has("font_size") ? args.path("font_size").asDouble() : null;
        if (fontSize != null && fontSize <= 0) {
            return error("font_size must be > 0");
        }
        Color fontColor = args.has("font_color") ? parseColorHex(args.path("font_color").asText("")) : null;

        for (XSLFTextRun run : runs) {
            if (args.has("bold")) {
                run.setBold(args.path("bold").asBoolean());
            }
            if (args.has("italic")) {
                run.setItalic(args.path("italic").asBoolean());
            }
            if (args.has("underline")) {
                run.setUnderlined(args.path("underline").asBoolean());
            }
            if (fontSize != null) {
                run.setFontSize(fontSize);
            }
            if (fontColor != null) {
                run.setFontColor(fontColor);
            }
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("message", "Text style updated");
        return success(payload);
    }

    private ToolCallResult saveDocument(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        String format = args.path("format").asText("pptx").toLowerCase(Locale.ROOT);
        if (!"pptx".equals(format) && !"pdf".equals(format)) {
            return error("format must be one of: pptx, pdf");
        }

        Path outputPath;
        if (args.has("output_path") && !args.path("output_path").asText().isBlank()) {
            outputPath = resolvePath(args.path("output_path").asText(), true);
        } else if (session.getSourcePath() != null) {
            outputPath = session.getSourcePath();
        } else {
            return error("output_path is required for unsaved documents");
        }

        createParentDirectories(outputPath);

        if ("pptx".equals(format)) {
            try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
                session.getSlideShow().write(out);
            }
            session.setSourcePath(outputPath);
            session.setDirty(false);
        } else {
            Path tempPptx = Files.createTempFile("mcpo-export-", ".pptx");
            try {
                try (FileOutputStream out = new FileOutputStream(tempPptx.toFile())) {
                    session.getSlideShow().write(out);
                }
                exportPdfWithSoffice(tempPptx, outputPath);
            } finally {
                Files.deleteIfExists(tempPptx);
            }
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("output_path", outputPath.toString());
        payload.put("format", format);
        payload.put("message", "Document saved");
        return success(payload);
    }

    private ToolCallResult renderSlideImage(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        Path outputPath = resolvePath(requiredString(args, "output_path"), true);
        createParentDirectories(outputPath);

        Dimension pageSize = session.getSlideShow().getPageSize();
        int width = args.path("width").asInt(pageSize.width);
        int height = args.path("height").asInt(pageSize.height);
        if (width < 1 || height < 1) {
            return error("width and height must be >= 1");
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        double scaleX = width / (double) pageSize.width;
        double scaleY = height / (double) pageSize.height;
        graphics.scale(scaleX, scaleY);
        slide.draw(graphics);
        graphics.dispose();

        String format = inferImageFormat(outputPath);
        javax.imageio.ImageIO.write(image, format, outputPath.toFile());

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("output_path", outputPath.toString());
        payload.put("format", format);
        payload.put("message", "Slide rendered as image");
        return success(payload);
    }

    private ToolCallResult renderSlideSvg(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        Path outputPath = resolvePath(requiredString(args, "output_path"), true);
        createParentDirectories(outputPath);

        Dimension pageSize = session.getSlideShow().getPageSize();
        int width = args.path("width").asInt(pageSize.width);
        int height = args.path("height").asInt(pageSize.height);
        if (width < 1 || height < 1) {
            return error("width and height must be >= 1");
        }

        DOMImplementation domImplementation = GenericDOMImplementation.getDOMImplementation();
        Document document = domImplementation.createDocument(SVG_NS, "svg", null);
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        svgGenerator.setSVGCanvasSize(new Dimension(width, height));

        double scaleX = width / (double) pageSize.width;
        double scaleY = height / (double) pageSize.height;
        svgGenerator.scale(scaleX, scaleY);
        slide.draw(svgGenerator);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputPath.toFile()),
                StandardCharsets.UTF_8)) {
            svgGenerator.stream(writer, true);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("output_path", outputPath.toString());
        payload.put("format", "svg");
        payload.put("message", "Slide rendered as SVG");
        return success(payload);
    }

    private ToolCallResult findText(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        String query = requiredString(args, "query");
        boolean caseSensitive = args.path("case_sensitive").asBoolean(false);
        String normalizedQuery = caseSensitive ? query : query.toLowerCase(Locale.ROOT);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("query", query);
        ArrayNode matches = payload.putArray("matches");

        List<XSLFSlide> slides = session.getSlideShow().getSlides();
        for (int slideIndex = 0; slideIndex < slides.size(); slideIndex++) {
            XSLFSlide slide = slides.get(slideIndex);
            List<XSLFShape> shapes = slide.getShapes();
            for (int shapeIndex = 0; shapeIndex < shapes.size(); shapeIndex++) {
                XSLFShape shape = shapes.get(shapeIndex);
                if (!(shape instanceof XSLFTextShape textShape)) {
                    continue;
                }
                String text = textShape.getText();
                if (text == null || text.isBlank()) {
                    continue;
                }

                String haystack = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
                int from = 0;
                while (true) {
                    int idx = haystack.indexOf(normalizedQuery, from);
                    if (idx < 0) {
                        break;
                    }
                    ObjectNode match = matches.addObject();
                    match.put("slide_index", slideIndex);
                    match.put("shape_index", shapeIndex);
                    match.put("start", idx);
                    match.put("end", idx + query.length());
                    match.put("text", text);
                    from = idx + Math.max(1, query.length());
                }
            }
        }

        payload.put("count", matches.size());
        return success(payload);
    }

    private ToolCallResult renderSelectionImage(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        List<Integer> shapeIndices = parseShapeIndices(args.path("shape_indices"));
        if (shapeIndices.isEmpty()) {
            return error("shape_indices must contain at least one index");
        }

        Rectangle2D.Double selection = selectionBounds(slide, shapeIndices);
        if (selection == null) {
            return error("Unable to calculate selection bounds from provided shape_indices");
        }

        Path outputPath = resolvePath(requiredString(args, "output_path"), true);
        createParentDirectories(outputPath);

        int width = args.path("width").asInt((int) Math.ceil(selection.width));
        int height = args.path("height").asInt((int) Math.ceil(selection.height));
        if (width < 1 || height < 1) {
            return error("width and height must be >= 1");
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.scale(width / selection.width, height / selection.height);
        graphics.translate(-selection.x, -selection.y);
        slide.draw(graphics);
        graphics.dispose();

        String format = inferImageFormat(outputPath);
        javax.imageio.ImageIO.write(image, format, outputPath.toFile());

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("output_path", outputPath.toString());
        payload.put("format", format);
        payload.put("message", "Selection rendered as image");
        return success(payload);
    }

    private ToolCallResult renderSelectionSvg(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        List<Integer> shapeIndices = parseShapeIndices(args.path("shape_indices"));
        if (shapeIndices.isEmpty()) {
            return error("shape_indices must contain at least one index");
        }

        Rectangle2D.Double selection = selectionBounds(slide, shapeIndices);
        if (selection == null) {
            return error("Unable to calculate selection bounds from provided shape_indices");
        }

        Path outputPath = resolvePath(requiredString(args, "output_path"), true);
        createParentDirectories(outputPath);

        int width = args.path("width").asInt((int) Math.ceil(selection.width));
        int height = args.path("height").asInt((int) Math.ceil(selection.height));
        if (width < 1 || height < 1) {
            return error("width and height must be >= 1");
        }

        DOMImplementation domImplementation = GenericDOMImplementation.getDOMImplementation();
        Document document = domImplementation.createDocument(SVG_NS, "svg", null);
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        svgGenerator.setSVGCanvasSize(new Dimension(width, height));
        svgGenerator.scale(width / selection.width, height / selection.height);
        svgGenerator.translate(-selection.x, -selection.y);
        slide.draw(svgGenerator);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputPath.toFile()),
                StandardCharsets.UTF_8)) {
            svgGenerator.stream(writer, true);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("output_path", outputPath.toString());
        payload.put("format", "svg");
        payload.put("message", "Selection rendered as SVG");
        return success(payload);
    }

    private ToolCallResult uploadTemplate(JsonNode args) throws IOException {
        Path source = resolvePath(requiredString(args, "source_path"), false);
        String extension = extensionOf(source.getFileName().toString());
        if (!("pptx".equals(extension) || "potx".equals(extension))) {
            return error("Template must be a .pptx or .potx file");
        }

        Files.createDirectories(templatesDir);
        String preferredName = args.path("template_name").asText("").strip();
        if (preferredName.isBlank()) {
            preferredName = source.getFileName().toString();
        }
        String safeName = sanitizeTemplateName(preferredName, extension);

        Path target = templatesDir.resolve(safeName).toAbsolutePath().normalize();
        if (!target.startsWith(templatesDir)) {
            return error("Template target path escapes template directory");
        }
        if (allowedRoot != null && !target.startsWith(allowedRoot)) {
            return error("Template target path is outside allowed root: " + target);
        }

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        boolean makeDefault = args.path("make_default").asBoolean(true);
        if (makeDefault) {
            setDefaultTemplateInternal(target);
        }

        ObjectNode payload = okPayload();
        payload.put("template_path", target.toString());
        payload.put("default_template_path", defaultTemplatePath == null ? "" : defaultTemplatePath.toString());
        payload.put("message", makeDefault ? "Template uploaded and set as default" : "Template uploaded");
        return success(payload);
    }

    private ToolCallResult setDefaultTemplate(JsonNode args) throws IOException {
        Path templatePath = resolvePath(requiredString(args, "template_path"), false);
        String extension = extensionOf(templatePath.getFileName().toString());
        if (!("pptx".equals(extension) || "potx".equals(extension))) {
            return error("Default template must be a .pptx or .potx file");
        }

        setDefaultTemplateInternal(templatePath);

        ObjectNode payload = okPayload();
        payload.put("default_template_path", defaultTemplatePath.toString());
        payload.put("message", "Default template updated");
        return success(payload);
    }

    private ToolCallResult getDefaultTemplate(JsonNode args) {
        ObjectNode payload = okPayload();
        payload.put("default_template_path", defaultTemplatePath == null ? "" : defaultTemplatePath.toString());
        payload.put("has_default_template", defaultTemplatePath != null);
        return success(payload);
    }

    private ToolCallResult generatePresentation(JsonNode args) throws IOException {
        if (store.size() >= maxOpenDocs) {
            return error("Open document limit reached (" + maxOpenDocs + ")");
        }

        String templateArg = args.path("template_path").asText("");
        XMLSlideShow show = loadTemplateOrBlank(templateArg);
        XSLFSlide firstSlide = ensureFirstSlide(show);

        String title = args.path("title").asText("").strip();
        if (!title.isBlank()) {
            setSlideTitle(firstSlide, title);
        }

        List<String> slideTitles = parseSlideTitles(args.path("slide_titles"));
        if (!slideTitles.isEmpty()) {
            setSlideTitle(firstSlide, slideTitles.get(0));
            for (int i = 1; i < slideTitles.size(); i++) {
                XSLFSlide slide = createDefaultSlide(show);
                setSlideTitle(slide, slideTitles.get(i));
            }
        }

        PptDocumentSession session = store.create(show);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", show.getSlides().size());
        payload.put("template_path", currentTemplatePathAsString(templateArg));

        if (args.has("output_path") && !args.path("output_path").asText("").isBlank()) {
            Path outputPath = resolvePath(args.path("output_path").asText(), true);
            createParentDirectories(outputPath);
            try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
                show.write(out);
            }
            session.setSourcePath(outputPath);
            session.setDirty(false);
            payload.put("output_path", outputPath.toString());
        }

        payload.put("message", "Presentation generated");
        return success(payload);
    }

    private PptDocumentSession requireSession(JsonNode args) {
        String id = requiredString(args, "document_id");
        Optional<PptDocumentSession> maybe = store.get(id);
        return maybe.orElse(null);
    }

    private XSLFSlide getSlideByIndex(XMLSlideShow show, int index) {
        if (index < 0 || index >= show.getSlides().size()) {
            return null;
        }
        return show.getSlides().get(index);
    }

    private String requiredString(JsonNode args, String key) {
        String value = args.path(key).asText("");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        return value;
    }

    private JsonNode normalizeArguments(JsonNode arguments) {
        if (arguments == null || arguments.isNull() || arguments.isMissingNode()) {
            return mapper.createObjectNode();
        }
        return arguments;
    }

    private void validateArguments(ToolDefinition definition, JsonNode arguments) {
        JsonNode schema = definition.inputSchema();
        if (!arguments.isObject()) {
            throw new IllegalArgumentException("arguments must be a JSON object");
        }

        JsonNode required = schema.path("required");
        if (required.isArray()) {
            for (JsonNode requiredKey : required) {
                String key = requiredKey.asText();
                if (!arguments.has(key) || arguments.path(key).isNull()) {
                    throw new IllegalArgumentException("Missing required argument: " + key);
                }
            }
        }

        JsonNode properties = schema.path("properties");
        boolean allowAdditional = schema.path("additionalProperties").asBoolean(true);

        Iterator<String> fieldNames = arguments.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            if (!allowAdditional && !properties.has(field)) {
                throw new IllegalArgumentException("Unknown argument: " + field);
            }
            if (properties.has(field)) {
                validateProperty(field, arguments.path(field), properties.path(field));
            }
        }
    }

    private void validateProperty(String fieldName, JsonNode value, JsonNode propertySchema) {
        if (value == null || value.isNull()) {
            return;
        }

        String type = propertySchema.path("type").asText("");
        if (!type.isBlank()) {
            boolean typeValid = switch (type) {
                case "string" -> value.isTextual();
                case "integer" -> value.isIntegralNumber();
                case "number" -> value.isNumber();
                case "boolean" -> value.isBoolean();
                case "array" -> value.isArray();
                case "object" -> value.isObject();
                default -> true;
            };
            if (!typeValid) {
                throw new IllegalArgumentException("Invalid type for argument " + fieldName + ": expected " + type);
            }
        }

        JsonNode enumNode = propertySchema.path("enum");
        if (enumNode.isArray() && enumNode.size() > 0) {
            boolean matched = false;
            for (JsonNode enumValue : enumNode) {
                if (enumValue.equals(value)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new IllegalArgumentException("Invalid value for argument " + fieldName + ": " + value);
            }
        }

        if (value.isNumber() && propertySchema.has("minimum")) {
            double minimum = propertySchema.path("minimum").asDouble();
            if (value.asDouble() < minimum) {
                throw new IllegalArgumentException(
                        "Invalid value for argument " + fieldName + ": must be >= " + minimum);
            }
        }

        if (value.isArray()) {
            int minItems = propertySchema.path("minItems").asInt(-1);
            if (minItems >= 0 && value.size() < minItems) {
                throw new IllegalArgumentException(
                        "Invalid value for argument " + fieldName + ": must contain at least " + minItems + " items");
            }
            JsonNode itemSchema = propertySchema.path("items");
            for (JsonNode item : value) {
                validateProperty(fieldName + "[]", item, itemSchema);
            }
        }
    }

    private XSLFTable resolveTableShape(XMLSlideShow show, int slideIndex, int shapeIndex) {
        XSLFSlide slide = getSlideByIndex(show, slideIndex);
        if (slide == null) {
            return null;
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return null;
        }
        XSLFShape shape = slide.getShapes().get(shapeIndex);
        if (shape instanceof XSLFTable table) {
            return table;
        }
        return null;
    }

    private XSLFTableCell getTableCell(XSLFTable table, int rowIndex, int colIndex) {
        if (rowIndex < 0 || rowIndex >= table.getNumberOfRows()) {
            return null;
        }
        XSLFTableRow row = table.getRows().get(rowIndex);
        if (colIndex < 0 || colIndex >= row.getCells().size()) {
            return null;
        }
        return row.getCells().get(colIndex);
    }

    private List<XSLFTextRun> collectTextRuns(XSLFTextShape textShape) {
        List<XSLFTextRun> runs = new ArrayList<>();
        textShape.getTextParagraphs().forEach(paragraph -> runs.addAll(paragraph.getTextRuns()));
        return runs;
    }

    private Color parseColorHex(String input) {
        String value = input == null ? "" : input.strip();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            throw new IllegalArgumentException("font_color must be in #RRGGBB format");
        }
        try {
            int rgb = Integer.parseInt(value, 16);
            return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("font_color must be in #RRGGBB format", ex);
        }
    }

    private ObjectNode okPayload() {
        ObjectNode node = mapper.createObjectNode();
        node.put("status", "success");
        return node;
    }

    private ToolCallResult success(ObjectNode payload) {
        return new ToolCallResult(true, payload);
    }

    private ToolCallResult error(String message) {
        return error("TOOL_ERROR", message, false);
    }

    private ToolCallResult error(String code, String message, boolean retriable) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("status", "error");
        payload.put("code", code);
        payload.put("error", message);
        payload.put("retriable", retriable);
        return new ToolCallResult(false, payload);
    }

    private String inferImageFormat(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "jpg";
        }
        return "png";
    }

    private PictureData.PictureType inferPictureType(Path imagePath) {
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

    private Path resolvePath(String rawPath, boolean forWrite) {
        Path path = Path.of(rawPath).toAbsolutePath().normalize();
        if (allowedRoot != null && !path.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("Path is outside allowed root: " + path);
        }
        if (!forWrite && !Files.exists(path)) {
            throw new IllegalArgumentException("Path does not exist: " + path);
        }
        return path;
    }

    private void createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private XMLSlideShow loadTemplateOrBlank(String requestedTemplatePath) {
        String candidate = requestedTemplatePath == null ? "" : requestedTemplatePath.strip();
        Path templateToUse = null;

        if (!candidate.isBlank()) {
            templateToUse = resolvePath(candidate, false);
        } else if (defaultTemplatePath != null) {
            templateToUse = defaultTemplatePath;
        }

        if (templateToUse == null) {
            return new XMLSlideShow();
        }

        try (FileInputStream in = new FileInputStream(templateToUse.toFile())) {
            return new XMLSlideShow(in);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to load template: " + templateToUse + " (" + e.getMessage() + ")", e);
        }
    }

    private XSLFSlide ensureFirstSlide(XMLSlideShow show) {
        if (show.getSlides().isEmpty()) {
            return createDefaultSlide(show);
        }
        return show.getSlides().get(0);
    }

    private void setSlideTitle(XSLFSlide slide, String title) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String existing = textShape.getText();
                if (existing != null && !existing.isBlank()) {
                    textShape.clearText();
                    textShape.setText(title);
                    return;
                }
            }
        }

        XSLFTextShape titleShape = slide.createTextBox();
        titleShape.setAnchor(new Rectangle2D.Double(40, 30, 840, 80));
        titleShape.setText(title);
    }

    private List<String> parseSlideTitles(JsonNode node) {
        List<String> titles = new ArrayList<>();
        if (!node.isArray()) {
            return titles;
        }
        for (JsonNode item : node) {
            String value = item.asText("").strip();
            if (!value.isBlank()) {
                titles.add(value);
            }
        }
        return titles;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String currentTemplatePathAsString(String requestedTemplatePath) {
        String candidate = requestedTemplatePath == null ? "" : requestedTemplatePath.strip();
        if (!candidate.isBlank()) {
            return resolvePath(candidate, false).toString();
        }
        return defaultTemplatePath == null ? "" : defaultTemplatePath.toString();
    }

    private Path parseTemplatesDir() {
        String raw = System.getenv("MCPO_TEMPLATE_DIR");
        Path root;
        if (raw == null || raw.isBlank()) {
            root = Path.of(System.getProperty("user.home"), ".mcpo-ppt", "templates");
        } else {
            root = Path.of(raw);
        }
        Path normalized = root.toAbsolutePath().normalize();
        if (allowedRoot != null && !normalized.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("Template directory is outside allowed root: " + normalized);
        }
        return normalized;
    }

    private Path parseDefaultTemplateConfigPath() {
        String raw = System.getenv("MCPO_DEFAULT_TEMPLATE_CONFIG");
        Path resolved;
        if (raw == null || raw.isBlank()) {
            if (allowedRoot != null) {
                resolved = allowedRoot.resolve(DEFAULT_TEMPLATE_CONFIG).toAbsolutePath().normalize();
            } else {
                resolved = Path.of(System.getProperty("user.home"), DEFAULT_TEMPLATE_CONFIG).toAbsolutePath()
                        .normalize();
            }
        } else {
            resolved = Path.of(raw).toAbsolutePath().normalize();
        }
        if (allowedRoot != null && !resolved.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("Default template config path is outside allowed root: " + resolved);
        }
        return resolved;
    }

    private Path loadDefaultTemplatePath() {
        try {
            if (!Files.exists(defaultTemplateConfigPath)) {
                return null;
            }
            JsonNode root = mapper.readTree(Files.readString(defaultTemplateConfigPath, StandardCharsets.UTF_8));
            String value = root.path("default_template_path").asText("").strip();
            if (value.isBlank()) {
                return null;
            }
            Path template = Path.of(value).toAbsolutePath().normalize();
            if (!Files.exists(template)) {
                return null;
            }
            if (allowedRoot != null && !template.startsWith(allowedRoot)) {
                return null;
            }
            return template;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setDefaultTemplateInternal(Path templatePath) throws IOException {
        Path normalized = templatePath.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            throw new IllegalArgumentException("Template path does not exist: " + normalized);
        }
        if (allowedRoot != null && !normalized.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("Template path is outside allowed root: " + normalized);
        }
        this.defaultTemplatePath = normalized;

        ObjectNode root = mapper.createObjectNode();
        root.put("default_template_path", normalized.toString());
        createParentDirectories(defaultTemplateConfigPath);
        Files.writeString(defaultTemplateConfigPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                StandardCharsets.UTF_8);
    }

    private int parseMaxOpenDocs() {
        String raw = System.getenv("MCPO_MAX_OPEN_DOCS");
        if (raw == null || raw.isBlank()) {
            return DEFAULT_MAX_OPEN_DOCS;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : DEFAULT_MAX_OPEN_DOCS;
        } catch (NumberFormatException ex) {
            return DEFAULT_MAX_OPEN_DOCS;
        }
    }

    private Path parseAllowedRoot() {
        String raw = System.getenv("MCPO_ALLOWED_ROOT");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Path.of(raw).toAbsolutePath().normalize();
    }

    private void exportPdfWithSoffice(Path inputPptx, Path outputPdf) throws IOException {
        String sofficeExecutable = System.getenv("SOFFICE_PATH");
        if (sofficeExecutable == null || sofficeExecutable.isBlank()) {
            sofficeExecutable = "soffice";
        }

        Path outputDir = outputPdf.getParent();
        if (outputDir == null) {
            outputDir = Path.of(".").toAbsolutePath().normalize();
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                Arrays.asList(
                        sofficeExecutable,
                        "--headless",
                        "--convert-to",
                        "pdf",
                        "--outdir",
                        outputDir.toString(),
                        inputPptx.toString()));
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process process = processBuilder.start();
        boolean finished;
        try {
            finished = process.waitFor(SOFFICE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PDF export interrupted", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("PDF export timed out waiting for LibreOffice");
        }

        if (process.exitValue() != 0) {
            throw new IOException("LibreOffice PDF export failed with exit code " + process.exitValue());
        }

        String baseName = inputPptx.getFileName().toString();
        int dot = baseName.lastIndexOf('.');
        String pdfName = (dot > 0 ? baseName.substring(0, dot) : baseName) + ".pdf";
        Path generatedPdf = outputDir.resolve(pdfName).toAbsolutePath().normalize();
        if (!Files.exists(generatedPdf)) {
            throw new IOException("Expected PDF output was not created: " + generatedPdf);
        }

        if (!generatedPdf.equals(outputPdf)) {
            Files.move(generatedPdf, outputPdf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private List<Integer> parseShapeIndices(JsonNode node) {
        List<Integer> indices = new ArrayList<>();
        if (!node.isArray()) {
            return indices;
        }
        for (JsonNode item : node) {
            int idx = item.asInt(-1);
            if (idx >= 0) {
                indices.add(idx);
            }
        }
        return indices;
    }

    private String sanitizeTemplateName(String templateName, String extension) {
        String normalized = templateName.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("template_name cannot be blank");
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

    private Rectangle2D.Double selectionBounds(XSLFSlide slide, List<Integer> shapeIndices) {
        List<XSLFShape> shapes = slide.getShapes();
        Rectangle2D bounds = null;
        for (int idx : shapeIndices) {
            if (idx < 0 || idx >= shapes.size()) {
                continue;
            }
            Rectangle2D anchor = shapes.get(idx).getAnchor();
            if (anchor == null) {
                continue;
            }
            bounds = bounds == null ? anchor : bounds.createUnion(anchor);
        }
        if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return null;
        }
        return new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    private String collectSlideText(XSLFSlide slide) {
        List<String> textParts = new ArrayList<>();
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text != null && !text.isBlank()) {
                    textParts.add(text.strip());
                }
            }
        }
        return String.join("\n", textParts);
    }

    private String collectSlideText(XSLFNotes notes) {
        List<String> textParts = new ArrayList<>();
        for (XSLFShape shape : notes.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text != null && !text.isBlank()) {
                    textParts.add(text.strip());
                }
            }
        }
        return String.join("\n", textParts);
    }

    private XSLFSlide createDefaultSlide(XMLSlideShow show) {
        if (!show.getSlideMasters().isEmpty()) {
            XSLFSlideMaster master = show.getSlideMasters().get(0);
            XSLFSlideLayout layout = master.getLayout(SlideLayoutResolver.bestEffortLayout(master));
            if (layout != null) {
                return show.createSlide(layout);
            }
        }
        return show.createSlide();
    }

    private boolean isValidRect(double x, double y, double w, double h) {
        return Double.isFinite(x)
                && Double.isFinite(y)
                && Double.isFinite(w)
                && Double.isFinite(h)
                && w > 0
                && h > 0;
    }

    private ToolDefinition tool(String name, String description, String schemaJson) {
        try {
            return new ToolDefinition(name, description, mapper.readTree(schemaJson));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid schema for tool " + name, e);
        }
    }

    private static final class SlideLayoutResolver {
        private SlideLayoutResolver() {
        }

        static org.apache.poi.xslf.usermodel.SlideLayout bestEffortLayout(XSLFSlideMaster master) {
            org.apache.poi.xslf.usermodel.SlideLayout[] preferred = new org.apache.poi.xslf.usermodel.SlideLayout[] {
                    org.apache.poi.xslf.usermodel.SlideLayout.TITLE,
                    org.apache.poi.xslf.usermodel.SlideLayout.TITLE_AND_CONTENT,
                    org.apache.poi.xslf.usermodel.SlideLayout.BLANK
            };
            for (org.apache.poi.xslf.usermodel.SlideLayout layout : preferred) {
                if (master.getLayout(layout) != null) {
                    return layout;
                }
            }
            return null;
        }
    }
}
