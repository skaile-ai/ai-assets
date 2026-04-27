package com.skaile.excelmcp.suites;

import static org.assertj.core.api.Assertions.assertThat;

import com.skaile.excelmcp.testsupport.TestSuiteRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Discovers every suite directory under {@code src/test/resources/test-suites/} and runs it through
 * {@link TestSuiteRunner}. Adding a new regression case is "drop a folder in" — no new Java. Suites
 * that need extensions get an explicit {@code @Test} method instead (and are skipped by the
 * factory). See {@code deterministic-test-suites-plan.md} §4.
 */
class TestSuiteRegressionTest {

  private static final Path SUITE_ROOT = Path.of("src/test/resources/test-suites");

  private static final Set<String> SUITES_WITH_EXTENSIONS = Set.of("named-range-and-table-listing");

  @TestFactory
  Stream<DynamicTest> everySuiteInTheRegressionFolder() throws Exception {
    if (!Files.isDirectory(SUITE_ROOT)) return Stream.empty();
    return Files.list(SUITE_ROOT)
        .filter(Files::isDirectory)
        .filter(d -> !SUITES_WITH_EXTENSIONS.contains(d.getFileName().toString()))
        .sorted()
        .map(
            dir ->
                DynamicTest.dynamicTest(
                    dir.getFileName().toString(), () -> TestSuiteRunner.runSuite(dir)));
  }

  @Test
  void namedRangeAndTableListingWithExtension() throws Exception {
    TestSuiteRunner.runSuite(
        SUITE_ROOT.resolve("named-range-and-table-listing"),
        TestSuiteRegressionTest::assertNamedRangeAndTableInventory);
  }

  /**
   * Extension hook demo. The comparator only checks rendered cell values + table name/row count —
   * if a future edit silently drops the {@code first_item} named range, the diff would still pass.
   * This extension closes that gap by asserting the full table + named-range inventory against a
   * recorded snapshot.
   */
  private static void assertNamedRangeAndTableInventory(Workbook expected, Workbook actual) {
    assertThat(tableInventory(actual)).containsExactly("Stock@Inventory!A1:B3");
    assertThat(namedRangeInventory(actual)).containsExactly("first_item -> Inventory!$A$2");
  }

  private static List<String> tableInventory(Workbook wb) {
    List<String> out = new ArrayList<>();
    for (Sheet s : wb) {
      if (s instanceof XSSFSheet xs) {
        for (XSSFTable t : xs.getTables()) {
          out.add(t.getName() + "@" + xs.getSheetName() + "!" + t.getCTTable().getRef());
        }
      }
    }
    return out;
  }

  private static List<String> namedRangeInventory(Workbook wb) {
    List<String> out = new ArrayList<>();
    for (Name n : wb.getAllNames()) {
      out.add(n.getNameName() + " -> " + n.getRefersToFormula());
    }
    return out;
  }
}
