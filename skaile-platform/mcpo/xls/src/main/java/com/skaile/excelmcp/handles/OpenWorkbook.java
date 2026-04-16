package com.skaile.excelmcp.handles;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Registry entry for an open workbook — metadata only (§5.2, reinterpreted per §4.1/§11b.5).
 *
 * <p>The plan's §5.2 sketch placed the POI {@code XSSFWorkbook} directly on this record. That would
 * leak POI types into {@code handles/} and through to {@code tools/}, violating the §4.1 layer
 * rules and the §11b.5 rule "POI types leaking into the tool layer or response shapes is ruled
 * out". Instead the engine layer owns POI workbooks keyed by {@link HandleId}; this record carries
 * only metadata and is safe to expose to tools.
 *
 * <p>PLAN-DEFER: deviate from §5.2 record shape — see {@code excel-mcp-server-future-work.md}.
 */
public record OpenWorkbook(HandleId id, Path sourcePath, String format, Instant openedAt) {}
