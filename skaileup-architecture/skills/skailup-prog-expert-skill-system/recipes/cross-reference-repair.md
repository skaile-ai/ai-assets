# Recipe: Cross-Reference Repair

Detecting and fixing broken bidirectional links between artifact folders.

## Detection Algorithm

```python
def audit_cross_references(concept_dir):
    """Scan all artifacts, build reference graph, find broken links."""
    forward_refs = {}   # file -> [referenced files]
    back_refs = {}      # file -> [files that reference it]

    # 1. Scan feature files for screen references
    for feature_file in glob(f"{concept_dir}/03_features/**/*.md"):
        fm = parse_frontmatter(feature_file)
        for screen in fm.get('screens', []):
            target = screen.get('path', '')
            forward_refs.setdefault(feature_file, []).append(target)
            back_refs.setdefault(target, []).append(feature_file)

    # 2. Scan screen files for implements references
    for screen_file in glob(f"{concept_dir}/07_screens/**/*.md"):
        fm = parse_frontmatter(screen_file)
        for impl in fm.get('implements', []):
            forward_refs.setdefault(screen_file, []).append(impl)
            back_refs.setdefault(impl, []).append(screen_file)

    # 3. Find issues
    issues = []

    # Missing targets (forward ref points to non-existent file)
    for source, targets in forward_refs.items():
        for target in targets:
            if not exists(join(concept_dir, target)):
                issues.append(('orphan_ref', source, target))

    # Missing backlinks (A references B, but B doesn't reference A back)
    for feature_file in glob(f"{concept_dir}/03_features/**/*.md"):
        fm = parse_frontmatter(feature_file)
        screen_paths = [s['path'] for s in fm.get('screens', [])]
        for screen_path in screen_paths:
            screen_file = join(concept_dir, screen_path)
            if exists(screen_file):
                screen_fm = parse_frontmatter(screen_file)
                feature_rel = relative(concept_dir, feature_file)
                if feature_rel not in screen_fm.get('implements', []):
                    issues.append(('missing_backlink', screen_file, feature_rel))

    return issues
```

## Repair Protocol

1. **Show diff before applying** — never auto-fix without user seeing what changes
2. **Update both files atomically** — if adding a backlink, also update forward ref timestamps
3. **Emit feedback_loop event** — for observability
4. **Run audit again after repair** — verify all issues resolved

## Quality Metrics

```python
metrics = {
    'coverage': features_with_screens / total_features,     # Target: 100%
    'integrity': valid_backlinks / total_forward_refs,       # Target: 100%
    'freshness': files_updated_last_30_days / total_files,   # Target: >80%
    'orphans': unreferenced_files / total_files,             # Target: 0%
}
```
