# sheet-rename-rewrites-formulas

Fresh MCP-specific suite. Cross-sheet formula on `Sheet2!A1` references `Sheet1`; after `sheet.rename Sheet1 → Data` and `workbook.recalculate`, POI must rewrite the formula to reference `Data!A1+Data!A2` and the cached value must remain 30. README.md:59 claims POI rewrites formulas on rename — this suite proves it.

Regenerate `template.xlsx` + `expected.xlsx` with this jshell snippet (run from `skaile-platform/mcpo/xls/`):

```
./mvnw dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q && jshell --class-path "$(cat /tmp/cp.txt)" - <<'JSHELL'
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import java.io.*;

void build(String path, String firstSheetName) throws Exception {
  try (XSSFWorkbook wb = new XSSFWorkbook(); FileOutputStream os = new FileOutputStream(path)) {
    XSSFSheet s1 = wb.createSheet(firstSheetName);
    s1.createRow(0).createCell(0).setCellValue(10.0);
    s1.createRow(1).createCell(0).setCellValue(20.0);
    XSSFSheet s2 = wb.createSheet("Sheet2");
    Cell c = s2.createRow(0).createCell(0);
    c.setCellFormula(firstSheetName + "!A1+" + firstSheetName + "!A2");
    wb.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(c);
    wb.write(os);
  }
}
build("src/test/resources/test-suites/sheet-rename-rewrites-formulas/template.xlsx", "Sheet1");
build("src/test/resources/test-suites/sheet-rename-rewrites-formulas/expected.xlsx", "Data");
/exit
JSHELL
```
