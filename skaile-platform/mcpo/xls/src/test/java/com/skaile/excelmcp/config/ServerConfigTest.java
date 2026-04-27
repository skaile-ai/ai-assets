package com.skaile.excelmcp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Environment-variable parsing tests. {@link ServerConfig#fromEnvironment()} is exercised through a
 * test seam that takes a {@code Function<String,String>} so we don't need to mutate the live
 * process environment.
 */
class ServerConfigTest {

  private static ServerConfig parse(Map<String, String> env) {
    return ServerConfig.fromEnvironment(env::get);
  }

  @Test
  void rootSetAllowUnsandboxedUnsetProducesSandboxedConfig() {
    ServerConfig cfg = parse(Map.of("EXCEL_MCP_ROOT", "/data"));
    assertThat(cfg.excelMcpRoot()).contains(Path.of("/data"));
    assertThat(cfg.allowUnsandboxed()).isFalse();
  }

  @Test
  void bothRootAndAllowUnsandboxedProduceSandboxedConfigRootWins() {
    ServerConfig cfg =
        parse(Map.of("EXCEL_MCP_ROOT", "/data", "EXCEL_MCP_ALLOW_UNSANDBOXED", "true"));
    assertThat(cfg.excelMcpRoot()).contains(Path.of("/data"));
    assertThat(cfg.allowUnsandboxed()).isTrue();
  }

  @Test
  void allowUnsandboxedTrueWithNoRootIsReflectedAsEmptyRoot() {
    ServerConfig cfg = parse(Map.of("EXCEL_MCP_ALLOW_UNSANDBOXED", "true"));
    assertThat(cfg.excelMcpRoot()).isEmpty();
    assertThat(cfg.allowUnsandboxed()).isTrue();
  }

  @Test
  void neitherRootNorAllowUnsandboxedIsReflectedAsFailClosedInputs() {
    ServerConfig cfg = parse(Map.of());
    assertThat(cfg.excelMcpRoot()).isEmpty();
    assertThat(cfg.allowUnsandboxed()).isFalse();
  }

  @Test
  void allowUnsandboxedOnlyAcceptsExactlyTrue() {
    assertThat(parse(Map.of("EXCEL_MCP_ALLOW_UNSANDBOXED", "1")).allowUnsandboxed()).isFalse();
    assertThat(parse(Map.of("EXCEL_MCP_ALLOW_UNSANDBOXED", "yes")).allowUnsandboxed()).isFalse();
    assertThat(parse(Map.of("EXCEL_MCP_ALLOW_UNSANDBOXED", "TRUE")).allowUnsandboxed()).isFalse();
    assertThat(parse(Map.of("EXCEL_MCP_ALLOW_UNSANDBOXED", "")).allowUnsandboxed()).isFalse();
    assertThat(parse(Map.of("EXCEL_MCP_ALLOW_UNSANDBOXED", "true")).allowUnsandboxed()).isTrue();
  }

  @Test
  void blankRootIsTreatedAsUnset() {
    ServerConfig cfg =
        parse(
            Map.of(
                "EXCEL_MCP_ROOT", "   ",
                "EXCEL_MCP_ALLOW_UNSANDBOXED", "true"));
    assertThat(cfg.excelMcpRoot()).isEmpty();
  }

  @Test
  void maxFileBytesParsesWhenValid() {
    ServerConfig cfg =
        parse(
            Map.of(
                "EXCEL_MCP_ALLOW_UNSANDBOXED", "true",
                "EXCEL_MCP_MAX_FILE_BYTES", "12345"));
    assertThat(cfg.maxFileBytes()).isEqualTo(12345L);
  }

  @Test
  void maxFileBytesThrowsOnMalformedValue() {
    Map<String, String> env = new HashMap<>();
    env.put("EXCEL_MCP_ALLOW_UNSANDBOXED", "true");
    env.put("EXCEL_MCP_MAX_FILE_BYTES", "twelve");
    assertThatThrownBy(() -> parse(env)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void maxCellsFallsBackToDefaultWhenMissing() {
    ServerConfig cfg = parse(Map.of("EXCEL_MCP_ALLOW_UNSANDBOXED", "true"));
    assertThat(cfg.maxCells()).isEqualTo(ServerConfig.DEFAULT_MAX_CELLS);
    assertThat(cfg.maxFileBytes()).isEqualTo(ServerConfig.DEFAULT_MAX_FILE_BYTES);
  }
}
