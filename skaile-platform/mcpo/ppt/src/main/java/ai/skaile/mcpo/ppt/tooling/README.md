# tooling package layout

This directory is organized by role to keep `PptToolService` as a readable facade.

## top level
- `PptToolService.java`: tool registry + dispatch map + shared orchestration.

## operations/
- `PptDocumentOperations.java`: document lifecycle and transaction tool delegation.
- `PptSlideOperations.java`: slide/content mutation tool delegation.
- `PptRenderService.java`: rendering and analysis tool delegation.
- `PptTemplateService.java`: templates/import/export/media/data tool delegation.

## contracts/
- `ToolHandler.java`: functional handler interface.
- `ToolDefinition.java`: tool metadata/schema contract.
- `ToolCallResult.java`: standardized tool call result record.

## infra/
- `ToolArgumentValidator.java`: argument normalization and schema checks.
- `ToolResponseFactory.java`: success/error payload construction.
- `PptPathResolver.java`: safe path resolution and format inference.

Notes:
- Package names intentionally remain `ai.skaile.mcpo.ppt.tooling` for compatibility.
- Files are grouped physically for discoverability, without API changes.
