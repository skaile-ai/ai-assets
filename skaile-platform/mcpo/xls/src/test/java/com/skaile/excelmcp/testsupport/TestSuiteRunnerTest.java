package com.skaile.excelmcp.testsupport;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

/**
 * Covers the non-happy paths of {@link TestSuiteRunner}. The happy path is exercised suite-by-suite
 * via {@code TestSuiteRegressionTest}; here we just pin the error messages so authoring mistakes
 * surface with enough context to fix without reading the runner source.
 */
class TestSuiteRunnerTest {

  @Test
  void reportsMissingTemplate(@TempDir Path dir) {
    assertThatThrownBy(() -> TestSuiteRunner.runSuite(dir))
        .isInstanceOf(AssertionFailedError.class)
        .hasMessageContaining("missing template.xlsx");
  }

  @Test
  void reportsMissingRequest(@TempDir Path dir) throws Exception {
    Files.createFile(dir.resolve("template.xlsx"));
    assertThatThrownBy(() -> TestSuiteRunner.runSuite(dir))
        .isInstanceOf(AssertionFailedError.class)
        .hasMessageContaining("missing request.json");
  }

  @Test
  void reportsMissingExpected(@TempDir Path dir) throws Exception {
    Files.createFile(dir.resolve("template.xlsx"));
    Files.writeString(dir.resolve("request.json"), "{\"operations\": []}");
    assertThatThrownBy(() -> TestSuiteRunner.runSuite(dir))
        .isInstanceOf(AssertionFailedError.class)
        .hasMessageContaining("missing expected.xlsx");
  }

  @Test
  void reportsMalformedRequest(@TempDir Path dir) throws Exception {
    Files.createFile(dir.resolve("template.xlsx"));
    Files.createFile(dir.resolve("expected.xlsx"));
    Files.writeString(dir.resolve("request.json"), "not json");
    assertThatThrownBy(() -> TestSuiteRunner.runSuite(dir))
        .isInstanceOf(AssertionFailedError.class)
        .hasMessageContaining("malformed request.json");
  }
}
