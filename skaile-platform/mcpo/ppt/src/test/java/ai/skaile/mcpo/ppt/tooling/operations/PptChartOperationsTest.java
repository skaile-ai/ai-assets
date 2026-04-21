package ai.skaile.mcpo.ppt.tooling.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.skaile.mcpo.ppt.tooling.PptToolService;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link PptChartOperations}: list_charts happy-path,
 * update_chart_data happy-path + every structured error branch. The fixture
 * is built with {@link ChartFixtureBuilder} — a clustered column chart with
 * 3 series × 4 categories backed by an embedded XLSX workbook.
 */
class PptChartOperationsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void listChartsReportsSeriesAndCategories(@TempDir Path tempDir) throws Exception {
        Path fixture = ChartFixtureBuilder.writeClusteredColumn(tempDir.resolve("chart.pptx"));
        PptToolService service = new PptToolService();
        String docId = openFixture(service, fixture);

        ObjectNode args = mapper.createObjectNode();
        args.put("document_id", docId);
        ToolCallResult result = service.call("ppt.list_charts", args);
        assertTrue(result.success(), "list_charts should succeed: " + result.payload());

        ArrayNode charts = (ArrayNode) result.payload().get("charts");
        assertEquals(1, charts.size());
        JsonNode chart = charts.get(0);
        assertEquals(0, chart.path("slide_index").asInt());
        assertEquals("column", chart.path("chart_type").asText());
        assertTrue(chart.path("has_embedded_workbook").asBoolean());
        assertEquals(3, chart.path("series").size());
        for (int i = 0; i < 3; i++) {
            assertEquals(ChartFixtureBuilder.SERIES_NAMES[i],
                    chart.path("series").get(i).path("name").asText());
            assertEquals(4, chart.path("series").get(i).path("value_count").asInt());
            assertEquals(4, chart.path("series").get(i).path("category_count").asInt());
        }
        JsonNode cats = chart.path("categories");
        assertEquals(4, cats.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(ChartFixtureBuilder.CATEGORIES[i], cats.get(i).asText());
        }
    }

    @Test
    void updateChartDataRewritesCacheAndWorkbook(@TempDir Path tempDir) throws Exception {
        Path fixture = ChartFixtureBuilder.writeClusteredColumn(tempDir.resolve("chart.pptx"));
        PptToolService service = new PptToolService();
        String docId = openFixture(service, fixture);

        double[][] newValues = {
                {100.0, 200.0, 300.0, 400.0},
                {150.0, 250.0, 350.0, 450.0},
                {50.0, 60.0, 70.0, 80.0}
        };
        String[] newCats = {"A", "B", "C", "D"};

        ObjectNode update = mapper.createObjectNode();
        update.put("document_id", docId);
        update.put("slide_index", 0);
        update.put("shape_index", 0);
        ArrayNode seriesArr = update.putArray("series");
        for (int s = 0; s < 3; s++) {
            ObjectNode sNode = seriesArr.addObject();
            sNode.put("name", "Renamed " + (s + 1));
            ArrayNode vals = sNode.putArray("values");
            for (double v : newValues[s]) {
                vals.add(v);
            }
        }
        ArrayNode cats = update.putArray("categories");
        for (String c : newCats) {
            cats.add(c);
        }

        ToolCallResult updated = service.call("ppt.update_chart_data", update);
        assertTrue(updated.success(), "update_chart_data should succeed: " + updated.payload());
        assertEquals(3, updated.payload().path("updated_series").asInt());
        assertTrue(updated.payload().path("categories_updated").asBoolean());

        // Round-trip: save, reopen, re-read caches and workbook to confirm both
        // the numCache XML and embedded workbook reflect the new values.
        Path saved = tempDir.resolve("updated.pptx");
        ObjectNode saveArgs = mapper.createObjectNode();
        saveArgs.put("document_id", docId);
        saveArgs.put("output_path", saved.toString());
        ToolCallResult save = service.call("ppt.export_document", saveArgs);
        assertTrue(save.success(), "save should succeed: " + save.payload());

        try (InputStream in = Files.newInputStream(saved);
             XMLSlideShow reopened = new XMLSlideShow(in)) {
            XSLFSlide slide = reopened.getSlides().get(0);
            XSLFGraphicFrame frame = (XSLFGraphicFrame) slide.getShapes().get(0);
            XSLFChart chart = frame.getChart();
            assertNotNull(chart, "chart relationship should survive round trip");

            var flatSeries = new java.util.ArrayList<XDDFChartData.Series>();
            for (XDDFChartData data : chart.getChartSeries()) {
                for (int i = 0; i < data.getSeriesCount(); i++) {
                    flatSeries.add(data.getSeries(i));
                }
            }
            assertEquals(3, flatSeries.size());

            for (int s = 0; s < 3; s++) {
                XDDFNumericalDataSource<? extends Number> vals = flatSeries.get(s).getValuesData();
                assertEquals(4, vals.getPointCount());
                for (int i = 0; i < 4; i++) {
                    Number pt = vals.getPointAt(i);
                    assertEquals(newValues[s][i], pt.doubleValue(), 0.0001,
                            "numCache value mismatch at series " + s + " index " + i);
                }
            }

            XDDFDataSource<?> cat0 = flatSeries.get(0).getCategoryData();
            for (int i = 0; i < 4; i++) {
                assertEquals(newCats[i], String.valueOf(cat0.getPointAt(i)));
            }

            XSSFWorkbook wb = chart.getWorkbook();
            XSSFSheet sheet = wb.getSheetAt(0);
            CellRangeAddress valRange = new CellRangeAddress(1, 4, 1, 1);
            for (int i = 0; i < 4; i++) {
                double cellVal = sheet.getRow(valRange.getFirstRow() + i)
                        .getCell(valRange.getFirstColumn())
                        .getNumericCellValue();
                assertEquals(newValues[0][i], cellVal, 0.0001,
                        "workbook cell mismatch for series 0 row " + i);
            }
            for (int i = 0; i < 4; i++) {
                XSSFRow row = sheet.getRow(i + 1);
                assertEquals(newCats[i], row.getCell(0).getStringCellValue());
            }
        }
    }

    @Test
    void updateChartDataRejectsNonChartShape(@TempDir Path tempDir) throws Exception {
        PptToolService service = new PptToolService();
        ObjectNode create = mapper.createObjectNode();
        create.put("title", "Non-chart");
        ToolCallResult created = service.call("ppt.create_document", create);
        assertTrue(created.success());
        String docId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", docId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "hello");
        addTextbox.put("x", 50);
        addTextbox.put("y", 50);
        addTextbox.put("width", 200);
        addTextbox.put("height", 60);
        ToolCallResult addShape = service.call("ppt.add_textbox", addTextbox);
        assertTrue(addShape.success());
        int shapeIndex = addShape.payload().path("shape_index").asInt();

        ObjectNode update = mapper.createObjectNode();
        update.put("document_id", docId);
        update.put("slide_index", 0);
        update.put("shape_index", shapeIndex);
        ArrayNode series = update.putArray("series");
        ObjectNode s = series.addObject();
        s.putArray("values").add(1.0);

        ToolCallResult result = service.call("ppt.update_chart_data", update);
        assertFalse(result.success());
        assertEquals("SHAPE_NOT_CHART", result.payload().path("code").asText());
    }

    @Test
    void updateChartDataRejectsSeriesCountMismatch(@TempDir Path tempDir) throws Exception {
        Path fixture = ChartFixtureBuilder.writeClusteredColumn(tempDir.resolve("chart.pptx"));
        PptToolService service = new PptToolService();
        String docId = openFixture(service, fixture);

        ObjectNode update = mapper.createObjectNode();
        update.put("document_id", docId);
        update.put("slide_index", 0);
        update.put("shape_index", 0);
        ArrayNode series = update.putArray("series");
        ObjectNode one = series.addObject();
        ArrayNode vals = one.putArray("values");
        for (int i = 0; i < 4; i++) {
            vals.add((double) i);
        }

        ToolCallResult result = service.call("ppt.update_chart_data", update);
        assertFalse(result.success());
        assertEquals("SERIES_COUNT_MISMATCH", result.payload().path("code").asText());
    }

    @Test
    void updateChartDataRejectsCategoryCountMismatch(@TempDir Path tempDir) throws Exception {
        Path fixture = ChartFixtureBuilder.writeClusteredColumn(tempDir.resolve("chart.pptx"));
        PptToolService service = new PptToolService();
        String docId = openFixture(service, fixture);

        ObjectNode update = mapper.createObjectNode();
        update.put("document_id", docId);
        update.put("slide_index", 0);
        update.put("shape_index", 0);
        ArrayNode series = update.putArray("series");
        for (int s = 0; s < 3; s++) {
            ArrayNode vals = series.addObject().putArray("values");
            for (int i = 0; i < 3; i++) {
                vals.add(1.0);
            }
        }

        ToolCallResult result = service.call("ppt.update_chart_data", update);
        assertFalse(result.success());
        assertEquals("CATEGORY_COUNT_MISMATCH", result.payload().path("code").asText());
    }

    @Test
    void updateChartDataRejectsCategoriesCountMismatch(@TempDir Path tempDir) throws Exception {
        Path fixture = ChartFixtureBuilder.writeClusteredColumn(tempDir.resolve("chart.pptx"));
        PptToolService service = new PptToolService();
        String docId = openFixture(service, fixture);

        ObjectNode update = mapper.createObjectNode();
        update.put("document_id", docId);
        update.put("slide_index", 0);
        update.put("shape_index", 0);
        ArrayNode series = update.putArray("series");
        for (int s = 0; s < 3; s++) {
            ArrayNode vals = series.addObject().putArray("values");
            for (int i = 0; i < 4; i++) {
                vals.add(1.0);
            }
        }
        ArrayNode cats = update.putArray("categories");
        cats.add("only");
        cats.add("two");

        ToolCallResult result = service.call("ppt.update_chart_data", update);
        assertFalse(result.success());
        assertEquals("CATEGORY_COUNT_MISMATCH", result.payload().path("code").asText());
    }

    @Test
    void listChartsRespectsSlideIndexFilter(@TempDir Path tempDir) throws Exception {
        Path fixture = ChartFixtureBuilder.writeClusteredColumn(tempDir.resolve("chart.pptx"));
        PptToolService service = new PptToolService();
        String docId = openFixture(service, fixture);

        // Add a second (chart-less) slide; filter to slide 1 should return empty.
        ObjectNode addSlide = mapper.createObjectNode();
        addSlide.put("document_id", docId);
        ToolCallResult added = service.call("ppt.add_slide", addSlide);
        assertTrue(added.success());

        ObjectNode listSlide1 = mapper.createObjectNode();
        listSlide1.put("document_id", docId);
        listSlide1.put("slide_index", 1);
        ToolCallResult result1 = service.call("ppt.list_charts", listSlide1);
        assertTrue(result1.success());
        assertEquals(0, result1.payload().path("charts").size());

        ObjectNode listSlide0 = mapper.createObjectNode();
        listSlide0.put("document_id", docId);
        listSlide0.put("slide_index", 0);
        ToolCallResult result0 = service.call("ppt.list_charts", listSlide0);
        assertTrue(result0.success());
        assertEquals(1, result0.payload().path("charts").size());
    }

    @Test
    void capabilitiesReportsChartsUpdateEnabled() {
        PptToolService service = new PptToolService();
        ToolCallResult caps = service.call("ppt.capabilities", mapper.createObjectNode());
        assertTrue(caps.success());
        assertTrue(caps.payload()
                .path("feature_flags").path("charts_update").asBoolean());
    }

    private String openFixture(PptToolService service, Path fixture) {
        ObjectNode open = mapper.createObjectNode();
        open.put("path", fixture.toString());
        ToolCallResult opened = service.call("ppt.open_document", open);
        assertTrue(opened.success(), "open_document should succeed: " + opened.payload());
        return opened.payload().path("document_id").asText();
    }
}
