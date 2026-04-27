#!/usr/bin/env bash
# Regression: workbook.close MUST NOT flush the in-memory workbook back to disk and overwrite
# external edits made between MCP open and MCP close.
#
# Reproduced sequence:
#   1. MCP workbook.open(/tmp/x.xlsx)
#   2. external process (simulating Excel / another writer) opens, mutates, saves, closes x.xlsx
#   3. MCP workbook.close(handle)  — WITHOUT any workbook.save beforehand
#   4. assert /tmp/x.xlsx on disk still contains the external edit (md5 + semantic read-back)
#
# With the prior readOnly=false WorkbookFactory.create, step 3 silently overwrote the external
# edit because POI held a read/write OPCPackage handle on the source file.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
FIXTURE="$TMP/shared.xlsx"

# Initial content: A1="before".
cat > "$TMP/MakeInitial.java" <<'EOF'
import java.nio.file.*; import java.io.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
public class MakeInitial {
  public static void main(String[] a) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = Files.newOutputStream(Path.of(a[0]))) {
      Sheet s = wb.createSheet("Data");
      s.createRow(0).createCell(0).setCellValue("before");
      wb.write(os);
    }
  }
}
EOF

# External edit (simulates the other writer): rewrite A1="external" and save in place.
cat > "$TMP/ExternalEdit.java" <<'EOF'
import java.nio.file.*; import java.io.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
public class ExternalEdit {
  public static void main(String[] a) throws Exception {
    Path p = Path.of(a[0]);
    XSSFWorkbook wb;
    try (InputStream in = Files.newInputStream(p)) {
      wb = (XSSFWorkbook) WorkbookFactory.create(in);
    }
    wb.getSheet("Data").getRow(0).getCell(0).setCellValue("external");
    try (OutputStream out = Files.newOutputStream(p)) {
      wb.write(out);
    }
    wb.close();
  }
}
EOF

( cd "$TMP" && javac -cp "$JAR" MakeInitial.java ExternalEdit.java \
    && java -cp "$JAR:$TMP" MakeInitial "$FIXTURE" )

python3 - "$JAR" "$FIXTURE" "$TMP" <<'PY'
import hashlib, os, subprocess, sys
sys.path.insert(0, os.environ["SMOKE_DIR"])
from _smoke_common import start, handshake, call

jar, fixture, tmp = sys.argv[1], sys.argv[2], sys.argv[3]

def md5(path):
    h = hashlib.md5()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 16), b""):
            h.update(chunk)
    return h.hexdigest()

p = start(jar)
try:
    handshake(p, "close-regression")

    # Step 1: MCP opens the shared file.
    opened = call(p, 10, "workbook.open", {"path": fixture})
    h = opened["handle"]

    # Sanity: MCP sees A1="before".
    pre = call(p, 11, "range.get", {"handle": h, "sheet": "Data", "range": "A1"})
    assert pre["cells"][0][0]["value"] == "before", pre

    # Step 2: external process mutates the same file while MCP still holds its handle.
    subprocess.run(
        ["java", "-cp", f"{jar}:{tmp}", "ExternalEdit", fixture],
        check=True, capture_output=True)

    # Capture the md5 right after the external edit; compared against the post-close md5 below.
    external_md5 = md5(fixture)

    # Step 3: MCP closes the handle — MUST NOT overwrite the external edit.
    call(p, 12, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except Exception: p.kill()

# Step 4a: md5 on disk after MCP close must equal the md5 captured right after the external edit.
post_close_md5 = md5(fixture)
assert post_close_md5 == external_md5, (
    f"DATA LOSS: MCP workbook.close overwrote external edit.\n"
    f"  external md5: {external_md5}\n"
    f"  post-close md5: {post_close_md5}")

# Step 4b: semantic readback confirms the external edit is still present.
readback_src = """
import java.nio.file.*; import java.io.*;
import org.apache.poi.ss.usermodel.*;
public class ReadA1 {
  public static void main(String[] a) throws Exception {
    try (InputStream in = Files.newInputStream(Path.of(a[0]))) {
      Workbook wb = WorkbookFactory.create(in);
      String v = wb.getSheet("Data").getRow(0).getCell(0).getStringCellValue();
      System.out.print(v);
      wb.close();
    }
  }
}
"""
with open(os.path.join(tmp, "ReadA1.java"), "w") as f:
    f.write(readback_src)
subprocess.run(["javac", "-cp", jar, os.path.join(tmp, "ReadA1.java")], check=True)
out = subprocess.run(
    ["java", "-cp", f"{jar}:{tmp}", "ReadA1", fixture],
    check=True, capture_output=True, text=True)
assert out.stdout == "external", f"expected A1='external', got {out.stdout!r}"

print("Regression smoke OK: workbook.close did not overwrite external edits.")
PY
