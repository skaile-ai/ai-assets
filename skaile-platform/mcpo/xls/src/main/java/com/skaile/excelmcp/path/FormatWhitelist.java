package com.skaile.excelmcp.path;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Extension-based format whitelist. Accepts {@code .xlsx}, {@code .xlsm}, {@code .xls}; rejects
 * {@code .xlsb} with the future-friendly message from §1.4. Extension checking only — format
 * mismatches (say, a JPG renamed to .xlsx) surface at POI load time.
 *
 * <p>Error {@code details} only expose the agent-supplied input string (tautologically safe) and
 * the extension parsed from it; the server-normalised absolute path never enters the envelope. The
 * engine-internal {@link #format(Path)} overload is called on already-validated paths and reports
 * just the file basename in {@code details.path} for the same reason.
 */
public final class FormatWhitelist {

  private static final Set<String> ACCEPTED = Set.of("xlsx", "xlsm", "xls");
  private static final String XLSB_HINT =
      ".xlsb is not supported in v1 (future expansion: calamine read or LibreOffice convert-on-open)";

  private FormatWhitelist() {}

  /** Agent-facing overload: echoes the agent's original {@code input} string in {@code details}. */
  public static String format(Path path, String input) throws McpException {
    return formatInternal(path, input);
  }

  /**
   * Engine-internal overload for paths that have already been through {@link PathValidator}. Uses
   * the file basename in {@code details.path} so the server-normalised absolute path never leaks.
   */
  public static String format(Path path) throws McpException {
    String basename = path.getFileName() == null ? "" : path.getFileName().toString();
    return formatInternal(path, basename);
  }

  private static String formatInternal(Path path, String detailsPath) throws McpException {
    String name = path.getFileName() == null ? "" : path.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    if (ext.equals("xlsb")) {
      throw new McpException(
          ErrorCode.FORMAT_UNSUPPORTED, XLSB_HINT, Map.of("path", detailsPath, "extension", ext));
    }
    if (!ACCEPTED.contains(ext)) {
      throw new McpException(
          ErrorCode.FORMAT_UNSUPPORTED,
          "unsupported file extension: " + ext,
          Map.of("path", detailsPath, "extension", ext, "accepted", ACCEPTED));
    }
    return ext;
  }
}
