---
name: marimo-notebook
description: Full Marimo notebook template with common patterns for data science workflows
libraries_used: ["marimo", "pandas", "matplotlib", "ruff"]
---

# Marimo Notebook Recipe

## Minimal Template

```python
import marimo

__generated_with = "0.1.0"
app = marimo.App()

@app.cell
def __():
    import marimo as mo
    return mo,

@app.cell
def __(mo):
    mo.md("# Notebook Title")
    return

if __name__ == "__main__":
    app.run()
```

## Data Science Template (pandas + matplotlib)

```python
import marimo

__generated_with = "0.1.0"
app = marimo.App()

@app.cell
def __():
    import marimo as mo
    import pandas as pd
    import matplotlib.pyplot as plt
    return mo, pd, plt

@app.cell
def __(mo):
    mo.md("# Data Analysis")
    return

@app.cell
def __(pd):
    # Load data
    df = pd.read_csv("data.csv")
    assert not df.empty, "Dataset is empty"
    assert "value" in df.columns, "Column 'value' missing"
    return df,

@app.cell
def __(df, pd):
    # Transform — always create new variables, never mutate
    df_clean = df.dropna()
    df_filtered = df_clean[df_clean["value"] > 0]
    assert len(df_filtered) > 0, "No rows after filtering"
    return df_filtered,

@app.cell
def __(df_filtered, plt):
    # Plot — return figure, never call .show()
    fig, ax = plt.subplots(figsize=(10, 6))
    ax.hist(df_filtered["value"], bins=30)
    ax.set_title("Distribution of Values")
    ax.set_xlabel("Value")
    ax.set_ylabel("Count")
    return fig,

if __name__ == "__main__":
    app.run()
```

## Interactive UI Template

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
    # UI elements — declare in their own cell
    threshold = mo.ui.slider(0, 100, value=50, label="Threshold")
    threshold
    return threshold,

@app.cell
def __(df, mo, threshold):
    # React to slider value
    filtered = df[df["score"] > threshold.value]
    mo.md(f"**{len(filtered)} rows** above threshold {threshold.value}")
    return filtered,
```

## Common Patterns

### Conditional Display
```python
@app.cell
def __(mo, result):
    mo.md(f"**Result:** {result}") if result else mo.md("*No result yet*")
    return
```

### Multiple Outputs
```python
@app.cell
def __(mo, df):
    # Use mo.vstack / mo.hstack to combine outputs
    return mo.vstack([
        mo.md(f"Shape: {df.shape}"),
        df.head(),
    ])
```

### Table Display
```python
@app.cell
def __(mo, df):
    mo.ui.table(df)  # Interactive, filterable table
    return
```

## Validation Checklist Before Finishing

```bash
# 1. Static analysis
ruff check notebook.py

# 2. Headless smoke test (runs all cells)
marimo export html notebook.py -o /dev/null

# If either fails: read traceback → fix logic → repeat
```
