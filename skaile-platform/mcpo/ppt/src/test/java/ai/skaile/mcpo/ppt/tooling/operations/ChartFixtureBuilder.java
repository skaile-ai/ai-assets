package ai.skaile.mcpo.ppt.tooling.operations;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.BarGrouping;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 * Test helper that writes a one-slide clustered column chart PPTX to disk.
 * The chart has 3 series (Series 1..3) × 4 categories (Q1..Q4); the values
 * and categories are cell-range-backed so the embedded XLSX workbook holds
 * the source of truth and {@code list_charts} / {@code update_chart_data}
 * can be exercised against a realistic sample.
 *
 * <p>Lives in the test source tree so production jars stay free of sample
 * generation code. Generates the fixture on demand in a temp path rather than
 * committing a binary under {@code src/test/resources} — this keeps every
 * chart structural change self-documenting via this Java source.
 */
public final class ChartFixtureBuilder {
    private static final String CHART_URI = "http://schemas.openxmlformats.org/drawingml/2006/chart";
    private static final String REL_URI = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    public static final String[] CATEGORIES = {"Q1", "Q2", "Q3", "Q4"};
    public static final String[] SERIES_NAMES = {"Series 1", "Series 2", "Series 3"};
    public static final double[][] SERIES_VALUES = {
            {10.0, 20.0, 30.0, 40.0},
            {15.0, 25.0, 35.0, 45.0},
            {5.0, 10.0, 15.0, 20.0}
    };

    private ChartFixtureBuilder() {
    }

    public static Path writeClusteredColumn(Path targetPptx) throws Exception {
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            XSLFSlide slide = pptx.createSlide();
            XSLFChart chart = pptx.createChart();

            POIXMLDocumentPart.RelationPart rel =
                    slide.addRelation(null, XSLFRelation.CHART, chart);
            String rId = rel.getRelationship().getId();
            addChartGraphicFrame(slide, rId);

            XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);

            XSSFSheet sheet = chart.getWorkbook().getSheetAt(0);
            populateWorkbook(sheet);

            XDDFBarChartData barData = (XDDFBarChartData) chart.createData(
                    ChartTypes.BAR, categoryAxis, valueAxis);
            barData.setBarDirection(BarDirection.COL);
            barData.setBarGrouping(BarGrouping.CLUSTERED);

            CellRangeAddress catRange = new CellRangeAddress(1, 4, 0, 0);
            XDDFCategoryDataSource catSource =
                    XDDFDataSourcesFactory.fromStringCellRange(sheet, catRange);

            for (int s = 0; s < SERIES_NAMES.length; s++) {
                CellRangeAddress valRange = new CellRangeAddress(1, 4, s + 1, s + 1);
                XDDFNumericalDataSource<Double> valSource =
                        XDDFDataSourcesFactory.fromNumericCellRange(sheet, valRange);
                XDDFBarChartData.Series series =
                        (XDDFBarChartData.Series) barData.addSeries(catSource, valSource);
                CellReference titleRef =
                        new CellReference(sheet.getSheetName(), 0, s + 1, true, true);
                series.setTitle(SERIES_NAMES[s], titleRef);
            }
            chart.plot(barData);

            try (OutputStream out = Files.newOutputStream(targetPptx)) {
                pptx.write(out);
            }
        }
        return targetPptx;
    }

    private static void populateWorkbook(XSSFSheet sheet) {
        XSSFRow header = row(sheet, 0);
        cell(header, 0).setCellValue("");
        for (int s = 0; s < SERIES_NAMES.length; s++) {
            cell(header, s + 1).setCellValue(SERIES_NAMES[s]);
        }
        for (int i = 0; i < CATEGORIES.length; i++) {
            XSSFRow row = row(sheet, i + 1);
            cell(row, 0).setCellValue(CATEGORIES[i]);
            for (int s = 0; s < SERIES_VALUES.length; s++) {
                cell(row, s + 1).setCellValue(SERIES_VALUES[s][i]);
            }
        }
    }

    private static XSSFRow row(XSSFSheet sheet, int r) {
        XSSFRow row = sheet.getRow(r);
        return row == null ? sheet.createRow(r) : row;
    }

    private static XSSFCell cell(XSSFRow row, int c) {
        XSSFCell cell = row.getCell(c);
        return cell == null ? row.createCell(c) : cell;
    }

    /**
     * Add a {@code <p:graphicFrame>} referencing the chart's relationship id. POI's
     * {@code XSLFChart.prototype} is package-private, so we author the same XML
     * manually against the slide's {@code spTree}. Coordinates are in EMUs
     * (914400 per inch); the frame sits ~1in from the top-left and spans 6.7in × 5in.
     */
    private static void addChartGraphicFrame(XSLFSlide slide, String rId) {
        var spTree = slide.getXmlObject().getCSld().getSpTree();
        var frame = spTree.addNewGraphicFrame();

        var nvGr = frame.addNewNvGraphicFramePr();
        var cnv = nvGr.addNewCNvPr();
        cnv.setId(2);
        cnv.setName("Chart 1");
        nvGr.addNewCNvGraphicFramePr().addNewGraphicFrameLocks().setNoGrp(true);
        nvGr.addNewNvPr();

        var xfrm = frame.addNewXfrm();
        var off = xfrm.addNewOff();
        off.setX(914400L);
        off.setY(914400L);
        var ext = xfrm.addNewExt();
        ext.setCx(6096000L);
        ext.setCy(4572000L);

        var gr = frame.addNewGraphic().addNewGraphicData();
        try (var cur = gr.newCursor()) {
            cur.toNextToken();
            cur.beginElement(new QName(CHART_URI, "chart"));
            cur.insertAttributeWithValue("id", REL_URI, rId);
        }
        gr.setUri(CHART_URI);
    }

    /**
     * Convenience for tests that only need a path and don't care where it lives —
     * writes the fixture into a JUnit-managed temp directory.
     */
    public static Path writeDefault(Path tempDir) throws IOException {
        try {
            return writeClusteredColumn(tempDir.resolve("chart-sample.pptx"));
        } catch (IOException io) {
            throw io;
        } catch (Exception e) {
            throw new IOException("Failed to build chart fixture", e);
        }
    }
}
