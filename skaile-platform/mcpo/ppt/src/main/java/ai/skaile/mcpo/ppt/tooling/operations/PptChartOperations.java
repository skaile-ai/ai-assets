package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeFinder;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.XDDFArea3DChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFAreaChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFBar3DChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLine3DChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFPie3DChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFPieChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFScatterChartData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * {@code ppt.list_charts} + {@code ppt.update_chart_data}. Supports the data-update
 * workflow for charts that were authored in PowerPoint / LibreOffice (v2 will add
 * chart creation). Every write path updates both the chart's numeric/string cache
 * XML <em>and</em> the embedded XLSX workbook, so PowerPoint's "Edit Data" button
 * continues to open the right numbers after a mutation.
 *
 * <p>Errors surfaced from handlers:
 * <ul>
 *   <li>{@code SHAPE_NOT_CHART} — shape_index does not resolve to a chart graphic frame.</li>
 *   <li>{@code EMBEDDED_WORKBOOK_MISSING} — the chart has no embedded XLSX relationship.</li>
 *   <li>{@code SERIES_COUNT_MISMATCH} — number of provided series ≠ chart's series count.</li>
 *   <li>{@code CATEGORY_COUNT_MISMATCH} — values-per-series or categories array size ≠ existing.</li>
 * </ul>
 */
public final class PptChartOperations {

    private final ObjectMapper mapper;
    private final ToolResponseFactory responseFactory;
    private final PptShapeFinder shapeFinder;

    public PptChartOperations(ObjectMapper mapper,
                              ToolResponseFactory responseFactory,
                              PptShapeFinder shapeFinder) {
        this.mapper = mapper;
        this.responseFactory = responseFactory;
        this.shapeFinder = shapeFinder;
    }

    public Map<String, ToolHandler> handlers() {
        return Map.of(
                "ppt.list_charts", this::listCharts,
                "ppt.update_chart_data", this::updateChartData);
    }

    // ---------- list_charts ----------

    ToolCallResult listCharts(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);
        XMLSlideShow pptx = session.getSlideShow();

        int startSlide = 0;
        int endSlide = pptx.getSlides().size();
        if (args.hasNonNull("slide_index")) {
            int slideIdx = args.path("slide_index").asInt(-1);
            shapeFinder.requireSlide(session, slideIdx);
            startSlide = slideIdx;
            endSlide = slideIdx + 1;
        }

        ArrayNode charts = mapper.createArrayNode();
        for (int si = startSlide; si < endSlide; si++) {
            XSLFSlide slide = pptx.getSlides().get(si);
            List<XSLFShape> shapes = slide.getShapes();
            for (int pi = 0; pi < shapes.size(); pi++) {
                XSLFShape shape = shapes.get(pi);
                if (shape instanceof XSLFGraphicFrame frame && frame.hasChart()) {
                    XSLFChart chart = frame.getChart();
                    if (chart != null) {
                        charts.add(summarizeChart(chart, si, pi));
                    }
                }
            }
        }

        ObjectNode payload = responseFactory.okPayload();
        payload.put("document_id", session.getId());
        payload.set("charts", charts);
        return responseFactory.success(payload);
    }

    private ObjectNode summarizeChart(XSLFChart chart, int slideIndex, int shapeIndex) {
        ObjectNode node = mapper.createObjectNode();
        node.put("slide_index", slideIndex);
        node.put("shape_index", shapeIndex);

        List<XDDFChartData> allData = chart.getChartSeries();
        node.put("chart_type", detectChartType(allData));

        ArrayNode seriesNode = node.putArray("series");
        ArrayNode categoriesNode = node.putArray("categories");
        boolean capturedCategories = false;
        for (XDDFChartData data : allData) {
            for (int i = 0; i < data.getSeriesCount(); i++) {
                XDDFChartData.Series series = data.getSeries(i);
                ObjectNode sNode = seriesNode.addObject();
                sNode.put("name", extractSeriesName(series));
                XDDFDataSource<?> cat = series.getCategoryData();
                XDDFNumericalDataSource<? extends Number> vals = series.getValuesData();
                sNode.put("category_count", cat != null ? cat.getPointCount() : 0);
                sNode.put("value_count", vals != null ? vals.getPointCount() : 0);
                if (!capturedCategories && cat != null) {
                    for (int c = 0; c < cat.getPointCount(); c++) {
                        Object pt = cat.getPointAt(c);
                        categoriesNode.add(pt == null ? "" : String.valueOf(pt));
                    }
                    capturedCategories = true;
                }
            }
        }

        node.put("has_embedded_workbook", hasEmbeddedWorkbook(chart));
        return node;
    }

    // ---------- update_chart_data ----------

    ToolCallResult updateChartData(JsonNode args) throws IOException, InvalidFormatException {
        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);

        if (!(shape instanceof XSLFGraphicFrame frame) || !frame.hasChart()) {
            return responseFactory.error("SHAPE_NOT_CHART",
                    "shape_index does not point to a chart shape", false);
        }
        XSLFChart chart = frame.getChart();
        if (chart == null) {
            return responseFactory.error("SHAPE_NOT_CHART",
                    "Could not resolve chart part for shape", false);
        }
        if (!hasEmbeddedWorkbook(chart)) {
            return responseFactory.error("EMBEDDED_WORKBOOK_MISSING",
                    "Chart has no embedded workbook; legacy chart format not supported",
                    false);
        }

        JsonNode seriesInput = args.path("series");
        if (!seriesInput.isArray()) {
            return responseFactory.error("VALIDATION_ERROR",
                    "series must be an array of {name?, values} objects", false);
        }

        List<XDDFChartData> allData = chart.getChartSeries();
        List<XDDFChartData.Series> flatSeries = new ArrayList<>();
        for (XDDFChartData data : allData) {
            for (int i = 0; i < data.getSeriesCount(); i++) {
                flatSeries.add(data.getSeries(i));
            }
        }

        if (seriesInput.size() != flatSeries.size()) {
            return responseFactory.error("SERIES_COUNT_MISMATCH",
                    "Chart has " + flatSeries.size() + " series but "
                            + seriesInput.size() + " provided", false);
        }

        for (int i = 0; i < seriesInput.size(); i++) {
            JsonNode values = seriesInput.get(i).path("values");
            if (!values.isArray()) {
                return responseFactory.error("VALIDATION_ERROR",
                        "series[" + i + "].values must be an array of numbers", false);
            }
            int existingCount = flatSeries.get(i).getValuesData().getPointCount();
            if (values.size() != existingCount) {
                return responseFactory.error("CATEGORY_COUNT_MISMATCH",
                        "series[" + i + "] has " + existingCount
                                + " existing values but " + values.size() + " provided",
                        false);
            }
        }

        boolean categoriesUpdated = false;
        JsonNode catInput = args.path("categories");
        if (catInput != null && !catInput.isMissingNode() && !catInput.isNull()) {
            if (!catInput.isArray()) {
                return responseFactory.error("VALIDATION_ERROR",
                        "categories must be an array of strings", false);
            }
            int existingCatCount = flatSeries.isEmpty()
                    ? 0
                    : flatSeries.get(0).getCategoryData().getPointCount();
            if (catInput.size() != existingCatCount) {
                return responseFactory.error("CATEGORY_COUNT_MISMATCH",
                        "Chart has " + existingCatCount + " existing categories but "
                                + catInput.size() + " provided", false);
            }
            updateCategoriesForAllSeries(chart, flatSeries, catInput);
            categoriesUpdated = true;
        }

        for (int i = 0; i < seriesInput.size(); i++) {
            JsonNode s = seriesInput.get(i);
            double[] newValues = new double[s.path("values").size()];
            for (int k = 0; k < newValues.length; k++) {
                newValues[k] = s.path("values").get(k).asDouble();
            }
            updateSeriesValues(chart, flatSeries.get(i), newValues);
            if (s.hasNonNull("name")) {
                String newName = s.path("name").asText();
                XDDFChartData.Series series = flatSeries.get(i);
                series.setTitle(newName);
                // series.setTitle rewrites the chart XML's c:tx strCache but leaves the
                // embedded XLSX header cell pointing at the old shared-string entry.
                // Resolve the title's cell reference and overwrite the cell so
                // PowerPoint's "Edit Data" dialog stays consistent with the chart.
                writeSeriesNameToWorkbook(chart, series, newName);
            }
        }

        session.touch(true);
        ObjectNode payload = responseFactory.okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("updated_series", flatSeries.size());
        payload.put("categories_updated", categoriesUpdated);
        return responseFactory.success(payload);
    }

    // ---------- value / category writers ----------

    /**
     * Write {@code newValues} into the embedded workbook cells at the series' value
     * reference (if reference-backed), rebuild the data source, and replay {@code plot()}
     * so the chart's numCache XML matches. If the existing source is literal (rare for
     * workbook-backed charts), only the numCache is refreshed.
     */
    private void updateSeriesValues(XSLFChart chart, XDDFChartData.Series series, double[] newValues)
            throws IOException, InvalidFormatException {
        XDDFNumericalDataSource<? extends Number> existing = series.getValuesData();
        Double[] boxed = new Double[newValues.length];
        for (int i = 0; i < newValues.length; i++) {
            boxed[i] = newValues[i];
        }
        String formatCode = existing == null ? null : existing.getFormatCode();

        XDDFNumericalDataSource<Double> replacement;
        if (existing != null && existing.isReference()) {
            SheetRange sheetRange = resolveSheetRange(chart, existing.getDataRangeReference());
            if (sheetRange != null) {
                writeNumericCells(sheetRange, newValues);
                replacement = XDDFDataSourcesFactory.fromNumericCellRange(
                        sheetRange.sheet, sheetRange.range);
            } else {
                replacement = XDDFDataSourcesFactory.fromArray(boxed);
            }
        } else {
            replacement = XDDFDataSourcesFactory.fromArray(boxed);
        }
        if (formatCode != null) {
            replacement.setFormatCode(formatCode);
        }

        series.replaceData(series.getCategoryData(), replacement);
        series.plot();
    }

    /**
     * Rewrites the shared category source on every series. Category columns in
     * clustered bar / column / line charts are typically re-used across all series,
     * so we update once per series even though the workbook cells are shared —
     * {@code plot()} per series rewrites the strCache on that series's XML.
     */
    private void updateCategoriesForAllSeries(XSLFChart chart,
                                              List<XDDFChartData.Series> flatSeries,
                                              JsonNode catInput)
            throws IOException, InvalidFormatException {
        String[] newCategories = new String[catInput.size()];
        for (int i = 0; i < newCategories.length; i++) {
            newCategories[i] = catInput.get(i).asText("");
        }

        boolean wroteToWorkbook = false;
        for (XDDFChartData.Series series : flatSeries) {
            XDDFDataSource<?> existing = series.getCategoryData();
            String formatCode = existing == null ? null : existing.getFormatCode();
            XDDFDataSource<String> replacement;
            if (existing != null && existing.isReference()) {
                SheetRange sheetRange = resolveSheetRange(chart, existing.getDataRangeReference());
                if (sheetRange != null) {
                    if (!wroteToWorkbook) {
                        writeStringCells(sheetRange, newCategories);
                        wroteToWorkbook = true;
                    }
                    replacement = XDDFDataSourcesFactory.fromStringCellRange(
                            sheetRange.sheet, sheetRange.range);
                } else {
                    replacement = XDDFDataSourcesFactory.fromArray(newCategories);
                }
            } else {
                replacement = XDDFDataSourcesFactory.fromArray(newCategories);
            }
            // fromStringCellRange never exposes setFormatCode on its XDDFCategoryDataSource,
            // so this is only exercised when the existing source was a literal numeric-categorized
            // source — an edge case that's safe to ignore for v1.
            if (formatCode != null && replacement instanceof XDDFNumericalDataSource<?> num) {
                num.setFormatCode(formatCode);
            }

            series.replaceData(replacement, series.getValuesData());
            series.plot();
        }
    }

    // ---------- helpers ----------

    private static String detectChartType(List<XDDFChartData> allData) {
        if (allData == null || allData.isEmpty()) {
            return "unknown";
        }
        XDDFChartData first = allData.get(0);
        if (first instanceof XDDFBarChartData bar) {
            return bar.getBarDirection() == BarDirection.BAR ? "bar" : "column";
        }
        if (first instanceof XDDFBar3DChartData bar3d) {
            return bar3d.getBarDirection() == BarDirection.BAR ? "bar" : "column";
        }
        if (first instanceof XDDFLineChartData || first instanceof XDDFLine3DChartData) {
            return "line";
        }
        if (first instanceof XDDFPieChartData || first instanceof XDDFPie3DChartData) {
            return "pie";
        }
        if (first instanceof XDDFScatterChartData) {
            return "scatter";
        }
        if (first instanceof XDDFAreaChartData || first instanceof XDDFArea3DChartData) {
            return "area";
        }
        return "unknown";
    }

    private static boolean hasEmbeddedWorkbook(XSLFChart chart) {
        for (var rp : chart.getRelationParts()) {
            if (POIXMLDocument.PACK_OBJECT_REL_TYPE
                    .equals(rp.getRelationship().getRelationshipType())) {
                return true;
            }
        }
        return false;
    }

    private static String extractSeriesName(XDDFChartData.Series series) {
        // POI exposes the cached title string only indirectly via the underlying CTSerTx. We
        // parse the cache value off the series' value source's getFormatCode() is not it —
        // fall back to reading the XML via selectPath on the series' underlying object.
        // XDDF doesn't expose getCTSer publicly; the concrete subclass holds it as a field.
        // Simpler: if the series name is in the c:tx/c:v direct value or c:tx/c:strRef/c:strCache
        // we reach it via the series' XML. Use XmlObject accessor via reflection-free path:
        // series.getSeriesText() is protected; call via the shared accessor helper below.
        try {
            java.lang.reflect.Method m = XDDFChartData.Series.class.getDeclaredMethod("getSeriesText");
            m.setAccessible(true);
            Object tx = m.invoke(series);
            if (tx instanceof org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx ctTx) {
                if (ctTx.isSetV() && ctTx.getV() != null) {
                    return ctTx.getV();
                }
                if (ctTx.isSetStrRef() && ctTx.getStrRef().isSetStrCache()) {
                    var cache = ctTx.getStrRef().getStrCache();
                    if (cache.sizeOfPtArray() > 0) {
                        return cache.getPtArray(0).getV();
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // POI surface changed between versions — fall through to empty name.
        }
        return "";
    }

    /** Resolve a chart data reference like {@code Sheet1!$B$2:$B$5} to a concrete sheet + range. */
    private static SheetRange resolveSheetRange(XSLFChart chart, String dataRangeReference)
            throws IOException, InvalidFormatException {
        if (dataRangeReference == null || dataRangeReference.isBlank()) {
            return null;
        }
        XSSFWorkbook workbook = chart.getWorkbook();
        if (workbook == null) {
            return null;
        }
        String sheetName;
        String addrPart;
        int bang = dataRangeReference.indexOf('!');
        if (bang >= 0) {
            sheetName = unquoteSheetName(dataRangeReference.substring(0, bang));
            addrPart = dataRangeReference.substring(bang + 1);
        } else {
            sheetName = workbook.getSheetAt(0).getSheetName();
            addrPart = dataRangeReference;
        }
        XSSFSheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        try {
            CellRangeAddress range = CellRangeAddress.valueOf(addrPart);
            return new SheetRange(sheet, range);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String unquoteSheetName(String raw) {
        String v = raw.strip();
        if (v.startsWith("'") && v.endsWith("'") && v.length() >= 2) {
            return v.substring(1, v.length() - 1).replace("''", "'");
        }
        return v;
    }

    private static void writeNumericCells(SheetRange sr, double[] values) {
        int rows = sr.range.getLastRow() - sr.range.getFirstRow() + 1;
        int cols = sr.range.getLastColumn() - sr.range.getFirstColumn() + 1;
        boolean columnRange = rows >= cols;
        int limit = Math.min(values.length, columnRange ? rows : cols);
        for (int i = 0; i < limit; i++) {
            int rowIdx = sr.range.getFirstRow() + (columnRange ? i : 0);
            int colIdx = sr.range.getFirstColumn() + (columnRange ? 0 : i);
            cellAt(sr.sheet, rowIdx, colIdx).setCellValue(values[i]);
        }
    }

    private static void writeStringCells(SheetRange sr, String[] values) {
        int rows = sr.range.getLastRow() - sr.range.getFirstRow() + 1;
        int cols = sr.range.getLastColumn() - sr.range.getFirstColumn() + 1;
        boolean columnRange = rows >= cols;
        int limit = Math.min(values.length, columnRange ? rows : cols);
        for (int i = 0; i < limit; i++) {
            int rowIdx = sr.range.getFirstRow() + (columnRange ? i : 0);
            int colIdx = sr.range.getFirstColumn() + (columnRange ? 0 : i);
            cellAt(sr.sheet, rowIdx, colIdx).setCellValue(values[i] == null ? "" : values[i]);
        }
    }

    /**
     * Extract the series title's c:tx/c:strRef/c:f cell reference (e.g. {@code Sheet1!$B$1})
     * and overwrite the targeted workbook cell with {@code newName}. Best-effort: if the
     * series title is a direct c:v literal with no ref, or the ref can't be resolved, we
     * skip silently — the chart XML was already updated by {@code series.setTitle}.
     */
    private static void writeSeriesNameToWorkbook(XSLFChart chart, XDDFChartData.Series series,
                                                  String newName)
            throws IOException, InvalidFormatException {
        String titleRef = extractSeriesTitleRef(series);
        if (titleRef == null || titleRef.isBlank()) {
            return;
        }
        SheetRange sheetRange = resolveSheetRange(chart, titleRef);
        if (sheetRange == null) {
            return;
        }
        // Series-title refs are almost always a single cell (e.g. $B$1), but the range
        // may include multiple cells when a series name spans merged headers — write to
        // the first cell in the range either way.
        int rowIdx = sheetRange.range.getFirstRow();
        int colIdx = sheetRange.range.getFirstColumn();
        cellAt(sheetRange.sheet, rowIdx, colIdx).setCellValue(newName == null ? "" : newName);
    }

    /** Pulls {@code c:tx/c:strRef/c:f} off the series tx via the reflective helper already used by list_charts. */
    private static String extractSeriesTitleRef(XDDFChartData.Series series) {
        try {
            java.lang.reflect.Method m = XDDFChartData.Series.class.getDeclaredMethod("getSeriesText");
            m.setAccessible(true);
            Object tx = m.invoke(series);
            if (tx instanceof org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx ctTx) {
                if (ctTx.isSetStrRef()) {
                    String f = ctTx.getStrRef().getF();
                    if (f != null && !f.isBlank()) {
                        return f;
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // POI surface changed — fall through to null, caller skips the write.
        }
        return null;
    }

    private static XSSFCell cellAt(XSSFSheet sheet, int rowIdx, int colIdx) {
        XSSFRow row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        XSSFCell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        return cell;
    }

    private record SheetRange(XSSFSheet sheet, CellRangeAddress range) {
    }
}
