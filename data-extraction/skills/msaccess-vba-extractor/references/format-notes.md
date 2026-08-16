# How VBA is stored in MS Access, and why standard tools fail

## The short version

- `.accdb` (Access 2007+) uses the **ACE** engine; its header string is
  `Standard ACE DB` at offset 4. `.mdb` (Access 97-2003) uses **Jet4**
  (`Standard Jet DB`).
- VBA module source is **MS-OVBA compressed** and stored inside **long-value
  (LVAL) pages** of the database file (page size 4096 bytes).
- **`olevba` does not open `.accdb`** - it raises `FileOpenError: not a supported
  file type`. It only handles OLE2-wrapped VBA (old `.mdb`, `.doc`, `.xls`).
- **`mdbtools` (`mdb-export`) cannot reassemble ACE12 long values** - it returns
  `File not found` for the OLE column that holds the VBA. It reads table rows,
  not the VBA storage.

So for a modern `.accdb` you must decompress the VBA yourself. That is what
`scripts/extract_vba.py` does.

## MS-OVBA compression (MS-[MS-OVBA] 2.4.1)

A module's source is a **CompressedContainer**:

```
byte 0:        SignatureByte = 0x01
then 1+ CompressedChunks:
  CompressedChunkHeader (2 bytes, little-endian uint16):
    bits 0-11  : CompressedChunkSize  (actual chunk length = value + 3, incl. header)
    bits 12-14 : CompressedChunkSignature = 0b011  (must match)
    bit  15    : CompressedChunkFlag (1 = compressed, 0 = raw 4096 bytes)
  chunk data: a sequence of TokenSequences
    each TokenSequence = 1 FlagByte + up to 8 tokens
    FlagByte bit i (LSB first): 0 = literal byte, 1 = copy token (2 bytes)
    copy token decode (relative to start of THIS chunk's decompressed output):
      diff       = len(decompressed_so_far_in_chunk)
      bitcount   = max(4, ceil(log2(diff)))   # in python: max(4,(diff-1).bit_length())
      lengthmask = 0xFFFF >> bitcount
      length     = (token & lengthmask) + 3
      offset     = ((token & ~lengthmask) >> (16 - bitcount)) + 1
      copy `length` bytes from `offset` back
```

Each compressed chunk decompresses to **at most 4096 bytes**. A chunk shorter than
4096 is the final chunk. Decompressed source begins with
`Attribute VB_Name = "<ModuleName>"`.

### Finding containers in the raw file

The literal string `Attribute VB_Name` is **not** searchable on disk because a
FlagByte is interleaved every 8 bytes; it reads `Attribut\x00e VB_Nam`. Search for
`Attribut`, then scan backwards up to ~48 bytes for the `0x01` signature followed
by a valid chunk header (signature bits `011`). That offset is the container start.

## LVAL page layout (the reassembly problem)

ACE data pages carrying long values start with a 16-byte header whose bytes 4-7
are the ASCII signature `LVAL`:

```
offset 0    : page type (0x01 = data/LVAL data; 0x09 = a non-data LVAL page)
offset 2-3  : free space in page (uint16)
offset 4-7  : "LVAL"
offset 12-13: number of rows (uint16)
offset 14-15: row 0 data offset (uint16)
row0[0:4]   : "next" pointer for the fragment (page = value >> 8, row = value & 0xFF)
row0[4:end] : fragment payload (end = 4096 - freespace)
```

When a module's compressed bytes fit inside one page fragment, a plain linear read
of the file decompresses it correctly (the common case - most modules).

When a module spans multiple pages, two things break a linear read:

1. **Interrupting pages.** A `type 0x09` LVAL page (nearly empty, freespace ~4080)
   or a non-LVAL page sits physically between the data pages. Splicing together
   only the `type 0x01` LVAL fragments in file order removes these gaps and
   recovers several such modules. (`_lval_page_order_blob` in the script.)

2. **Non-adjacent, mis-ordered pages.** Some modules' fragments live on pages that
   are neither physically adjacent nor reliably linked - the raw `row0` "next"
   pointer often points to the wrong page (observed: a page whose pointer decodes
   to a valid page number that only holds ~500 bytes when the chunk needs 1700+,
   and a real continuation many hundreds of pages away). Following those pointers,
   or greedily guessing the next page by "does the output look printable", both
   fail: a wrong continuation still decompresses to **printable garbage** (e.g.
   `Public Function Connelicit` repeated) because copy tokens keep referencing
   recent text. Printable-ness is NOT a validity signal.

To fully resolve case 2 you need a real ACE long-value **chain** parser driven by
the `MSysAccessStorage` table (each VBA stream is a row whose `Lv` OLE column
holds the pointer to its LVAL chain head + total length). `mdbtools` does not
implement this for ACE12. That is the remaining gap; those modules are emitted to
`partial/` and should be exported from Access directly.

## Reliable corruption signals

- **NUL bytes** in the decompressed output => desync. (Clean VBA has none.)
- **Control chars** other than tab/CR/LF => desync.
- High Latin-1 bytes (128-255) are **fine** - German umlauts (ä ö ü ß) are real
  source text, not corruption. Do not reject them.
- A desynced continuation can be all-printable, so also require the module to
  **end on a real statement** (`End Sub`/`End Function`/... or `Option Explicit`
  for an empty module) before calling it complete.
