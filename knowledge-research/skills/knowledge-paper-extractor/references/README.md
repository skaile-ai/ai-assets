# References

## Libraries Used

### refextract
- **Purpose**: Extracts cited references from the bibliography section of PDF papers
- **API**: `refextract.extract_references_from_file(pdf_path)` → list of dicts
- **Output fields**: `raw_ref`, `author`, `title`, `doi`, `journal`, `volume`, `year`, `reportnumber`
- **Docs**: https://pypi.org/project/refextract/

### pdfminer.six
- **Purpose**: Extracts text with layout and font information for heuristic metadata parsing
- **API**: `extract_pages(pdf_path, maxpages=1)` → page layout with LTTextBox/LTTextLine/LTChar
- **Docs**: https://pdfminersix.readthedocs.io/

### pypdf
- **Purpose**: Reads embedded PDF metadata (title, author, subject)
- **API**: `PdfReader(path).metadata` → PdfDocumentInfo
- **Docs**: https://pypdf.readthedocs.io/