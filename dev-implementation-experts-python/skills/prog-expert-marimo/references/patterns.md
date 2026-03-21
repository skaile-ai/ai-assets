# Marimo Patterns & Best Practices

## Core File Structure

```python
import marimo

__generated_with = "0.1.0"
app = marimo.App()

@app.cell
def __():
    import marimo as mo
    import pandas as pd
    return mo, pd

@app.cell
def __(mo):
    mo.md("# My Notebook Title")
    return

if __name__ == "__main__":
    app.run()
```

## Reactivity Rules

| Rule | Correct | Wrong |
|---|---|---|
| Global uniqueness | `df_clean = df.dropna()` | `df.dropna(inplace=True)` |
| No shared names | Each cell returns unique names | Two cells returning `df` → error |
| Order independence | Marimo resolves by variable refs | Do not assume cell order |

## Cell Parameter Injection

Variables a cell depends on are declared as function parameters:

```python
@app.cell
def __(mo, pd):           # depends on mo and pd from other cells
    df = pd.DataFrame(...)
    return df,            # trailing comma — always return a tuple
```

## Displaying Output

```python
# Markdown
mo.md("## Section Title")

# Last expression auto-displays
df  # shows dataframe in cell output

# Plots — return figure, don't call .show()
import matplotlib.pyplot as plt
fig, ax = plt.subplots()
ax.plot(x, y)
fig   # display

# UI Elements
slider = mo.ui.slider(0, 100)
slider
```

## Inline Assertions (Design Phase)

Add assertions at the end of cells to validate assumptions:

```python
@app.cell
def __(df):
    df_processed = df[df['value'] > 0]
    assert not df_processed.empty, "Processed dataframe is empty"
    assert "score" in df_processed.columns, "Column 'score' is missing"
    return df_processed,
```

## Test Protocol: Lint → Export → Assert

### Step 1: Lint
```bash
ruff check your_notebook.py
```
Fix all syntax errors before proceeding.

### Step 2: Headless Export (Smoke Test)
```bash
marimo export html your_notebook.py -o /dev/null
```
- Forces every cell to execute
- Non-zero exit = failure → read traceback, fix, retry from Step 1

### Step 3: Verify Checklist
- [ ] File is a valid `.py` (not `.ipynb`)
- [ ] All cells are decorated with `@app.cell`
- [ ] All external imports are inside cells (not at module level)
- [ ] `ruff check` passes
- [ ] `marimo export` passes without errors

## Error Recovery Pattern

When smoke test fails:
1. Find cell from traceback function name
2. Check parent cells — did a dependency fail to return its variable?
3. Check assertions — did logic fail? Fix logic, not just the assertion
4. Re-run from Step 1 (lint)