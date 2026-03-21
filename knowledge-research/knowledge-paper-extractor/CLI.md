# Knowledge Paper Extractor CLI

The `knowledge-paper-extractor` skill provides a command-line interface to extract metadata (title, authors, abstract) and cited references from scientific PDF papers or book chapters. 

## Usage

```bash
uv run scripts/extract_paper.py [OPTIONS] PDF_PATH
```

### Arguments

* `PDF_PATH` (Required): Path to the scientific PDF file you want to process.

### Options

* `-o`, `--output-dir TEXT`: Directory to write the extracted output files. (Default: `./output`)
* `--help`: Show the help message and exit.

## Outputs

When you run the extractor, it generates the following files in the specified output directory:

* `metadata.json`: Contains the extracted title, authors, abstract, and embedded PDF metadata.
* `references.json`: A structured list of all cited references extracted via `refextract`.
* `references.csl.json`: The resolved references queried against the Crossref API in CSL-JSON format.
* `summary.md`: A human-readable Markdown summary of the paper including the extracted abstract and a formatted list of references with DOI/URL links.

### Example

```bash
uv run scripts/extract_paper.py ./papers/attention_is_all_you_need.pdf -o ./output/attention
```

---

## Suggested Subcommands (For Future Implementation)

1. **`batch`**  
   **Description:** Process an entire directory of PDFs recursively, extracting metadata and references for all papers and organizing the outputs into subfolders named after each PDF.  
   **Example:** `uv run scripts/extract_paper.py batch ./papers/ --output-dir ./extracted_data/`

2. **`export-bibtex`**  
   **Description:** Convert the resolved `references.csl.json` output into standard BibTeX format (`.bib`), making it easier to import references directly into LaTeX documents or reference managers like Zotero/Mendeley.  
   **Example:** `uv run scripts/extract_paper.py export-bibtex ./output/attention/references.csl.json -o ./output/attention/refs.bib`

3. **`search`**  
   **Description:** Search through previously extracted summaries and metadata across multiple processed directories, allowing users to find papers by author, keyword in abstract, or specific references.  
   **Example:** `uv run scripts/extract_paper.py search "transformer models" --in ./extracted_data/`