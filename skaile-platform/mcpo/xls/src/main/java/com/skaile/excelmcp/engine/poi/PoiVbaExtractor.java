package com.skaile.excelmcp.engine.poi;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.shape.VbaModuleShape;
import com.skaile.excelmcp.shape.VbaModuleSourceShape;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.poifs.macros.Module;
import org.apache.poi.poifs.macros.VBAMacroReader;

/**
 * Read-only VBA module extraction via POI's {@link VBAMacroReader} (lives in {@code
 * poi-scratchpad}). Confined to this class — no VBA-adjacent POI import appears anywhere else in
 * {@code engine/}, and no MCP/tool class imports {@code poi-scratchpad} directly.
 *
 * <p>v1 reads VBA from the workbook's source file on disk. A workbook created via {@code
 * workbook.create()} with no source path always reports {@link ErrorCode#VBA_NOT_PRESENT}, which is
 * accurate — POI has no API for authoring VBA in-memory.
 */
public final class PoiVbaExtractor {

  private PoiVbaExtractor() {}

  /** Wire value for {@link VbaModuleShape#type()} given POI's module type. */
  private static String wireType(Module.ModuleType type) {
    if (type == null) {
      return "module";
    }
    return switch (type) {
      case Document -> "document";
      case Class -> "class";
      case Module -> "module";
    };
  }

  /**
   * List every VBA module in the workbook referenced by {@code sourcePath}. {@code sourcePath} may
   * be {@code null} (for workbooks created via {@code workbook.create()} without a source) — in
   * that case the call raises {@link ErrorCode#VBA_NOT_PRESENT}.
   */
  public static List<VbaModuleShape> listModules(Path sourcePath) throws McpException {
    Map<String, Module> modules = readModulesOrFail(sourcePath);
    List<VbaModuleShape> out = new ArrayList<>(modules.size());
    for (Map.Entry<String, Module> e : modules.entrySet()) {
      out.add(new VbaModuleShape(e.getKey(), wireType(e.getValue().geModuleType())));
    }
    return out;
  }

  /**
   * Return the module with the given name (case-insensitive lookup) along with its source text.
   * Raises {@link ErrorCode#VBA_NOT_PRESENT} when the workbook has no VBA project, or {@link
   * ErrorCode#VBA_MODULE_NOT_FOUND} when no module matches the name.
   */
  public static VbaModuleSourceShape getModule(Path sourcePath, String name) throws McpException {
    Map<String, Module> modules = readModulesOrFail(sourcePath);
    String lc = name.toLowerCase(Locale.ROOT);
    for (Map.Entry<String, Module> e : modules.entrySet()) {
      if (e.getKey().toLowerCase(Locale.ROOT).equals(lc)) {
        Module mod = e.getValue();
        return new VbaModuleSourceShape(e.getKey(), wireType(mod.geModuleType()), mod.getContent());
      }
    }
    throw new McpException(
        ErrorCode.VBA_MODULE_NOT_FOUND,
        "VBA module not found: " + name,
        Map.of("name", name, "available_modules", new ArrayList<>(modules.keySet())));
  }

  private static Map<String, Module> readModulesOrFail(Path sourcePath) throws McpException {
    if (sourcePath == null) {
      throw new McpException(
          ErrorCode.VBA_NOT_PRESENT,
          "workbook has no source path; VBA extraction requires a file on disk",
          Map.of());
    }
    File file = sourcePath.toFile();
    if (!file.isFile()) {
      throw new McpException(
          ErrorCode.VBA_NOT_PRESENT,
          "workbook source file not accessible: " + sourcePath,
          Map.of("path", sourcePath.toString()));
    }
    try (VBAMacroReader reader = new VBAMacroReader(file)) {
      Map<String, Module> modules = reader.readMacroModules();
      // VBAMacroReader returns an empty map (not null) when the file has no vbaProject.bin OR
      // when the package is present but holds zero modules. Either way, "no modules to show" is
      // the VBA_NOT_PRESENT contract per plan §9.7.
      if (modules == null || modules.isEmpty()) {
        throw new McpException(
            ErrorCode.VBA_NOT_PRESENT,
            "workbook has no VBA project: " + sourcePath.getFileName(),
            Map.of("path", sourcePath.toString()));
      }
      return new LinkedHashMap<>(modules);
    } catch (IOException ex) {
      // Missing vbaProject.bin part or malformed binary — both surface as VBA_NOT_PRESENT rather
      // than INTERNAL_ERROR, so the agent can handle the common case uniformly.
      throw new McpException(
          ErrorCode.VBA_NOT_PRESENT,
          "workbook does not expose readable VBA: "
              + sourcePath.getFileName()
              + " ("
              + ex.getMessage()
              + ")",
          Map.of("path", sourcePath.toString(), "exception", ex.getClass().getSimpleName()),
          ex);
    }
  }
}
