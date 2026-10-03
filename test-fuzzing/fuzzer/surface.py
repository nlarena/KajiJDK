# -*- coding: utf-8 -*-
"""Discover the comparable API surface of the WHOLE KajiLibrary, class by class.

    python test-fuzzing/fuzzer/surface.py --jdk "$OPENJDK"
    python test-fuzzing/fuzzer/surface.py --jdk "$OPENJDK" --packages java.util java.text

The earlier fuzzers were pointed at one or two classes named on the command line. This walks the
library instead: every top-level `.class` under `KajiLibrary/` becomes a candidate, and what comes
out is the set of members that **both** implementations declare with the same descriptor. That
intersection is the only thing a differential campaign can compare; everything else belongs in the
coverage report, not in the results.

Three things this has to get right, and all three bit an earlier version:

**Read our side by FILE PATH, never by class name.** `javap -cp KajiLibrary java.lang.String`
returns the *platform* class, because `java.*` resolves from the system modules before the class
path. A tool that gets this wrong compares the JDK against itself and reports perfect parity.

**Read with `-v`, not `-s`, and drop `ACC_BRIDGE` / `ACC_SYNTHETIC`.** A class that implements
`Comparable<T>` carries a bridge `compareTo(Object)` that does not exist in the source; generating
a call from its erased descriptor makes javac reject the whole batch.

**Batch the javap calls.** One process per class is unusable at this scale: 200 classes in one
invocation take about three seconds, so the whole library costs a couple of minutes instead of an
hour. The result is cached, because nothing here changes unless the library is recompiled.
"""
import argparse
import io
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
KAJILIB = REPO / "KajiLibrary"

# Members are indented by exactly two spaces; their `descriptor:` and `flags:` lines by more.
DECL = re.compile(r"^ {2}(?! )(?!#)(.*);\s*$")
DESC = re.compile(r"^\s+descriptor:\s*(\S+)\s*$")
FLAGS = re.compile(r"^\s+flags:\s*\(0x[0-9a-fA-F]+\)\s*(.*)$")
HEAD = re.compile(r"^(?:public |final |abstract |static |strictfp |sealed |non-sealed )*"
                  r"(class|interface|enum|record|@interface)\s+([\w.$]+)")
MODS = {"public", "protected", "private", "static", "final", "abstract", "native",
        "synchronized", "transient", "volatile", "strictfp", "default"}


def run_javap(javap, targets, extra=()):
    """javap over many targets at once. Targets are file paths (ours) or class names (reference)."""
    cmd = [str(javap), "-v", "-p"] + list(extra) + [str(t) for t in targets]
    p = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return p.stdout


def split_classes(text):
    """javap concatenates its reports. Cut them apart at each `Classfile`/`Compiled from` header."""
    chunks, current = [], []
    for line in text.splitlines():
        if line.startswith("Classfile ") or line.startswith('Compiled from "'):
            if current:
                chunks.append(current)
            current = [line]
        elif current:
            current.append(line)
    if current:
        chunks.append(current)
    return chunks


def parse_class(lines):
    """One javap report -> {name, kind, abstract, members: {(name, descriptor): mods}}."""
    name, kind, is_abstract = None, None, False
    for line in lines:
        m = HEAD.match(line)
        if m:
            kind, name = m.group(1), m.group(2)
            is_abstract = line.startswith("abstract ") or " abstract " in (" " + line)
            break
    if name is None:
        return None
    members = {}
    pending = last = None
    for line in lines:
        if " = " in line:                      # constant-pool entry, not a member
            continue
        m = DECL.match(line)
        if m:
            pending, last = m.group(1).strip(), None
            continue
        m = DESC.match(line)
        if m and pending is not None:
            tokens = pending.split("(")[0].split()
            if "(" in pending and tokens:
                member = tokens[-1]
                if member == name or member == name.rsplit(".", 1)[-1]:
                    member = "<init>"
                elif "." in member:
                    member = member.rsplit(".", 1)[-1]
                last = (member, m.group(1))
                members[last] = {t for t in tokens if t in MODS}
            pending = None
            continue
        m = FLAGS.match(line)
        if m and last is not None:
            if "ACC_BRIDGE" in m.group(1) or "ACC_SYNTHETIC" in m.group(1):
                members.pop(last, None)
            last = None
    return {"name": name, "kind": kind, "abstract": is_abstract, "members": members}


def candidates(packages, include_nested):
    """Top-level `.class` files under KajiLibrary, as (class name, path). `repros` is not library."""
    out = []
    for root, _, files in os.walk(str(KAJILIB)):
        rel = os.path.relpath(root, str(KAJILIB)).replace("\\", "/")
        if rel.startswith("repros"):
            continue
        for f in files:
            if not f.endswith(".class"):
                continue
            if "$" in f and not include_nested:
                continue
            name = ((rel + "/" + f) if rel != "." else f)[:-len(".class")].replace("/", ".")
            if packages and not any(name == p or name.startswith(p + ".") for p in packages):
                continue
            out.append((name, os.path.join(root, f)))
    out.sort()
    return out


def discover(javap, packages, include_nested, batch, report=print):
    cands = candidates(packages, include_nested)
    report("surface: %d candidate classes under KajiLibrary" % len(cands))
    ours, theirs = {}, {}
    t0 = time.time()
    for i in range(0, len(cands), batch):
        part = cands[i:i + batch]
        for chunk in split_classes(run_javap(javap, [p for _, p in part])):
            info = parse_class(chunk)
            if info:
                ours[info["name"]] = info
        for chunk in split_classes(run_javap(javap, [n for n, _ in part])):
            info = parse_class(chunk)
            if info:
                theirs[info["name"]] = info
        report("  %d/%d classes read (%.0fs)" % (min(i + batch, len(cands)), len(cands),
                                                 time.time() - t0))
    return cands, ours, theirs


def build(cands, ours, theirs):
    """Intersect the two sides. Returns the per-class records plus a library-wide tally."""
    classes, tally = {}, {"only_ours": [], "missing_reference": []}
    for name, _ in cands:
        a, b = ours.get(name), theirs.get(name)
        if a is None:
            continue
        if b is None:
            tally["missing_reference"].append(name)
            continue
        pub_a = {k for k, v in a["members"].items() if "public" in v}
        pub_b = {k for k, v in b["members"].items() if "public" in v}
        common = pub_a & pub_b
        ops = []
        for member, desc in sorted(common):
            ops.append({"name": member, "desc": desc,
                        "static": "static" in a["members"][(member, desc)],
                        "ctor": member == "<init>"})
        classes[name] = {
            "kind": a["kind"], "abstract": a["abstract"],
            "public_ours": len(pub_a), "public_reference": len(pub_b), "common": len(common),
            "only_ours": sorted(n + d for n, d in (pub_a - pub_b)),
            "only_reference": sorted(n + d for n, d in (pub_b - pub_a)),
            "declares_toString": ("toString", "()Ljava/lang/String;") in common,
            "ops": ops,
        }
        if pub_a - pub_b:
            tally["only_ours"].append(name)
    return classes, tally


def summarise(classes, tally, report=print):
    by_pkg = {}
    for name, c in classes.items():
        pkg = ".".join(name.split(".")[:2])
        d = by_pkg.setdefault(pkg, [0, 0, 0, 0])
        d[0] += 1
        d[1] += c["common"]
        d[2] += len(c["only_reference"])
        d[3] += len(c["only_ours"])
    report("")
    report("%-26s %7s %9s %12s %10s" % ("package", "classes", "common", "only-ref", "only-ours"))
    report("-" * 68)
    for pkg in sorted(by_pkg, key=lambda k: -by_pkg[k][1])[:24]:
        n, c, r, o = by_pkg[pkg]
        report("%-26s %7d %9d %12d %10d" % (pkg, n, c, r, o))
    report("-" * 68)
    report("%-26s %7d %9d %12d %10d"
           % ("TOTAL (%d packages)" % len(by_pkg), sum(v[0] for v in by_pkg.values()),
              sum(v[1] for v in by_pkg.values()), sum(v[2] for v in by_pkg.values()),
              sum(v[3] for v in by_pkg.values())))
    report("")
    report("classes KajiLibrary has that the reference does not: %d"
           % len(tally["missing_reference"]))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jdk", required=True, help="reference JDK directory")
    ap.add_argument("--packages", nargs="*", default=None,
                    help="limit to these package prefixes; default is the whole library")
    ap.add_argument("--nested", action="store_true", help="include nested classes (Outer$Inner)")
    ap.add_argument("--batch", type=int, default=200, help="classes per javap invocation")
    ap.add_argument("--out", default=str(HERE / "surface.json"))
    a = ap.parse_args()

    javap = Path(a.jdk) / "bin" / "javap.exe"
    if not javap.exists():
        javap = Path(a.jdk) / "bin" / "javap"
    t0 = time.time()
    cands, ours, theirs = discover(javap, a.packages, a.nested, a.batch)
    classes, tally = build(cands, ours, theirs)
    summarise(classes, tally)
    doc = {"meta": {"jdk": a.jdk, "packages": a.packages, "nested": a.nested,
                    "candidates": len(cands), "classes": len(classes),
                    "seconds": round(time.time() - t0, 1)},
           "tally": tally, "classes": classes}
    io.open(a.out, "w", encoding="utf-8", newline="").write(
        json.dumps(doc, ensure_ascii=False) + "\n")
    print("")
    print("written %s (%.1f MB, %.0fs)"
          % (a.out, os.path.getsize(a.out) / 1048576.0, time.time() - t0))


if __name__ == "__main__":
    main()
