#!/usr/bin/env bun
/**
 * doc-audit.ts — Gap detection for skaildev-doc
 *
 * Runs doc-tracker.ts internally and applies heuristics to find undocumented
 * source files, stale pages, broken @doc:see refs, and coverage statistics.
 *
 * Usage: bun doc-audit.ts [--root <monorepo-root>] [--scope <path-prefix>]
 * Output: JSON to stdout
 */

import { execSync } from "node:child_process";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

// ---------------------------------------------------------------------------
// Types (mirrored from doc-tracker output)
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

interface Gap {
	file: string;
	reason: string;
	priority: "high" | "medium" | "low";
}

interface StalePage {
	path: string;
	staleSince: string | null;
	diffSummary: string | null;
}

interface BrokenRef {
	file: string;
	annotation: string;
	line: number;
	reason: string;
}

interface CoverageStats {
	tracked: number;
	total: number;
	skipped: number;
	percent: number;
}

interface AuditOutput {
	gaps: Gap[];
	stalePages: StalePage[];
	brokenRefs: BrokenRef[];
	coverage: CoverageStats;
	coverageByPackage: Record<string, CoverageStats>;
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
// Utilities
// ---------------------------------------------------------------------------

/**
 * Count the number of lines in a file. Returns 0 on read error.
 */
function getFileLineCount(filePath: string, root: string): number {
	const fullPath = join(root, filePath);
	try {
		const content = readFileSync(fullPath, "utf8");
		// Count newlines + 1 for the last line (if non-empty)
		const newlines = (content.match(/\n/g) ?? []).length;
		return content.length > 0 && !content.endsWith("\n")
			? newlines + 1
			: newlines;
	} catch {
		return 0;
	}
}

/**
 * Extract a package prefix from a relative file path.
 * Uses the first 2 path segments, unless the 2nd segment is one of
 * src / lib / scripts / docs — in that case uses only the first segment.
 */
function getPackagePrefix(filePath: string): string {
	const parts = filePath.split("/").filter(Boolean);
	if (parts.length === 0) return "(root)";
	if (parts.length === 1) return parts[0];

	const GENERIC_SECOND_SEGMENTS = new Set(["src", "lib", "scripts", "docs"]);

	if (GENERIC_SECOND_SEGMENTS.has(parts[1])) {
		return parts[0];
	}
	return `${parts[0]}/${parts[1]}`;
}

// ---------------------------------------------------------------------------
// Public-API file patterns
// ---------------------------------------------------------------------------

const PUBLIC_API_PATTERNS: RegExp[] = [
	/\/api\//,
	/\/routes\//,
	/\/server\/api\//,
	/\/controllers\//,
	/\.controller\.[tj]sx?$/,
	/\.route\.[tj]sx?$/,
];

function isPublicApiFile(filePath: string): boolean {
	return PUBLIC_API_PATTERNS.some((re) => re.test(filePath));
}

// ---------------------------------------------------------------------------
// Run doc-tracker and parse its JSON output
// ---------------------------------------------------------------------------

function runTracker(
	root: string,
	scope: string | null,
	scriptDir: string,
): TrackerOutput {
	const trackerPath = join(scriptDir, "doc-tracker.ts");

	if (!existsSync(trackerPath)) {
		throw new Error(`doc-tracker.ts not found at: ${trackerPath}`);
	}

	const scopeArg = scope ? `--scope "${scope}"` : "";
	const cmd = `bun "${trackerPath}" --root "${root}" ${scopeArg}`.trim();

	let stdout: string;
	try {
		stdout = execSync(cmd, {
			encoding: "utf8",
			stdio: ["pipe", "pipe", "pipe"],
			maxBuffer: 32 * 1024 * 1024, // 32 MB
		});
	} catch (err: unknown) {
		const message = err instanceof Error ? err.message : String(err);
		throw new Error(`doc-tracker.ts failed: ${message}`);
	}

	try {
		return JSON.parse(stdout) as TrackerOutput;
	} catch {
		throw new Error(
			`doc-tracker.ts produced invalid JSON. First 200 chars: ${stdout.slice(0, 200)}`,
		);
	}
}

// ---------------------------------------------------------------------------
// Directory coverage helpers
// ---------------------------------------------------------------------------

/**
 * Count source files per directory (one level: the immediate parent dir).
 * Returns a map of dirPath -> list of files in that dir.
 */
function buildDirFileMap(files: string[]): Map<string, string[]> {
	const map = new Map<string, string[]>();
	for (const f of files) {
		const parts = f.split("/");
		const dir = parts.length > 1 ? parts.slice(0, -1).join("/") : ".";
		if (!map.has(dir)) map.set(dir, []);
		map.get(dir)!.push(f);
	}
	return map;
}

// ---------------------------------------------------------------------------
// Main audit logic
// ---------------------------------------------------------------------------

async function main(): Promise<void> {
	const { root, scope } = parseArgs();

	if (!existsSync(root)) {
		process.stderr.write(`Error: root directory not found: ${root}\n`);
		process.exit(1);
	}

	// Resolve the directory containing this script
	const scriptDir = dirname(
		typeof __filename !== "undefined"
			? __filename
			: fileURLToPath(import.meta.url),
	);

	// 1. Run doc-tracker
	let tracker: TrackerOutput;
	try {
		tracker = runTracker(root, scope, scriptDir);
	} catch (err: unknown) {
		const msg = err instanceof Error ? err.message : String(err);
		process.stderr.write(`Fatal: ${msg}\n`);
		process.exit(1);
	}

	const { annotations, pages, sourceMap, untracked } = tracker;

	// All source files = tracked + untracked + skipped
	// We reconstruct "all" from sourceMap keys + untracked + skipped
	const skippedFiles = new Set(
		annotations.filter((a) => a.verb === "skip").map((a) => a.file),
	);
	const importantFiles = new Set(
		annotations.filter((a) => a.verb === "important").map((a) => a.file),
	);

	// Build @doc:see annotation lookup: file -> list of {seeTarget, line}
	const seeAnnotations: Array<{
		file: string;
		line: number;
		seeTarget: string;
	}> = annotations
		.filter((a) => a.verb === "see" && a.seeTarget !== undefined)
		.map((a) => ({ file: a.file, line: a.line, seeTarget: a.seeTarget! }));

	// All source files known to tracker
	const trackedFiles = new Set(Object.keys(sourceMap));
	const allSourceFiles = [...trackedFiles, ...untracked, ...skippedFiles];
	// Deduplicate (skipped files may also appear in untracked in edge cases)
	const allSourceFilesSet = new Set(allSourceFiles);

	// 2. Collect stale pages
	const stalePages: StalePage[] = pages
		.filter((p) => p.stale)
		.map((p) => ({
			path: p.path,
			staleSince: p.lastSynced ?? null,
			diffSummary: p.diffSummary ?? null,
		}));

	// 3. Gap detection
	const gaps: Gap[] = [];
	const flaggedFiles = new Set<string>();

	// HIGH: @doc:important files not in any _sources
	for (const file of importantFiles) {
		if (!trackedFiles.has(file)) {
			gaps.push({
				file,
				reason: "@doc:important but not referenced in any doc page _sources",
				priority: "high",
			});
			flaggedFiles.add(file);
		}
	}

	// HIGH: Public API files not in _sources and not @doc:skip
	for (const file of allSourceFilesSet) {
		if (flaggedFiles.has(file)) continue;
		if (skippedFiles.has(file)) continue;
		if (trackedFiles.has(file)) continue;
		if (isPublicApiFile(file)) {
			gaps.push({
				file,
				reason:
					"public API file (matches route/controller/api pattern) not covered by any doc page",
				priority: "high",
			});
			flaggedFiles.add(file);
		}
	}

	// MEDIUM: Files >150 lines not in _sources, not @doc:skip, not already flagged
	for (const file of allSourceFilesSet) {
		if (flaggedFiles.has(file)) continue;
		if (skippedFiles.has(file)) continue;
		if (trackedFiles.has(file)) continue;
		const lineCount = getFileLineCount(file, root);
		if (lineCount > 150) {
			gaps.push({
				file,
				reason: `large file (${lineCount} lines) with no doc coverage`,
				priority: "medium",
			});
			flaggedFiles.add(file);
		}
	}

	// MEDIUM: Directories with 3+ source files and zero coverage
	// Build per-dir maps for untracked (not skipped, not flagged yet)
	const uncoveredFiles = [...allSourceFilesSet].filter(
		(f) => !skippedFiles.has(f) && !trackedFiles.has(f),
	);
	const dirFileMap = buildDirFileMap(uncoveredFiles);
	// Also need to know total files per dir (all, including covered ones)
	const allDirFileMap = buildDirFileMap(
		[...allSourceFilesSet].filter((f) => !skippedFiles.has(f)),
	);

	for (const [dir, filesInDir] of dirFileMap.entries()) {
		// Total non-skipped files in this dir
		const totalInDir = allDirFileMap.get(dir)?.length ?? filesInDir.length;
		// Covered files in this dir (those in sourceMap)
		const coveredInDir = (allDirFileMap.get(dir) ?? []).filter((f) =>
			trackedFiles.has(f),
		).length;

		if (filesInDir.length >= 3 && coveredInDir === 0) {
			// Flag the directory, not individual files already flagged
			// Don't re-flag files already in gaps
			const alreadyFlagged = filesInDir.filter((f) => flaggedFiles.has(f));
			const notYetFlagged = filesInDir.filter((f) => !flaggedFiles.has(f));
			if (notYetFlagged.length > 0) {
				// Emit one gap entry for the directory
				gaps.push({
					file: `${dir}/`,
					reason: `directory has ${totalInDir} source files with zero doc coverage`,
					priority: "medium",
				});
				// Mark all files in dir as flagged to avoid LOW duplication
				for (const f of notYetFlagged) {
					flaggedFiles.add(f);
				}
				// Suppress the already-flagged count warning
				void alreadyFlagged;
			}
		}
	}

	// LOW: Remaining untracked, not skipped, not already flagged
	for (const file of untracked) {
		if (flaggedFiles.has(file)) continue;
		if (skippedFiles.has(file)) continue;
		gaps.push({
			file,
			reason: "untracked source file with no doc coverage",
			priority: "low",
		});
		flaggedFiles.add(file);
	}

	// 4. Check @doc:see broken refs
	const pageTitles = new Set(pages.map((p) => p.title).filter(Boolean));
	const brokenRefs: BrokenRef[] = [];

	for (const { file, line, seeTarget } of seeAnnotations) {
		if (!pageTitles.has(seeTarget)) {
			brokenRefs.push({
				file,
				annotation: `@doc:see "${seeTarget}"`,
				line,
				reason: `no doc page found with title "${seeTarget}"`,
			});
		}
	}

	// 5. Compute coverage stats
	const totalFiles = allSourceFilesSet.size;
	const skippedCount = skippedFiles.size;
	const trackedCount = trackedFiles.size;
	const denominator = totalFiles - skippedCount;
	const overallPercent =
		denominator > 0
			? Math.round((trackedCount / denominator) * 10000) / 100
			: 100;

	const coverage: CoverageStats = {
		tracked: trackedCount,
		total: totalFiles,
		skipped: skippedCount,
		percent: overallPercent,
	};

	// Per-package coverage
	const pkgMap = new Map<
		string,
		{ tracked: number; total: number; skipped: number }
	>();

	for (const file of allSourceFilesSet) {
		const pkg = getPackagePrefix(file);
		if (!pkgMap.has(pkg)) pkgMap.set(pkg, { tracked: 0, total: 0, skipped: 0 });
		const entry = pkgMap.get(pkg)!;
		entry.total++;
		if (skippedFiles.has(file)) {
			entry.skipped++;
		} else if (trackedFiles.has(file)) {
			entry.tracked++;
		}
	}

	const coverageByPackage: Record<string, CoverageStats> = {};
	for (const [pkg, stats] of pkgMap.entries()) {
		const denom = stats.total - stats.skipped;
		const pct =
			denom > 0 ? Math.round((stats.tracked / denom) * 10000) / 100 : 100;
		coverageByPackage[pkg] = {
			tracked: stats.tracked,
			total: stats.total,
			skipped: stats.skipped,
			percent: pct,
		};
	}

	// 6. Emit output
	const output: AuditOutput = {
		gaps,
		stalePages,
		brokenRefs,
		coverage,
		coverageByPackage,
	};

	process.stdout.write(JSON.stringify(output, null, 2) + "\n");
}

main().catch((err) => {
	process.stderr.write(`Fatal: ${err.message}\n${err.stack}\n`);
	process.exit(1);
});
