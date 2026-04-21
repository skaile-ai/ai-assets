# named-range-and-table-listing

Fresh MCP-specific suite that also serves as the copy-paste example for `SuiteExtension`. Template carries a table `Stock` at `Inventory!A1:B3` and a workbook-scoped named range `first_item` pointing at `Inventory!$A$2`. `request.json` updates `Inventory!B2` from 3 to 7; expected is the same workbook shape with `B2=7`.

Driven by an explicit `@Test` method (`namedRangeAndTableListingWithExtension`) in `TestSuiteRegressionTest`, not by the `@TestFactory` (which skips this directory to avoid running the suite twice). The attached `SuiteExtension` asserts that after the write:

- the table inventory is still `[Stock @ Inventory!A1:B3]`,
- the named-range inventory is still `[first_item -> Inventory!$A$2]`.

The extension is the point: the `WorkbookDiff` comparator doesn't check the named-range inventory at all, and checks tables only by name + row count — so a regression that silently drops `first_item` would pass the diff. This suite closes that gap.

Regenerate `template.xlsx` + `expected.xlsx` with this jshell snippet (run from `skaile-platform/mcpo/xls/`):

```
./mvnw dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q && jshell --class-path "$(cat /tmp/cp.txt)" - <<'JSHELL'
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.*;
import java.io.*;

void build(String path, double widgetQty) throws Exception {
  try (XSSFWorkbook wb = new XSSFWorkbook(); FileOutputStream os = new FileOutputStream(path)) {
    XSSFSheet s = wb.createSheet("Inventory");
    Row h = s.createRow(0);
    h.createCell(0).setCellValue("Item");
    h.createCell(1).setCellValue("Qty");
    Row r1 = s.createRow(1);
    r1.createCell(0).setCellValue("Widget");
    r1.createCell(1).setCellValue(widgetQty);
    Row r2 = s.createRow(2);
    r2.createCell(0).setCellValue("Gadget");
    r2.createCell(1).setCellValue(5.0);
    XSSFTable t = s.createTable(new AreaReference("A1:B3", wb.getSpreadsheetVersion()));
    t.setName("Stock");
    t.setDisplayName("Stock");
    Name n = wb.createName();
    n.setNameName("first_item");
    n.setRefersToFormula("Inventory!$A$2");
    wb.write(os);
  }
}
build("src/test/resources/test-suites/named-range-and-table-listing/template.xlsx", 3.0);
build("src/test/resources/test-suites/named-range-and-table-listing/expected.xlsx", 7.0);
/exit
JSHELL
```
