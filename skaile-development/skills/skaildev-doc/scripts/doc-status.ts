#!/usr/bin/env bun
/**
 * doc-status.ts — Human-readable or JSON documentation status report
 *
 * Thin wrapper over doc-audit.ts. Runs the audit and formats its output
 * either as raw JSON or as a structured markdown report.
 *
 * Usage: bun doc-status.ts [--root <monorepo-root>] [--scope <path-prefix>] [--format json|markdown]
 * Output: markdown (default) or JSON to stdout
 */

import { execSync } from "node:child_process";
import { existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

// ---------------------------------------------------------------------------
// Types (mirrored from doc-audit.ts output)
// ---------------------------------------------------------------------------

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

function parseArgs(): {
	root: string;
	scope: string | null;
	format: "json" | "markdown";
} {
	const args = process.argv.slice(2);
	let root = process.cwd();
	let scope: string | null = null;
	let format: "json" | "markdown" = "markdown";

	for (let i = 0; i < args.length; i++) {
		if (args[i] === "--root" && args[i + 1]) {
			root = resolve(args[++i]);
		} else if (args[i] === "--scope" && args[i + 1]) {
			scope = args[++i];
		} else if (args[i] === "--format" && args[i + 1]) {
			const val = args[++i];
			if (val === "json" || val === "markdown") {
				format = val;
			} else {
				process.stderr.write(
					`Warning: unknown --format value "${val}", defaulting to markdown\n`,
				);
			}
		}
	}

	return { root, scope, format };
}

// ---------------------------------------------------------------------------
// Run doc-audit.ts and parse its JSON output
// ---------------------------------------------------------------------------

function runAudit(
	root: string,
	scope: string | null,
	scriptDir: string,
): AuditOutput {
	const auditPath = join(scriptDir, "doc-audit.ts");

	if (!existsSync(auditPath)) {
		throw new Error(`doc-audit.ts not found at: ${auditPath}`);
	}

	const scopeArg = scope ? `--scope "${scope}"` : "";
	const cmd = `bun "${auditPath}" --root "${root}" ${scopeArg}`.trim();

	let stdout: string;
	try {
		stdout = execSync(cmd, {
			encoding: "utf8",
			stdio: ["pipe", "pipe", "pipe"],
			maxBuffer: 32 * 1024 * 1024, // 32 MB
		});
	} catch (err: unknown) {
		const message = err instanceof Error ? err.message : String(err);
		throw new Error(`doc-audit.ts failed: ${message}`);
	}

	try {
		return JSON.parse(stdout) as AuditOutput;
	} catch {
		throw new Error(
			`doc-audit.ts produced invalid JSON. First 200 chars: ${stdout.slice(0, 200)}`,
		);
	}
}

// ---------------------------------------------------------------------------
// Markdown formatting helpers
// ---------------------------------------------------------------------------

function formatPercent(n: number): string {
	return `${n.toFixed(2)}%`;
}

function padRight(s: string, len: number): string {
	return s.length >= len ? s : s + " ".repeat(len - s.length);
}

function padLeft(s: string, len: number): string {
	return s.length >= len ? s : " ".repeat(len - s.length) + s;
}

function buildMarkdown(audit: AuditOutput): string {
	const lines: string[] = [];

	// Title
	lines.push("# Documentation Status Report");
	lines.push("");

	// Overall coverage
	const { tracked, total, skipped, percent } = audit.coverage;
	lines.push(`## Coverage: ${formatPercent(percent)}`);
	lines.push("");
	lines.push(`- **Tracked:** ${tracked} files`);
	lines.push(`- **Total:** ${total} files`);
	lines.push(`- **Skipped:** ${skipped} files`);
	lines.push("");

	// Coverage by package table (sorted ascending by percent)
	const pkgEntries = Object.entries(audit.coverageByPackage).sort(
		([, a], [, b]) => a.percent - b.percent,
	);

	if (pkgEntries.length > 0) {
		lines.push("## Coverage by Package");
		lines.push("");

		// Compute column widths
		const headers = ["Package", "Tracked", "Total", "Skipped", "Coverage"];
		const rows: string[][] = pkgEntries.map(([pkg, stats]) => [
			pkg,
			String(stats.tracked),
			String(stats.total),
			String(stats.skipped),
			formatPercent(stats.percent),
		]);

		const colWidths = headers.map((h, i) =>
			Math.max(h.length, ...rows.map((r) => r[i].length)),
		);

		const headerLine =
			"| " +
			headers.map((h, i) => padRight(h, colWidths[i])).join(" | ") +
			" |";
		const separatorLine =
			"| " + colWidths.map((w) => "-".repeat(w)).join(" | ") + " |";

		lines.push(headerLine);
		lines.push(separatorLine);

		for (const row of rows) {
			const dataLine =
				"| " +
				row
					.map((cell, i) =>
						i === 0 || i === 4
							? padRight(cell, colWidths[i])
							: padLeft(cell, colWidths[i]),
					)
					.join(" | ") +
				" |";
			lines.push(dataLine);
		}
		lines.push("");
	}

	// Stale pages
	if (audit.stalePages.length > 0) {
		lines.push(`## Stale Pages (${audit.stalePages.length})`);
		lines.push("");

		const headers = ["Page", "Last Synced", "Diff Summary"];
		const rows: string[][] = audit.stalePages.map((p) => [
			p.path,
			p.staleSince ?? "—",
			p.diffSummary ?? "—",
		]);
		const colWidths = headers.map((h, i) =>
			Math.max(h.length, ...rows.map((r) => r[i].length)),
		);

		lines.push(
			"| " +
				headers.map((h, i) => padRight(h, colWidths[i])).join(" | ") +
				" |",
		);
		lines.push("| " + colWidths.map((w) => "-".repeat(w)).join(" | ") + " |");

		for (const row of rows) {
			lines.push(
				"| " + row.map((cell, i) => padRight(cell, colWidths[i])).join(" | ") + " |",
			);
		}
		lines.push("");
	}

	// High priority gaps
	const highGaps = audit.gaps.filter((g) => g.priority === "high");
	if (highGaps.length > 0) {
		lines.push(`## High Priority Gaps (${highGaps.length})`);
		lines.push("");
		for (const g of highGaps) {
			lines.push(`- \`${g.file}\` — ${g.reason}`);
		}
		lines.push("");
	}

	// Medium priority gaps
	const mediumGaps = audit.gaps.filter((g) => g.priority === "medium");
	if (mediumGaps.length > 0) {
		lines.push(`## Medium Priority Gaps (${mediumGaps.length})`);
		lines.push("");
		for (const g of mediumGaps) {
			lines.push(`- \`${g.file}\` — ${g.reason}`);
		}
		lines.push("");
	}

	// Low priority gaps — count only
	const lowGaps = audit.gaps.filter((g) => g.priority === "low");
	if (lowGaps.length > 0) {
		lines.push(`## Low Priority Gaps (${lowGaps.length})`);
		lines.push("");
		lines.push(
			`${lowGaps.length} untracked source file${lowGaps.length === 1 ? "" : "s"} with no doc coverage (not individually listed).`,
		);
		lines.push("");
	}

	// Broken references
	if (audit.brokenRefs.length > 0) {
		lines.push(`## Broken References (${audit.brokenRefs.length})`);
		lines.push("");

		const headers = ["File", "Line", "Annotation", "Reason"];
		const rows: string[][] = audit.brokenRefs.map((r) => [
			r.file,
			String(r.line),
			r.annotation,
			r.reason,
		]);
		const colWidths = headers.map((h, i) =>
			Math.max(h.length, ...rows.map((r) => r[i].length)),
		);

		lines.push(
			"| " +
				headers.map((h, i) => padRight(h, colWidths[i])).join(" | ") +
				" |",
		);
		lines.push("| " + colWidths.map((w) => "-".repeat(w)).join(" | ") + " |");

		for (const row of rows) {
			lines.push(
				"| " +
					row
						.map((cell, i) =>
							i === 1 ? padLeft(cell, colWidths[i]) : padRight(cell, colWidths[i]),
						)
						.join(" | ") +
					" |",
			);
		}
		lines.push("");
	}

	return lines.join("\n");
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main(): Promise<void> {
	const { root, scope, format } = parseArgs();

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

	let audit: AuditOutput;
	try {
		audit = runAudit(root, scope, scriptDir);
	} catch (err: unknown) {
		const msg = err instanceof Error ? err.message : String(err);
		process.stderr.write(`Fatal: ${msg}\n`);
		process.exit(1);
	}

	if (format === "json") {
		process.stdout.write(JSON.stringify(audit, null, 2) + "\n");
	} else {
		process.stdout.write(buildMarkdown(audit) + "\n");
	}
}

main().catch((err) => {
	process.stderr.write(`Fatal: ${err.message}\n${err.stack}\n`);
	process.exit(1);
});
