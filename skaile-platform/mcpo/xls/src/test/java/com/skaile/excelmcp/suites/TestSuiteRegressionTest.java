package com.skaile.excelmcp.suites;

import com.skaile.excelmcp.testsupport.TestSuiteRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Discovers every suite directory under {@code src/test/resources/test-suites/} and runs it through
 * {@link TestSuiteRunner}. Adding a new regression case is "drop a folder in" — no new Java. See
 * {@code deterministic-test-suites-plan.md} §4.
 */
class TestSuiteRegressionTest {

  private static final Path SUITE_ROOT = Path.of("src/test/resources/test-suites");

  @TestFactory
  Stream<DynamicTest> everySuiteInTheRegressionFolder() throws Exception {
    if (!Files.isDirectory(SUITE_ROOT)) return Stream.empty();
    return Files.list(SUITE_ROOT)
        .filter(Files::isDirectory)
        .sorted()
        .map(
            dir ->
                DynamicTest.dynamicTest(
                    dir.getFileName().toString(), () -> TestSuiteRunner.runSuite(dir)));
  }
}
