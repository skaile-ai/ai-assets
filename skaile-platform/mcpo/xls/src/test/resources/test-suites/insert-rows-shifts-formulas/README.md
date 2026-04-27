# insert-rows-shifts-formulas

Fresh MCP-specific suite. Row mutation is the highest-silent-regression-risk area: POI's `Sheet.shiftRows` both moves cell data and rewrites formula references, and either half can quietly break without surfacing as an exception. This suite inserts two rows above a formula region (`=A1+A2` at A3) and checks that after recalc the formula's cached value is still 30 — proving both the data shift and the reference rewrite happened.

Regenerate `template.xlsx` + `expected.xlsx` with this jshell snippet (run from `skaile-platform/mcpo/xls/`):

```
./mvnw dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q && jshell --class-path "$(cat /tmp/cp.txt)" - <<'JSHELL'
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import java.io.*;

void build(String path, int dataStartRow0Based, String formulaRef) throws Exception {
  try (XSSFWorkbook wb = new XSSFWorkbook(); FileOutputStream os = new FileOutputStream(path)) {
    XSSFSheet s = wb.createSheet("Sheet1");
    s.createRow(dataStartRow0Based).createCell(0).setCellValue(10.0);
    s.createRow(dataStartRow0Based + 1).createCell(0).setCellValue(20.0);
    Cell c = s.createRow(dataStartRow0Based + 2).createCell(0);
    c.setCellFormula(formulaRef);
    wb.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(c);
    wb.write(os);
  }
}
build("src/test/resources/test-suites/insert-rows-shifts-formulas/template.xlsx", 0, "A1+A2");
build("src/test/resources/test-suites/insert-rows-shifts-formulas/expected.xlsx", 2, "A3+A4");
/exit
JSHELL
```
