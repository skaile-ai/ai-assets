#!/usr/bin/env bun
/**
 * CI build script for catalog compilation.
 * Usage: bun run ai-assets/catalogs/build.ts
 *
 * Compiles all .catalog.yaml manifests, validates the output,
 * writes catalog.json files to ai-assets/catalogs/dist/, and
 * reports any orphan skills, warnings, or validation errors.
 */

import { resolve } from "node:path";
import { compileAndWrite, validateCatalog } from "../../agent-framework/asset-manager/src/catalog-compiler.ts";

const catalogsDir = resolve(import.meta.dir);
const assetsRoot = resolve(catalogsDir, "..");

console.log("Compiling catalogs...");
console.log(`  Manifests: ${catalogsDir}`);
console.log(`  Assets:    ${assetsRoot}`);
console.log();

const result = compileAndWrite(catalogsDir, assetsRoot);

let hasErrors = false;

for (const [name, { catalog, warnings }] of result.results) {
  console.log(`=== ${name} === (${catalog.entries.length} entries)`);

  // Validate
  const validation = validateCatalog(catalog);
  if (!validation.valid) {
    hasErrors = true;
    for (const err of validation.errors) {
      console.error(`  ERROR: ${err}`);
    }
  }
  for (const w of validation.warnings) {
    console.warn(`  WARN:  ${w}`);
  }

  // Compile warnings
  if (warnings.length > 0) {
    for (const w of warnings) {
      console.warn(`  WARN:  ${w}`);
    }
  }
  console.log();
}

console.log(`Output: ${result.outputPaths.join(", ")}`);
console.log();

if (result.orphans.length > 0) {
  console.warn(`Orphan assets (not included in any catalog):`);
  for (const o of result.orphans) {
    console.warn(`  - ${o}`);
  }
  console.log();
}

if (hasErrors) {
  console.error("Catalog compilation failed with errors.");
  process.exit(1);
}

console.log("Catalog compilation complete.");
