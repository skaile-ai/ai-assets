#!/usr/bin/env bun
/**
 * doc-tracker.ts — Core data collection for skaildev-doc
 *
 * Scans source files for @doc: annotations and Starlight pages for _sources
 * frontmatter. Builds bidirectional maps and checks staleness via git.
 *
 * Usage: bun doc-tracker.ts [--root <monorepo-root>] [--scope <path-prefix>]
 * Output: JSON to stdout
 */

import { execSync } from "node:child_process";
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface Annotation {
	file: string;
	line: number;
	verb: "important" | "skip" | "see";
	note: string;
	seeTarget?: string;
}

interface SourceRef {
	path: string;
	sections: string[];
	description: string;
}

interface DocPage {
	path: string;
	title: string;
	sources: SourceRef[];
	basedOnCommit: string | null;
	lastSynced: string | null;
	sourceHash: string | null;
	stale: boolean;
	diffSummary: string | null;
}

interface TrackerOutput {
	annotations: Annotation[];
	pages: DocPage[];
	sourceMap: Record<string, string[]>;
	reverseMap: Record<string, string[]>;
	untracked: string[];
}

// ---------------------------------------------------------------------------
// CLI argument parsing
// ---------------------------------------------------------------------------

function parseArgs(): { root: string; scope: string | null } {
	const args = process.argv.slice(2);
	let root = process.cwd();
	let scope: string | null = null;

	for (let i = 0; i < args.length; i++) {
		if (args[i] === "--root" && args[i + 1]) {
			root = resolve(args[++i]);
		} else if (args[i] === "--scope" && args[i + 1]) {
			scope = args[++i];
		}
	}

	return { root, scope };
}

// ---------------------------------------------------------------------------
// File walking
// ---------------------------------------------------------------------------

const SOURCE_EXTENSIONS = new Set([
	".ts",
	".tsx",
	".js",
	".jsx",
	".vue",
	".py",
	".sh",
]);

const EXCLUDE_DIRS = new Set([
	"node_modules",
	"dist",
	".nuxt",
	".output",
	"__tests__",
	".git",
	".cache",
	"coverage",
]);

const EXCLUDE_DOC_GENERATED_DIR = "docs/src/content/docs/resources/";

function isExcludedSourceFile(filePath: string): boolean {
	return (
		filePath.endsWith(".test.ts") ||
		filePath.endsWith(".spec.ts") ||
		filePath.endsWith(".test.js") ||
		filePath.endsWith(".spec.js") ||
		filePath.endsWith(".test.tsx") ||
		filePath.endsWith(".spec.tsx")
	);
}

function walkSourceFiles(
	dir: string,
	root: string,
	scope: string | null,
): string[] {
	const results: string[] = [];

	function walk(current: string): void {
		let entries: string[];
		try {
			entries = readdirSync(current);
		} catch {
			return;
		}

		for (const entry of entries) {
			const fullPath = join(current, entry);
			let stat;
			try {
				stat = statSync(fullPath);
			} catch {
				continue;
			}

			if (stat.isDirectory()) {
				if (EXCLUDE_DIRS.has(entry)) continue;
				walk(fullPath);
			} else if (stat.isFile()) {
				const ext = entry.includes(".") ? `.${entry.split(".").pop()}` : "";
				if (!SOURCE_EXTENSIONS.has(ext)) continue;
				if (isExcludedSourceFile(entry)) continue;

				const relPath = relative(root, fullPath);

				// Apply scope filter
				if (scope && !relPath.startsWith(scope)) continue;

				results.push(relPath);
			}
		}
	}

	walk(dir);
	return results;
}

function walkDocFiles(root: string): string[] {
	const results: string[] = [];
	const docGlobs = [
		join(root, "docs/src/content/docs"),
		...findMatchingDirs(root, "agent-framework/*/docs"),
		...findMatchingDirs(root, "forge/*/docs"),
		join(root, "ai-resources/docs"),
	];

	for (const baseDir of docGlobs) {
		if (!existsSync(baseDir)) continue;
		walkMdFiles(baseDir, root, results);
	}

	return results;
}

function findMatchingDirs(root: string, pattern: string): string[] {
	// pattern like "agent-framework/*/docs"
	const parts = pattern.split("/");
	const wildcardIdx = parts.indexOf("*");

	if (wildcardIdx === -1) {
		return [join(root, pattern)];
	}

	const parentParts = parts.slice(0, wildcardIdx);
	const childParts = parts.slice(wildcardIdx + 1);
	const parentDir = join(root, ...parentParts);

	if (!existsSync(parentDir)) return [];

	let entries: string[];
	try {
		entries = readdirSync(parentDir);
	} catch {
		return [];
	}

	const results: string[] = [];
	for (const entry of entries) {
		const candidate = join(parentDir, entry, ...childParts);
		if (existsSync(candidate) && statSync(candidate).isDirectory()) {
			results.push(candidate);
		}
	}
	return results;
}

function walkMdFiles(dir: string, root: string, results: string[]): void {
	let entries: string[];
	try {
		entries = readdirSync(dir);
	} catch {
		return;
	}

	for (const entry of entries) {
		const fullPath = join(dir, entry);
		let stat;
		try {
			stat = statSync(fullPath);
		} catch {
			continue;
		}

		if (stat.isDirectory()) {
			// Skip generated resources dir
			const relPath = relative(root, fullPath);
			if (
				relPath === EXCLUDE_DOC_GENERATED_DIR.replace(/\/$/, "") ||
				relPath.startsWith(EXCLUDE_DOC_GENERATED_DIR)
			) {
				continue;
			}
			walkMdFiles(fullPath, root, results);
		} else if (
			stat.isFile() &&
			(entry.endsWith(".md") || entry.endsWith(".mdx"))
		) {
			const relPath = relative(root, fullPath);
			// Skip generated resources
			if (relPath.startsWith(EXCLUDE_DOC_GENERATED_DIR)) continue;
			results.push(relPath);
		}
	}
}

// ---------------------------------------------------------------------------
// @doc: annotation scanning
// ---------------------------------------------------------------------------

// Matches: // @doc: important - note
//          // @doc: skip - note
//          // @doc: see "Target" - note
//          # @doc: important - note  (Python/shell)
const ANNOTATION_RE =
	/(?:\/\/|#)\s*@doc:\s*(important|skip|see)\s*(?:"([^"]+)")?\s*(?:-\s*(.*))?$/;

function scanAnnotations(filePath: string, root: string): Annotation[] {
	const fullPath = join(root, filePath);
	let content: string;
	try {
		content = readFileSync(fullPath, "utf8");
	} catch {
		return [];
	}

	const lines = content.split("\n");
	const results: Annotation[] = [];

	for (let i = 0; i < lines.length; i++) {
		const line = lines[i];
		const match = line.match(ANNOTATION_RE);
		if (!match) continue;

		const verb = match[1] as Annotation["verb"];
		const seeTarget = match[2] ?? undefined;
		const note = match[3]?.trim() ?? "";

		results.push({
			file: filePath,
			line: i + 1,
			verb,
			note,
			...(seeTarget !== undefined ? { seeTarget } : {}),
		});
	}

	return results;
}

// ---------------------------------------------------------------------------
// Frontmatter parsing
// ---------------------------------------------------------------------------

interface ParsedFrontmatter {
	title: string;
	description: string;
	sources: SourceRef[];
	basedOnCommit: string | null;
	lastSynced: string | null;
	sourceHash: string | null;
}

/**
 * Simple frontmatter parser for the specific fields we need.
 * Handles YAML between --- markers. Does NOT need to be a full YAML parser.
 *
 * Handles:
 *   title: "..."
 *   description: "..."
 *   _based_on_commit: "..."
 *   _last_synced: "..."
 *   _source_hash: "..."
 *   _sources:
 *     - path: "..."
 *       sections: ["...", "..."]
 *       description: "..."
 */
function parseFrontmatter(content: string): ParsedFrontmatter | null {
	const match = content.match(/^---\r?\n([\s\S]*?)\r?\n---/);
	if (!match) return null;

	const yaml = match[1];
	const result: ParsedFrontmatter = {
		title: "",
		description: "",
		sources: [],
		basedOnCommit: null,
		lastSynced: null,
		sourceHash: null,
	};

	// Parse simple scalar fields
	result.title = extractScalar(yaml, "title") ?? "";
	result.description = extractScalar(yaml, "description") ?? "";
	result.basedOnCommit = extractScalar(yaml, "_based_on_commit");
	result.lastSynced = extractScalar(yaml, "_last_synced");
	result.sourceHash = extractScalar(yaml, "_source_hash");

	// Parse _sources array of objects
	result.sources = extractSources(yaml);

	return result;
}

/**
 * Extract a simple scalar value for a YAML key.
 * Handles: key: value, key: "value", key: 'value'
 */
function extractScalar(yaml: string, key: string): string | null {
	const re = new RegExp(
		`^${escapeRegex(key)}:\\s*(?:"([^"\\n]*)"|'([^'\\n]*)'|([^\\n#]*))`,
		"m",
	);
	const m = yaml.match(re);
	if (!m) return null;
	const val = (m[1] ?? m[2] ?? m[3] ?? "").trim();
	return val || null;
}

function escapeRegex(s: string): string {
	return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * Extract the _sources array from YAML frontmatter.
 *
 * Expected format:
 *   _sources:
 *     - path: "some/path.ts"
 *       sections:
 *         - "## Heading"
 *       description: "What this covers"
 *
 * Also handles inline sections array: sections: ["a", "b"]
 */
function extractSources(yaml: string): SourceRef[] {
	// Find the _sources block
	const sourcesMatch = yaml.match(/^_sources:\s*\n([\s\S]*?)(?=\n\S|\n?$)/m);
	if (!sourcesMatch) return [];

	const sourcesBlock = sourcesMatch[1];
	const sources: SourceRef[] = [];

	// Split into individual items (each starts with "  - ")
	const itemBlocks = sourcesBlock.split(/\n(?=  -\s)/);

	for (const block of itemBlocks) {
		const trimmed = block.trim();
		if (!trimmed || !trimmed.startsWith("-")) continue;

		const path = extractInlineScalar(trimmed, "path");
		if (!path) continue;

		const description = extractInlineScalar(trimmed, "description") ?? "";
		const sections = extractInlineSections(trimmed);

		sources.push({ path, sections, description });
	}

	return sources;
}

/** Extract scalar from a mini-block (relative indentation) */
function extractInlineScalar(block: string, key: string): string | null {
	const re = new RegExp(
		`\\b${escapeRegex(key)}:\\s*(?:"([^"\\n]*)"|'([^'\\n]*)'|([^\\n#]*))`,
	);
	const m = block.match(re);
	if (!m) return null;
	const val = (m[1] ?? m[2] ?? m[3] ?? "").trim();
	return val || null;
}

/** Extract sections array from a source item block */
function extractInlineSections(block: string): string[] {
	// Try inline array: sections: ["a", "b"] or sections: ['a', 'b']
	const inlineMatch = block.match(/\bsections:\s*\[([^\]]*)\]/);
	if (inlineMatch) {
		return inlineMatch[1]
			.split(",")
			.map((s) => s.trim().replace(/^["']|["']$/g, ""))
			.filter(Boolean);
	}

	// Try block array under sections:
	const blockMatch = block.match(/\bsections:\s*\n((?:\s+-\s+[^\n]+\n?)*)/);
	if (blockMatch) {
		return blockMatch[1]
			.split("\n")
			.map((l) =>
				l
					.replace(/^\s+-\s+/, "")
					.replace(/^["']|["']$/g, "")
					.trim(),
			)
			.filter(Boolean);
	}

	return [];
}

// ---------------------------------------------------------------------------
// Staleness detection
// ---------------------------------------------------------------------------

function checkStaleness(
	basedOnCommit: string,
	sourcePaths: string[],
	root: string,
): { stale: boolean; diffSummary: string | null } {
	if (!sourcePaths.length) {
		return { stale: false, diffSummary: null };
	}

	try {
		// First verify the commit exists
		execSync(`git -C "${root}" cat-file -t "${basedOnCommit}" 2>/dev/null`, {
			encoding: "utf8",
		});
	} catch {
		return { stale: true, diffSummary: "base commit not found" };
	}

	try {
		const pathArgs = sourcePaths.map((p) => `"${p}"`).join(" ");
		const diff = execSync(
			`git -C "${root}" diff --stat "${basedOnCommit}..HEAD" -- ${pathArgs}`,
			{ encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] },
		).trim();

		if (!diff) {
			return { stale: false, diffSummary: null };
		}

		// Extract the summary line (last line of git diff --stat)
		const lines = diff.split("\n").filter(Boolean);
		const summary = lines[lines.length - 1]?.trim() ?? diff;
		return { stale: true, diffSummary: summary };
	} catch {
		return { stale: true, diffSummary: "git diff failed" };
	}
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main(): Promise<void> {
	const { root, scope } = parseArgs();

	if (!existsSync(root)) {
		process.stderr.write(`Error: root directory not found: ${root}\n`);
		process.exit(1);
	}

	// 1. Walk source files
	const sourceFiles = walkSourceFiles(root, root, scope);

	// 2. Walk doc files
	const docFiles = walkDocFiles(root);

	// 3. Scan annotations
	const annotations: Annotation[] = [];
	for (const file of sourceFiles) {
		annotations.push(...scanAnnotations(file, root));
	}

	// 4. Parse doc pages and check staleness
	const pages: DocPage[] = [];
	const reverseMap: Record<string, string[]> = {};

	for (const docPath of docFiles) {
		const fullPath = join(root, docPath);
		let content: string;
		try {
			content = readFileSync(fullPath, "utf8");
		} catch {
			continue;
		}

		const fm = parseFrontmatter(content);
		if (!fm) {
			// No frontmatter — still include with empty sources
			pages.push({
				path: docPath,
				title: "",
				sources: [],
				basedOnCommit: null,
				lastSynced: null,
				sourceHash: null,
				stale: false,
				diffSummary: null,
			});
			continue;
		}

		// Staleness detection
		let stale = false;
		let diffSummary: string | null = null;

		if (fm.basedOnCommit) {
			const sourcePaths = fm.sources.map((s) => s.path);
			const result = checkStaleness(fm.basedOnCommit, sourcePaths, root);
			stale = result.stale;
			diffSummary = result.diffSummary;
		} else if (fm.sourceHash) {
			// Legacy _source_hash without _based_on_commit
			stale = true;
			diffSummary = "legacy _source_hash — migrate to _based_on_commit";
		}
		// No _based_on_commit and no _source_hash = not stale (not tracked)

		// Build reverseMap entry
		if (fm.sources.length > 0) {
			if (!reverseMap[docPath]) reverseMap[docPath] = [];
			for (const src of fm.sources) {
				reverseMap[docPath].push(src.path);
			}
		}

		pages.push({
			path: docPath,
			title: fm.title,
			sources: fm.sources,
			basedOnCommit: fm.basedOnCommit,
			lastSynced: fm.lastSynced,
			sourceHash: fm.sourceHash,
			stale,
			diffSummary,
		});
	}

	// 5. Build sourceMap (source → doc pages)
	const sourceMap: Record<string, string[]> = {};

	for (const page of pages) {
		for (const src of page.sources) {
			if (!sourceMap[src.path]) sourceMap[src.path] = [];
			if (!sourceMap[src.path].includes(page.path)) {
				sourceMap[src.path].push(page.path);
			}
		}
	}

	// 6. Compute untracked source files
	// A source file is "untracked" if:
	//   - it's not referenced in any _sources
	//   - it doesn't have @doc: skip
	const skippedFiles = new Set(
		annotations.filter((a) => a.verb === "skip").map((a) => a.file),
	);

	const untracked = sourceFiles.filter(
		(f) => !sourceMap[f] && !skippedFiles.has(f),
	);

	const output: TrackerOutput = {
		annotations,
		pages,
		sourceMap,
		reverseMap,
		untracked,
	};

	process.stdout.write(JSON.stringify(output, null, 2) + "\n");
}

main().catch((err) => {
	process.stderr.write(`Fatal: ${err.message}\n${err.stack}\n`);
	process.exit(1);
});
