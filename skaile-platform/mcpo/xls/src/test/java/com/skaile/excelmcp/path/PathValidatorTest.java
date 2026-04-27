package com.skaile.excelmcp.path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for the §6.1 check order:
 *
 * <pre>
 *   parse → containment → format → file-existence
 * </pre>
 *
 * <p>The previous implementation checked existence before containment, so any file outside the
 * sandbox fell through as either FILE_NOT_FOUND or FORMAT_UNSUPPORTED and PATH_OUTSIDE_ROOT was
 * effectively dead code.
 */
class PathValidatorTest {

  private static PathValidator validatorFor(Path root) throws McpException {
    return new PathValidator(ExcelMcpRoot.resolve(Optional.of(root)));
  }

  @Test
  void existingFileOutsideRootIsRejectedAsOutsideRoot(@TempDir Path root, @TempDir Path elsewhere)
      throws Exception {
    Path outside = Files.writeString(elsewhere.resolve("leak.xlsx"), "stub");
    PathValidator v = validatorFor(root);

    assertThatThrownBy(() -> v.validateExisting(outside.toString()))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.PATH_OUTSIDE_ROOT);
  }

  @Test
  void nonexistentPathOutsideRootIsRejectedAsOutsideRootNotFileNotFound(
      @TempDir Path root, @TempDir Path elsewhere) throws Exception {
    Path outside = elsewhere.resolve("nope.xlsx"); // never created
    PathValidator v = validatorFor(root);

    assertThatThrownBy(() -> v.validateExisting(outside.toString()))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.PATH_OUTSIDE_ROOT);
  }

  @Test
  void dotdotEscapeIsRejectedAsOutsideRoot(@TempDir Path root, @TempDir Path elsewhere)
      throws Exception {
    Path victim = Files.writeString(elsewhere.resolve("secret"), "stub");
    // Build a dotdot-escape string that normalises to `victim`.
    String raw = root.resolve("..").resolve(elsewhere.getFileName()).resolve("secret").toString();
    PathValidator v = validatorFor(root);

    assertThatThrownBy(() -> v.validateExisting(raw))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.PATH_OUTSIDE_ROOT);
    // And the victim still exists — we didn't touch it.
    assertThat(Files.exists(victim)).isTrue();
  }

  @Test
  void nonexistentPathInsideRootStillReturnsFileNotFound(@TempDir Path root) throws Exception {
    Path insideButMissing = root.resolve("doesnt-exist.xlsx");
    PathValidator v = validatorFor(root);

    assertThatThrownBy(() -> v.validateExisting(insideButMissing.toString()))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.FILE_NOT_FOUND);
  }

  @Test
  void formatCheckIsBypassedByContainmentFailure(@TempDir Path root, @TempDir Path elsewhere)
      throws Exception {
    // An existing file with a bad extension outside the sandbox: containment must win over format.
    Path bad = Files.writeString(elsewhere.resolve("escape.notanxlsx"), "stub");
    PathValidator v = validatorFor(root);

    assertThatThrownBy(() -> v.validateExisting(bad.toString()))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.PATH_OUTSIDE_ROOT);
  }

  @Test
  void destinationInsideRootIsAcceptedEvenWhenMissing(@TempDir Path root) throws Exception {
    Path newFile = root.resolve("fresh.xlsx");
    PathValidator v = validatorFor(root);

    assertThat(v.validateDestination(newFile.toString())).isEqualTo(newFile);
  }

  @Test
  void destinationOutsideRootIsRejected(@TempDir Path root, @TempDir Path elsewhere)
      throws Exception {
    Path newOutside = elsewhere.resolve("fresh.xlsx");
    PathValidator v = validatorFor(root);

    assertThatThrownBy(() -> v.validateDestination(newOutside.toString()))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.PATH_OUTSIDE_ROOT);
  }

  @Test
  void rootUnsetAcceptsAnyExistingPath(@TempDir Path elsewhere) throws Exception {
    Path anywhere = Files.writeString(elsewhere.resolve("free.xlsx"), "stub");
    PathValidator v = new PathValidator(ExcelMcpRoot.resolve(Optional.empty()));

    assertThat(v.validateExisting(anywhere.toString())).isEqualTo(anywhere);
  }

  @Test
  void pathOutsideRootDoesNotLeakRoot(@TempDir Path root, @TempDir Path elsewhere)
      throws Exception {
    Path outside = Files.writeString(elsewhere.resolve("leak.xlsx"), "stub");
    String input = outside.toString();
    PathValidator v = validatorFor(root);

    McpException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            McpException.class, () -> v.validateExisting(input));
    assertThat(ex.code()).isEqualTo(ErrorCode.PATH_OUTSIDE_ROOT);
    assertThat(ex.details().keySet()).containsExactly("path");
    assertThat(ex.details().get("path")).isEqualTo(input);
  }

  @Test
  void formatUnsupportedDetailsEchoAgentInputNotCanonicalPath(@TempDir Path root) throws Exception {
    // Build an input string with a redundant ./ segment so the agent input differs from the
    // server-normalised absolute path; the envelope must still echo the agent's own form.
    String input = root + "/./report.txt";
    PathValidator v = validatorFor(root);

    McpException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            McpException.class, () -> v.validateDestination(input));
    assertThat(ex.code()).isEqualTo(ErrorCode.FORMAT_UNSUPPORTED);
    assertThat(ex.details()).containsEntry("path", input);
    assertThat(ex.details().get("path").toString()).contains("/./");
  }
}
