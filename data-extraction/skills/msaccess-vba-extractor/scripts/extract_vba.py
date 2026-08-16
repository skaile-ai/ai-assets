#!/usr/bin/env python3
"""Extract VBA source from a Microsoft Access database (.accdb / .mdb).

Why this exists: olevba and mdbtools do NOT extract VBA from modern ACE12
(.accdb "Standard ACE DB") files. The VBA is stored MS-OVBA-compressed inside
ACE long-value (LVAL) database pages. This script decompresses it directly.

Approach (no external deps, pure stdlib):
  1. Scan the raw file for compressed VBA module containers. Each module source
     is a MS-OVBA CompressedContainer: 0x01 signature + one or more chunks, and
     decompresses to text starting with `Attribute VB_Name = "..."`.
  2. Decompress each container found in the linear byte stream. Modules whose
     compressed bytes are physically contiguous come out complete.
  3. For modules interrupted by non-data pages, also try a second pass over the
     concatenation of type-0x01 LVAL page fragments in file order, which removes
     the interrupting pages.
  4. Keep, per module, the best version (valid + longest). Classify each as
     complete (ends on a real statement) or partial (truncated at a desync where
     the compressed data spills onto physically non-adjacent LVAL pages).

Limitation: a module whose compressed data is scattered across non-adjacent LVAL
pages cannot be fully reassembled without a complete ACE long-value chain parser
(the raw page "next" pointers are unreliable in practice). Those come out partial
or missing and must be exported from Access itself (VBE Alt+F11 -> Export File).

Usage:
    python3 extract_vba.py <database.accdb> [-o OUTDIR]     # default OUTDIR: ./vba
    python3 extract_vba.py --selfcheck                      # run the built-in test
"""
from __future__ import annotations
import argparse
import os
import re
import sys

PAGE = 4096  # ACE / Jet4 page size


# ---------------------------------------------------------------------------
# MS-OVBA decompression (MS-[MS-OVBA] 2.4.1)
# ---------------------------------------------------------------------------
def decompress_container(comp: bytes, start: int, limit: int = 5_000_000):
    """Decompress a MS-OVBA CompressedContainer starting at `start`.

    Returns (decompressed_bytes, complete) where `complete` is True when the
    container ended naturally (final chunk) rather than running out of input.
    """
    p = start
    if p >= len(comp) or comp[p] != 0x01:
        return b"", False
    p += 1
    out = bytearray()
    complete = False
    while p + 2 <= len(comp) and len(out) < limit:
        header = comp[p] | (comp[p + 1] << 8)
        if ((header >> 12) & 0x7) != 0b011:      # CompressedChunkSignature
            complete = True
            break
        size = (header & 0x0FFF) + 3             # total chunk size incl. 2-byte header
        flag = (header >> 15) & 1                # CompressedChunkFlag
        chunk_end = p + size
        p += 2
        if chunk_end > len(comp):                # truncated mid-chunk -> incomplete
            break
        if flag == 0:                            # raw 4096-byte chunk
            out += comp[p:p + 4096]
            p += 4096
            complete = True
            break
        chunk = bytearray()
        desync = False
        while p < chunk_end and p < len(comp):
            flag_byte = comp[p]
            p += 1
            for bit in range(8):
                if p >= chunk_end:
                    break
                if not (flag_byte >> bit) & 1:   # literal token
                    chunk.append(comp[p])
                    p += 1
                else:                            # copy token
                    token = comp[p] | (comp[p + 1] << 8)
                    p += 2
                    diff = len(chunk)
                    bitcount = max(4, (diff - 1).bit_length()) if diff else 4
                    length_mask = 0xFFFF >> bitcount
                    length = (token & length_mask) + 3
                    offset = ((token & ~length_mask & 0xFFFF) >> (16 - bitcount)) + 1
                    src = len(chunk) - offset
                    if src < 0:
                        desync = True
                        break
                    for i in range(length):
                        chunk.append(chunk[src + i])
            if desync:
                break
        out += chunk
        if len(chunk) < 4096:                    # final (short) chunk -> done
            complete = True
            break
    return bytes(out), complete


# ---------------------------------------------------------------------------
# Container discovery
# ---------------------------------------------------------------------------
_NAME_RE = re.compile(rb'Attribute VB_Name = "([^"\x00\r\n]{1,80})"')
# Compressed marker: literal "Attribute VB_Nam" gets a flag byte inserted every
# 8 bytes, so the on-disk bytes read "Attribut\x00e VB_Nam".
_COMPRESSED_MARKER = re.compile(rb"Attribut\x00e VB_Nam")


def _find_containers(buf: bytes):
    """Yield offsets in `buf` that look like a MS-OVBA container start (0x01 +
    valid chunk header) shortly before an 'Attribut' marker."""
    for m in re.finditer(rb"Attribut", buf):
        a = m.start()
        for i in range(a - 1, max(a - 48, 0), -1):
            if buf[i] == 0x01 and i + 2 < len(buf):
                header = buf[i + 1] | (buf[i + 2] << 8)
                if ((header >> 12) & 0x7) == 0b011:
                    yield i
                    break


def _lval_page_order_blob(data: bytes) -> bytes:
    """Concatenate the data region of every type-0x01 LVAL page in file order.

    ACE stores long values on LVAL pages; some intervening pages (type 0x09,
    non-LVAL) interrupt the linear byte stream. Splicing only the real data
    fragments back together fixes modules broken by such gaps."""
    frags = []
    for n in range(len(data) // PAGE):
        p = data[n * PAGE:(n + 1) * PAGE]
        if p[4:8] == b"LVAL" and p[0] == 0x01:
            free = int.from_bytes(p[2:4], "little")
            row0 = int.from_bytes(p[14:16], "little")
            if row0 + 4 <= PAGE:
                frags.append(p[row0 + 4:PAGE - free])
    return b"".join(frags)


# ---------------------------------------------------------------------------
# Classification
# ---------------------------------------------------------------------------
# Valid VBA text: printable ASCII + tab/CR/LF + high Latin-1 (German umlauts etc).
# Control chars (except whitespace) and NUL only appear on decompression desync.
_PRINTABLE = set(range(32, 127)) | set(range(128, 256)) | {9, 10, 13}
_COMPLETE_ENDINGS = (
    "End Sub", "End Function", "End Property", "End With", "End If",
    "End Type", "End Enum", "Option Explicit", "Option Compare Database",
)


def _is_valid(src: bytes) -> bool:
    return src and src.count(0) == 0 and all(x in _PRINTABLE for x in src)


def _is_complete(text: str) -> bool:
    body = text.rstrip("\r\n \t")
    if not body:
        return False
    last = body.split("\n")[-1].strip()
    return any(last.endswith(e) for e in _COMPLETE_ENDINGS)


def extract(path: str):
    """Return {name: (text, complete_bool)} for every recoverable module."""
    data = open(path, "rb").read()
    best: dict[str, tuple] = {}

    def consider(src: bytes, natural_end: bool):
        if not src:
            return
        m = _NAME_RE.search(src)
        if not m:
            return
        name = m.group(1).decode("latin-1")
        if name.startswith("FA"):            # XMP/image false positive guard
            return
        valid = _is_valid(src)
        text = src.decode("latin-1")
        complete = valid and natural_end and _is_complete(text)
        tier = 2 if complete else (1 if valid else 0)   # complete > valid > corrupt
        rank = (tier, len(text))
        if name not in best or rank > best[name][0]:
            best[name] = (rank, text, complete)

    # pass 1: linear file
    for off in _find_containers(data):
        consider(*decompress_container(data, off))
    # pass 2: LVAL page-order splice
    mega = _lval_page_order_blob(data)
    for off in _find_containers(mega):
        consider(*decompress_container(mega, off))

    return {n: (t, c) for n, (_, t, c) in best.items()}


def _ext(name: str, text: str) -> str:
    if "VB_Creatable" in text or "VB_PredeclaredId" in text or name.startswith(("Form_", "Report_")):
        return ".cls"
    return ".bas"


def write_out(modules: dict, outdir: str):
    comp_dir = os.path.join(outdir, "complete")
    part_dir = os.path.join(outdir, "partial")
    os.makedirs(comp_dir, exist_ok=True)
    os.makedirs(part_dir, exist_ok=True)
    complete, partial = [], []
    for name, (text, is_complete) in sorted(modules.items()):
        d = comp_dir if is_complete else part_dir
        with open(os.path.join(d, name + _ext(name, text)), "w", encoding="utf-8") as f:
            f.write(text)
        (complete if is_complete else partial).append(name)
    return complete, partial


def main(argv=None):
    ap = argparse.ArgumentParser(description="Extract VBA source from an Access .accdb/.mdb")
    ap.add_argument("database", nargs="?", help="path to .accdb / .mdb")
    ap.add_argument("-o", "--outdir", default="vba", help="output directory (default: ./vba)")
    ap.add_argument("--selfcheck", action="store_true", help="run built-in self-test and exit")
    args = ap.parse_args(argv)

    if args.selfcheck:
        _selfcheck()
        return 0
    if not args.database:
        ap.error("database path required (or use --selfcheck)")

    modules = extract(args.database)
    complete, partial = write_out(modules, args.outdir)
    print(f"complete: {len(complete)}   partial (truncated): {len(partial)}")
    if partial:
        print("partial modules (compressed data on non-adjacent LVAL pages):")
        for n in partial:
            print("  ", n)
        print("-> export these from Access: VBE (Alt+F11) -> right-click module -> Export File")
    print(f"written to {args.outdir}/complete and {args.outdir}/partial")
    return 0


def _build_literal_container(payload: bytes) -> bytes:
    """Build a MS-OVBA container encoding `payload` as all-literal tokens
    (one compressed chunk, flag bytes = 0x00). Used only by the self-check."""
    assert len(payload) <= 4096
    body = bytearray()
    for i in range(0, len(payload), 8):
        body.append(0x00)                 # flag byte: next 8 tokens are literals
        body += payload[i:i + 8]
    size = len(body) + 2                   # + 2-byte chunk header
    header = 0b1011_0000_0000_0000 | (size - 3)   # compressed flag + sig 011
    return bytes([0x01, header & 0xFF, (header >> 8) & 0xFF]) + bytes(body)


def _selfcheck():
    """Round-trip a container through the decompressor (framing + literal path)."""
    payload = b'Attribute VB_Name = "mdlDemo"\r\nOption Explicit\r\n'
    out, complete = decompress_container(_build_literal_container(payload), 0)
    assert out == payload, f"decompress mismatch: {out!r} != {payload!r}"
    assert complete, "expected complete container"
    assert _NAME_RE.search(out).group(1) == b"mdlDemo"
    print("selfcheck OK: MS-OVBA container round-trips (framing + literals)")


if __name__ == "__main__":
    sys.exit(main())
