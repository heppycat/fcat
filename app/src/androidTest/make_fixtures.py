#!/usr/bin/env python3
"""Writes the PDFs in assets/ used by the instrumented engine tests.

Run from this directory: python3 make_fixtures.py
Needs qpdf for the encrypted copy. The outputs are committed, so this only reruns when a fixture changes.

fixture.pdf
  Page 1: 595 x 842 pt. Black square, top-left (100, 100), bottom-right (200, 200), measured from the
          page's top-left corner. Text "Hello fcat" in Helvetica.
  Page 2: 842 x 595 pt (landscape). Text "Second page".
  Outline: "Chapter 1" -> page 1, with child "Section 1.1" -> page 2; "Website" -> external URI.
encrypted.pdf
  fixture.pdf with AES-256 encryption, user password "fcat".
"""

import subprocess
from pathlib import Path

ASSETS = Path(__file__).parent / "assets"


def content(ops: str) -> bytes:
    data = ops.encode("latin-1")
    return b"<< /Length %d >>\nstream\n" % len(data) + data + b"\nendstream"


def build() -> bytes:
    # PDF user space has its origin at the bottom-left, so the square at top-left y = 100..200
    # on an 842 pt page sits at y = 642..742.
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R /Outlines 9 0 R /PageMode /UseOutlines >>",
        b"<< /Type /Pages /Kids [3 0 R 4 0 R] /Count 2 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 5 0 R"
        b" /Resources << /Font << /F1 7 0 R >> >> >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Contents 6 0 R"
        b" /Resources << /Font << /F1 7 0 R >> >> >>",
        content("0 g 100 642 100 100 re f\nBT /F1 24 Tf 72 770 Td (Hello fcat) Tj ET"),
        content("BT /F1 24 Tf 72 500 Td (Second page) Tj ET"),
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
        b"null",  # 8: unused
        b"<< /Type /Outlines /First 10 0 R /Last 12 0 R /Count 3 >>",
        b"<< /Title (Chapter 1) /Parent 9 0 R /Next 12 0 R /First 11 0 R /Last 11 0 R /Count 1"
        b" /Dest [3 0 R /Fit] >>",
        b"<< /Title (Section 1.1) /Parent 10 0 R /Dest [4 0 R /Fit] >>",
        b"<< /Title (Website) /Parent 9 0 R /Prev 10 0 R"
        b" /A << /S /URI /URI (https://example.org/) >> >>",
    ]

    out = bytearray(b"%PDF-1.7\n%\xe2\xe3\xcf\xd3\n")
    offsets = []
    for number, body in enumerate(objects, start=1):
        offsets.append(len(out))
        out += b"%d 0 obj\n" % number + body + b"\nendobj\n"
    xref = len(out)
    out += b"xref\n0 %d\n0000000000 65535 f \n" % (len(objects) + 1)
    for offset in offsets:
        out += b"%010d 00000 n \n" % offset
    out += b"trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n" % (len(objects) + 1, xref)
    return bytes(out)


def main() -> None:
    ASSETS.mkdir(exist_ok=True)
    plain = ASSETS / "fixture.pdf"
    plain.write_bytes(build())
    subprocess.run(["qpdf", "--check", str(plain)], check=True, stdout=subprocess.DEVNULL)
    # AES-256 uses random salts, so this file differs on every run; fixture.pdf does not.
    subprocess.run(
        ["qpdf", "--encrypt", "fcat", "owner", "256", "--", str(plain), str(ASSETS / "encrypted.pdf")],
        check=True,
    )


if __name__ == "__main__":
    main()
