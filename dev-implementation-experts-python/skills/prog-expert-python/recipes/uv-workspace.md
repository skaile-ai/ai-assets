---
name: uv-workspace
description: uv workspace monorepo setup with multiple Python packages sharing dependencies
libraries_used: uv, uv_build
---

# uv Workspace Monorepo Recipe

## Structure
```
project-root/
├── pyproject.toml          # workspace root
├── uv.lock                 # lockfile (commit this)
├── package-a/
│   ├── pyproject.toml
│   └── src/package_a/
└── package-b/
    ├── pyproject.toml
    └── src/package_b/
```

## Root pyproject.toml
```toml
[project]
name = "my-project"
version = "0.1.0"
requires-python = ">=3.12"
dependencies = [
    "package-a",
    "package-b",
]

[tool.uv.workspace]
members = ["package-a", "package-b"]

[tool.uv.sources]
package-a = { workspace = true }
package-b = { workspace = true }

[tool.pytest.ini_options]
testpaths = ["tests", "package-a/tests"]
pythonpath = ["package-a/src", "package-b/src"]
addopts = "-v --tb=short"
```

## Per-Package pyproject.toml
```toml
[project]
name = "package-a"
version = "1.0.0"
requires-python = ">=3.12"
dependencies = ["pydantic>=2.0.0"]

[build-system]
requires = ["uv_build>=0.8.19,<0.9.0"]
build-backend = "uv_build"

[tool.uv.sources]
package-b = { workspace = true }  # if depends on sibling
```

## Key Commands
```bash
uv sync                    # install all workspace packages + deps
uv run pytest              # run tests across workspace
uv run python -m package_a # run a module
uv add pydantic            # add to root
uv add --dev pytest        # add dev dependency
uv build package-a/        # build a specific package
```

## Notes
- `uv.lock` is a single lockfile for the entire workspace — commit it.
- When a member package changes, run `uv sync` to refresh.
- Use `src/` layout: `src/package_name/__init__.py` — avoids import shadowing.
- `pythonpath` in `pytest.ini_options` must list all `src/` directories.
