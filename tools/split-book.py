#!/usr/bin/env python3
# This file is part of Gottsch Books.
# Copyright (c) 2026 Mark Gottschling (gottsch)
#
# Gottsch Books is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Gottsch Books is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with Gottsch Books.  If not, see <http://www.gnu.org/licenses/lgpl>.
"""
Writes the files of a book from an AI's answers.

An AI following AI-INSTRUCTIONS.md gives each file as its path on a line of its own, then the file in a fenced block:

    assets/mymod/books/guide/en_us/widgets.md
    ````markdown
    ---
    title: Widgets
    ---
    ...
    ````

Save the AI's answers as text files (copy each whole answer into a .txt file) and run:

    python split-book.py answer1.txt answer2.txt --out path/to/src/main/resources

Each file is written under --out (the current folder if not given). A path must be relative and sit under an
assets/ or data/ folder (or be a pack.mcmeta); anything else is skipped, so an answer can never write elsewhere on
your computer. --dry-run lists what would be written. Needs only Python 3.8 or later.
"""
import argparse
import re
import sys
from pathlib import Path, PurePosixPath

# a path on a line of its own, possibly in backticks or bold, possibly after "File:" or a heading mark
PATH_LINE = re.compile(r"^\s*(?:#+\s*)?(?:\*\*)?(?:file:\s*)?`?(?P<path>[A-Za-z0-9_./-]+\.(?:md|json|mcmeta))`?(?:\*\*)?:?\s*$",
                       re.IGNORECASE)
FENCE_OPEN = re.compile(r"^\s*(?P<fence>`{3,}|~{3,})\s*[A-Za-z0-9_-]*\s*$")


def allowed(path: str) -> bool:
    """relative, no '..', and under an assets/ or data/ folder, or a pack.mcmeta"""
    p = PurePosixPath(path)
    if p.is_absolute() or ".." in p.parts or path.startswith("/") or ":" in path:
        return False
    return p.name == "pack.mcmeta" or "assets" in p.parts[:-1] or "data" in p.parts[:-1]


def files(text: str):
    """(path, content, warning) for each path line followed by a fenced block"""
    lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    i = 0
    while i < len(lines):
        m = PATH_LINE.match(lines[i])
        if not m:
            i += 1
            continue
        path = m.group("path")
        j = i + 1
        while j < len(lines) and not lines[j].strip():
            j += 1
        fence = FENCE_OPEN.match(lines[j]) if j < len(lines) else None
        if not fence:
            i += 1
            continue
        mark = fence.group("fence")
        # it closes on a line of the same fence character, at least as long, and nothing else
        close = re.compile(r"^\s*" + re.escape(mark[0]) + "{" + str(len(mark)) + r",}\s*$")
        k = j + 1
        body = []
        while k < len(lines) and not close.match(lines[k]):
            body.append(lines[k])
            k += 1
        warning = None
        if k >= len(lines):
            warning = "its block never closes; the answer may have been cut off"
        elif len(mark) == 3 and path.endswith(".md") and any(l.strip().startswith("```") for l in body):
            warning = "it was fenced with three backticks, so a block inside it may have ended it early"
        yield path, "\n".join(body).rstrip("\n") + "\n", warning
        i = k + 1


def main() -> int:
    parser = argparse.ArgumentParser(description="Write a Gottsch Books book's files from an AI's answers.")
    parser.add_argument("answers", nargs="*", help="text files holding the AI's answers (stdin if none)")
    parser.add_argument("--out", default=".", help="the folder the paths are written under (default: here)")
    parser.add_argument("--dry-run", action="store_true", help="only list the files that would be written")
    args = parser.parse_args()

    if args.answers:
        texts = [Path(a).read_text(encoding="utf-8-sig") for a in args.answers]
    else:
        texts = [sys.stdin.read()]

    out = Path(args.out)
    written = skipped = 0
    seen = {}
    for text in texts:
        for path, content, warning in files(text):
            if not allowed(path):
                print(f"skipped  {path}  (not under assets/ or data/, or not a safe relative path)")
                skipped += 1
                continue
            target = out.joinpath(*PurePosixPath(path).parts)
            if path in seen:
                note = "again (the later version wins)"
            elif target.exists():
                note = "replaced"
            else:
                note = "new"
            seen[path] = True
            if warning:
                note += f"; check it: {warning}"
            if not args.dry_run:
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(content, encoding="utf-8", newline="\n")
            print(f"{'would write' if args.dry_run else 'wrote'}  {path}  ({note})")
            written += 1

    if written == 0 and skipped == 0:
        print("No files found. Each file should be its path on a line of its own, then a fenced block.")
        return 1
    print(f"{written} file(s) {'to write' if args.dry_run else 'written'} under {out.resolve()}"
          + (f", {skipped} skipped" if skipped else ""))
    if not args.dry_run and written:
        print("Next: start the game (or press F3+T in it) and run /gottschbooks check")
    return 0


if __name__ == "__main__":
    sys.exit(main())
