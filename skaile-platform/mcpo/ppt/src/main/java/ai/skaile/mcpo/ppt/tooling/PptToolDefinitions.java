package ai.skaile.mcpo.ppt.tooling;

import ai.skaile.mcpo.ppt.tooling.contracts.ToolDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

final class PptToolDefinitions {
    private PptToolDefinitions() {
    }

    static List<ToolDefinition> create(ObjectMapper mapper) {
                return List.of(
                tool(mapper, "ppt.create_document", "Create a new in-memory PowerPoint document and return a document handle.",
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
                tool(mapper, "ppt.open_document", "Open an existing PPTX file into memory and return a document handle.",
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
                tool(mapper, "ppt.close_document", "Close an open document handle and release memory.",
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
                tool(mapper, "ppt.get_document_info", "Get high-level metadata for an open document.",
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
                                tool(mapper, "ppt.set_page_setup", "Set slide page size using preset or custom dimensions.",
                                                """
                                                                {
                                                                    "type": "object",
                                                                    "properties": {
                                                                        "document_id": {"type": "string"},
                                                                        "preset": {"type": "string", "enum": ["standard_4_3", "widescreen_16_9", "a4_landscape", "a4_portrait", "custom"]},
                                                                        "width": {"type": "integer", "minimum": 1},
                                                                        "height": {"type": "integer", "minimum": 1}
                                                                    },
                                                                    "required": ["document_id", "preset"],
                                                                    "additionalProperties": false
                                                                }
                                                                """),
                tool(mapper, "ppt.list_slides", "List all slides and quick text previews.",
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
                                tool(mapper, "ppt.reorder_slides", "Reorder all slides using a full index permutation.",
                                                """
                                                                {
                                                                    "type": "object",
                                                                    "properties": {
                                                                        "document_id": {"type": "string"},
                                                                        "new_order": {
                                                                            "type": "array",
                                                                            "items": {"type": "integer", "minimum": 0},
                                                                            "minItems": 1
                                                                        }
                                                                    },
                                                                    "required": ["document_id", "new_order"],
                                                                    "additionalProperties": false
                                                                }
                                                                """),
                tool(mapper, "ppt.get_slide_content", "Get textual content and shape metadata for one slide.",
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
                tool(mapper, "ppt.add_slide", "Add a slide to a document and optionally initialize title text.",
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
                                tool(mapper, "ppt.duplicate_slide", "Duplicate one slide and optionally place it at a target index.",
                                                """
                                                                {
                                                                    "type": "object",
                                                                    "properties": {
                                                                        "document_id": {"type": "string"},
                                                                        "source_slide_index": {"type": "integer", "minimum": 0},
                                                                        "target_index": {"type": "integer", "minimum": 0}
                                                                    },
                                                                    "required": ["document_id", "source_slide_index"],
                                                                    "additionalProperties": false
                                                                }
                                                                """),
                                tool(mapper, "ppt.delete_slides", "Delete one or more slides by index.",
                                                """
                                                                {
                                                                    "type": "object",
                                                                    "properties": {
                                                                        "document_id": {"type": "string"},
                                                                        "slide_indices": {
                                                                            "type": "array",
                                                                            "items": {"type": "integer", "minimum": 0},
                                                                            "minItems": 1
                                                                        },
                                                                        "keep_at_least_one": {"type": "boolean", "default": true}
                                                                    },
                                                                    "required": ["document_id", "slide_indices"],
                                                                    "additionalProperties": false
                                                                }
                                                                """),
                                tool(mapper, "ppt.merge_presentations", "Merge slides from another presentation into an open document.",
                                                """
                                                                {
                                                                    "type": "object",
                                                                    "properties": {
                                                                        "document_id": {"type": "string"},
                                                                        "merge_path": {"type": "string"},
                                                                        "insert_at_index": {"type": "integer", "minimum": 0}
                                                                    },
                                                                    "required": ["document_id", "merge_path"],
                                                                    "additionalProperties": false
                                                                }
                                                                """),
                tool(mapper, "ppt.update_text", "Replace text in a specific slide.",
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
                                tool(mapper, "ppt.replace_text_globally", "Replace text occurrences across all slides in a document.",
                                                """
                                                                {
                                                                    "type": "object",
                                                                    "properties": {
                                                                        "document_id": {"type": "string"},
                                                                        "old_text": {"type": "string"},
                                                                        "new_text": {"type": "string"},
                                                                        "case_sensitive": {"type": "boolean", "default": false},
                                                                        "max_replacements": {"type": "integer", "minimum": 1}
                                                                    },
                                                                    "required": ["document_id", "old_text", "new_text"],
                                                                    "additionalProperties": false
                                                                }
                                                                """),
                tool(mapper, "ppt.add_textbox", "Add a text box to a slide at a specific position.",
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
                                tool(mapper, "ppt.add_shape", "Add a primitive shape (rectangle, ellipse, line, arrow) to a slide.",
                                                """
                                                                {
                                                                    "type": "object",
                                                                    "properties": {
                                                                        "document_id": {"type": "string"},
                                                                        "slide_index": {"type": "integer", "minimum": 0},
                                                                        "shape_type": {"type": "string", "enum": ["rectangle", "ellipse", "line", "arrow"]},
                                                                        "x": {"type": "number"},
                                                                        "y": {"type": "number"},
                                                                        "width": {"type": "number", "minimum": 1},
                                                                        "height": {"type": "number", "minimum": 1},
                                                                        "text": {"type": "string"},
                                                                        "fill_color": {"type": "string"},
                                                                        "border_color": {"type": "string"},
                                                                        "border_width": {"type": "number", "minimum": 0}
                                                                    },
                                                                    "required": ["document_id", "slide_index", "shape_type", "x", "y", "width", "height"],
                                                                    "additionalProperties": false
                                                                }
                                                                """),
                tool(mapper, "ppt.insert_image", "Insert an image into a slide at a specific position.",
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
                tool(mapper, "ppt.replace_image", "Replace an existing picture shape with a new image while preserving placement.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "image_path": {"type": "string"},
                                        "keep_size": {"type": "boolean", "default": true}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index", "image_path"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.get_slide_notes", "Get speaker notes text for one slide.",
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
                tool(mapper, "ppt.set_slide_notes", "Set speaker notes text for one slide.",
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
                tool(mapper, "ppt.add_table", "Add a table to a slide.",
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
                tool(mapper, "ppt.get_table",
                        "Return the full structure and text of a table shape.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.edit_table",
                        "Edit a table via a single operation. Implemented: set_cell, insert_row, delete_row, insert_col, delete_col, set_row_height, set_col_width, set_header_style. Reserved for Phase 4: merge_cells, set_cell_border.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "operation": {
                                            "type": "string",
                                            "enum": [
                                                "set_cell", "insert_row", "delete_row",
                                                "insert_col", "delete_col",
                                                "set_row_height", "set_col_width",
                                                "set_header_style",
                                                "merge_cells", "set_cell_border"
                                            ]
                                        },
                                        "row": {"type": "integer", "minimum": 0},
                                        "col": {"type": "integer", "minimum": 0},
                                        "text": {"type": "string"},
                                        "index": {"type": "integer", "minimum": 0},
                                        "row_index": {"type": "integer", "minimum": 0},
                                        "col_index": {"type": "integer", "minimum": 0},
                                        "height": {"type": "number", "minimum": 1},
                                        "width": {"type": "number", "minimum": 1},
                                        "fill_color": {"type": "string"},
                                        "font_color": {"type": "string"},
                                        "bold": {"type": "boolean"},
                                        "start_row": {"type": "integer", "minimum": 0},
                                        "start_col": {"type": "integer", "minimum": 0},
                                        "end_row": {"type": "integer", "minimum": 0},
                                        "end_col": {"type": "integer", "minimum": 0},
                                        "sides": {
                                            "type": "array",
                                            "items": {"type": "string", "enum": ["top", "bottom", "left", "right", "all"]}
                                        },
                                        "color": {"type": "string"},
                                        "dash_style": {"type": "string", "enum": ["solid", "dash", "dot", "dashdot"]}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index", "operation"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.set_text",
                        "Unified text mutation. scope=shape|run|paragraph selects the target; run-style fields (bold/italic/underline/font_*) apply to the selected runs, paragraph-style fields (text_align/line_spacing/bullet_*) apply to the matching paragraphs. strikethrough, rotation, auto_fit accept the schema but implementation lands in Phase 4.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "scope": {"type": "string", "enum": ["shape", "run", "paragraph"], "default": "shape"},
                                        "target_text": {"type": "string"},
                                        "occurrence": {"type": "integer", "minimum": 1, "default": 1},
                                        "case_sensitive": {"type": "boolean", "default": true},
                                        "paragraph_index": {"type": "integer", "minimum": 0},
                                        "bold": {"type": "boolean"},
                                        "italic": {"type": "boolean"},
                                        "underline": {"type": "boolean"},
                                        "strikethrough": {"type": "boolean"},
                                        "font_size": {"type": "number", "minimum": 1},
                                        "font_color": {"type": "string"},
                                        "font_family": {"type": "string"},
                                        "rotation": {"type": "number"},
                                        "auto_fit": {"type": "string", "enum": ["none", "normal", "shrink"]},
                                        "text_align": {"type": "string", "enum": ["left", "center", "right", "justify"]},
                                        "line_spacing": {"type": "number", "minimum": 0},
                                        "space_before": {"type": "number"},
                                        "space_after": {"type": "number"},
                                        "left_margin": {"type": "number"},
                                        "indent": {"type": "number"},
                                        "bullet_enabled": {"type": "boolean"},
                                        "numbered": {"type": "boolean"},
                                        "bullet_character": {"type": "string"},
                                        "bullet_level": {"type": "integer", "minimum": 0}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.move_shape", "Move a shape to a new x/y position on a slide.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "x": {"type": "number"},
                                        "y": {"type": "number"}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index", "x", "y"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.clone_shape", "Clone a text-capable shape on the same slide.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "offset_x": {"type": "number", "default": 20},
                                        "offset_y": {"type": "number", "default": 20}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.resize_shape", "Resize a shape to a new width/height.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "width": {"type": "number", "minimum": 1},
                                        "height": {"type": "number", "minimum": 1}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index", "width", "height"],
                                    "additionalProperties": false
                                }
                                """),

                tool(mapper, "ppt.add_hyperlink", "Attach a hyperlink to all text runs in a text shape.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "url": {"type": "string"}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index", "url"],
                                    "additionalProperties": false
                                }
                                """),


                tool(mapper, "ppt.set_slide_background", "Set a solid background color on a slide.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "color": {"type": "string"}
                                    },
                                    "required": ["document_id", "slide_index", "color"],
                                    "additionalProperties": false
                                }
                                """),

                tool(mapper, "ppt.import_markdown_outline", "Create a presentation from markdown headings and bullets.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "markdown_text": {"type": "string"},
                                        "output_path": {"type": "string"}
                                    },
                                    "required": ["markdown_text"],
                                    "additionalProperties": false
                                }
                                """),



                tool(mapper, "ppt.transaction_begin", "Start a transaction snapshot for a document.",
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
                tool(mapper, "ppt.transaction_commit", "Commit transaction and discard snapshot.",
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
                tool(mapper, "ppt.transaction_rollback", "Rollback document to last transaction snapshot.",
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


                tool(mapper, "ppt.get_slide_metrics", "Analyze slide composition and text density.",
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



                tool(mapper, "ppt.export_document",
                        "Export an open in-memory document to disk. format=pptx|pdf are implemented; html|png_batch|jpg_batch|svg_batch|outline_text are reserved for Phase 3.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"},
                                    "output_path": {"type": "string"},
                                    "format": {
                                      "type": "string",
                                      "enum": ["pptx", "pdf", "html", "png_batch", "jpg_batch", "svg_batch", "outline_text"],
                                      "default": "pptx"
                                    }
                                  },
                                  "required": ["document_id"],
                                  "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.render_slide",
                        "Render one slide to an image. format=png|jpg|svg (default png). fidelity=low|high; high fidelity lands in Phase 3.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"},
                                    "slide_index": {"type": "integer", "minimum": 0},
                                    "output_path": {"type": "string"},
                                    "format": {"type": "string", "enum": ["png", "jpg", "svg"], "default": "png"},
                                    "fidelity": {"type": "string", "enum": ["low", "high"], "default": "low"},
                                    "width": {"type": "integer", "minimum": 1},
                                    "height": {"type": "integer", "minimum": 1}
                                  },
                                  "required": ["document_id", "slide_index", "output_path"],
                                  "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.render_all_slides",
                        "Render every slide to an image file in output_dir. format=png|jpg|svg (default png). fidelity=low|high; high fidelity lands in Phase 3.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "document_id": {"type": "string"},
                                    "output_dir": {"type": "string"},
                                    "format": {"type": "string", "enum": ["png", "jpg", "svg"], "default": "png"},
                                    "fidelity": {"type": "string", "enum": ["low", "high"], "default": "low"},
                                    "file_name_pattern": {"type": "string", "default": "slide-%03d"},
                                    "width": {"type": "integer", "minimum": 1},
                                    "height": {"type": "integer", "minimum": 1}
                                  },
                                  "required": ["document_id", "output_dir"],
                                  "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.find_text", "Find text occurrences across all slides in an open presentation.",
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


                tool(mapper, "ppt.upload_template",
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
                tool(mapper, "ppt.set_default_template",
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
                tool(mapper, "ppt.get_default_template", "Return the currently configured default template.",
                        """
                                {
                                    "type": "object",
                                    "properties": {},
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.generate_presentation",
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
                                """),
                tool(mapper, "ppt.delete_shape", "Remove a shape from a slide by index.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.get_shape_properties", "Get detailed properties of a shape.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.set_shape_style", "Set shape fill, border, and text alignment style.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "fill_color": {"type": "string"},
                                        "border_color": {"type": "string"},
                                        "border_width": {"type": "number", "minimum": 0},
                                        "text_align": {"type": "string", "enum": ["left", "center", "right", "justify"]}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.set_document_metadata", "Set document metadata fields.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "title": {"type": "string"},
                                        "author": {"type": "string"},
                                        "subject": {"type": "string"},
                                        "keywords": {"type": "string"}
                                    },
                                    "required": ["document_id"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.set_slide_layout", "Apply a specific layout type to a slide.",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "layout_type": {"type": "string", "enum": ["blank", "title", "title_content", "title_only"]}
                                    },
                                    "required": ["document_id", "slide_index", "layout_type"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.set_shape_z_order", "Move a shape in z-order (front/back/forward/backward).",
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "document_id": {"type": "string"},
                                        "slide_index": {"type": "integer", "minimum": 0},
                                        "shape_index": {"type": "integer", "minimum": 0},
                                        "position": {"type": "string", "enum": ["front", "back", "forward", "backward"]}
                                    },
                                    "required": ["document_id", "slide_index", "shape_index", "position"],
                                    "additionalProperties": false
                                }
                                """),
                tool(mapper, "ppt.capabilities",
                        "Return server version, Apache POI / LibreOffice availability, supported input/export/render formats, installed fonts, feature flags, and safety limits.",
                        """
                                {
                                    "type": "object",
                                    "properties": {},
                                    "additionalProperties": false
                                }
                                """));
    }

    private static ToolDefinition tool(ObjectMapper mapper, String name, String description, String schemaJson) {
        try {
            return new ToolDefinition(name, description, mapper.readTree(schemaJson));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse tool schema for " + name, e);
        }
    }
}
