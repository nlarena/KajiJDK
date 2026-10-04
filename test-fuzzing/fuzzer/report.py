# -*- coding: utf-8 -*-
"""Turn the campaign files into the baseline report.

    python test-fuzzing/fuzzer/report.py --out test-fuzzing/BASELINE.md

Reads every `datos/baseline-q*.json`, which is one pass of `libfuzz.py` over the whole operation
pool, and writes the report in Markdown.

One honest limit, stated here and in the output: the campaigns count divergences per operation but
not **attempts** per operation, so a true divergence rate per package is not available. What the
per-package table gives instead is the count, next to how many operations that package contributes
to the pool. Over one complete pass each operation receives roughly one case, so the two columns
are comparable -- but it is an approximation, not a measurement, and the fix for next time is to
count attempts per package while the campaign runs.
"""
import argparse
import collections
import glob
import io
import json
import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from policy import Policy                                   # noqa: E402


def pool_by_package(surface_path):
    """How many operations each package contributes, so counts can be read next to size."""
    doc = json.load(io.open(surface_path, encoding="utf-8"))
    pol = Policy()
    per = collections.Counter()
    for cls, c in doc["classes"].items():
        if pol.reject_class(cls):
            continue
        for o in c["ops"]:
            if pol.reject_member(cls, o["name"]):
                continue
            per[".".join(cls.split(".")[:2])] += 1
    return per


def load(pattern):
    runs = []
    for p in sorted(glob.glob(pattern)):
        d = json.load(io.open(p, encoding="utf-8"))
        runs.append((os.path.basename(p), d))
    return runs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default=str(HERE.parent / "datos"))
    ap.add_argument("--pattern", default="baseline-q*.json")
    ap.add_argument("--surface", default=str(HERE / "surface.json"))
    ap.add_argument("--out", default=str(HERE.parent / "BASELINE.md"))
    a = ap.parse_args()

    runs = load(os.path.join(a.data, a.pattern))
    if not runs:
        print("no campaign files matched", file=sys.stderr)
        sys.exit(1)

    tot = collections.Counter()
    kinds, ops = collections.Counter(), collections.Counter()
    unreachable, vms, divergences = [], set(), []
    for _, d in runs:
        s = d["stats"]
        for k in ("cases", "agree", "diverge", "repeats", "unreachable", "no_reference",
                  "unattributed", "rewarded"):
            tot[k] += s.get(k, 0)
        tot["seconds"] += s.get("total_s", 0)
        kinds.update(s["by_kind"])
        ops.update(s.get("by_operation", {}))
        unreachable.extend(d.get("unreachable_sample", []))
        vms.add(d["meta"].get("vm_sha256", "?"))
        divergences.extend(d["divergences"])

    per_pkg_ops = pool_by_package(a.surface)
    per_pkg_div = collections.Counter()
    for k, v in ops.items():
        per_pkg_div[".".join(k.split(".")[:2])] += v

    first = runs[0][1]["meta"]
    L = []
    L.append("# Baseline: one pass over KajiLibrary")
    L.append("")
    L.append("Produced by `test-fuzzing/fuzzer/report.py` from the campaign files in")
    L.append("`test-fuzzing/datos/`. Everything below is differential: the same compiled `.class`")
    L.append("runs on the reference JDK and on KajiJDK, and only values the specification pins")
    L.append("down are compared.")
    L.append("")
    L.append("| | |")
    L.append("|---|---|")
    L.append("| reference | `%s` |" % first.get("reference", "?"))
    L.append("| under test | `%s` |" % first.get("under_test", "?"))
    L.append("| VM binary | `%s` |" % ", ".join(sorted(x[:16] for x in vms)))
    # Each campaign records the size of ITS shard, not of the whole pool.
    shards = {(d["meta"].get("shard"), d["meta"].get("pool_operations")) for _, d in runs}
    L.append("| operation pool | %d (%d queues) |"
             % (sum(n for _, n in shards if n), len(shards)))
    L.append("| campaigns | %d |" % len(runs))
    L.append("| wall time | %.0f min |" % (tot["seconds"] / 60.0))
    L.append("")
    L.append("## What happened")
    L.append("")
    L.append("| | count |")
    L.append("|---|---|")
    L.append("| cases compared | %d |" % tot["cases"])
    L.append("| agreed | %d |" % tot["agree"])
    L.append("| **diverged** | **%d (%.1f%%)** |"
             % (tot["diverge"], 100.0 * tot["diverge"] / max(tot["cases"], 1)))
    L.append("| skipped as duplicate skeletons | %d |" % tot["repeats"])
    L.append("| operations the fuzzer could not reach | %d |" % tot["unreachable"])
    L.append("| cases the reference did not answer | %d |" % tot["no_reference"])
    L.append("| divergences that could not be attributed | %d |" % tot["unattributed"])
    L.append("")
    L.append("## How they diverged")
    L.append("")
    L.append("| kind | count | share |")
    L.append("|---|---|---|")
    for k, v in kinds.most_common():
        L.append("| %s | %d | %.0f%% |" % (k, v, 100.0 * v / max(tot["diverge"], 1)))
    L.append("")
    L.append("`more specific exception` is the low-priority bucket: throwing a subclass of what")
    L.append("the specification promises satisfies the specification. `threw at a different")
    L.append("statement` means both sides failed but one got further.")
    L.append("")
    L.append("## Where they are")
    L.append("")
    L.append("Divergences are charged to the **statement that produced the differing value**, not")
    L.append("to the operation whose turn it was. The `pool` column is how many operations the")
    L.append("package contributes, so the two numbers can be read together; it is not a rate,")
    L.append("because attempts per package were not counted.")
    L.append("")
    L.append("| package | divergences | pool |")
    L.append("|---|---|---|")
    for pkg, n in per_pkg_div.most_common(20):
        L.append("| `%s` | %d | %d |" % (pkg, n, per_pkg_ops.get(pkg, 0)))
    L.append("")
    L.append("## Operations blamed most often")
    L.append("")
    L.append("| operation | divergences |")
    L.append("|---|---|")
    for k, v in ops.most_common(25):
        L.append("| `%s` | %d |" % (k, v))
    L.append("")
    L.append("## A sample of what the divergences look like")
    L.append("")
    seen = set()
    shown = 0
    for x in divergences:
        key = x.get("blamed")
        if not key or key in seen or x["kind"] == "no output (crash or hang)":
            continue
        seen.add(key)
        shown += 1
        if shown > 20:
            break
        L.append("**`%s`** — %s" % (key, x["kind"]))
        L.append("")
        L.append("```java")
        L.append(x["java"][:300])
        L.append("```")
        L.append("")
        L.append("| | |")
        L.append("|---|---|")
        L.append("| reference | `%s` |" % str(x["ref"])[:110].replace("|", "\\|"))
        L.append("| KajiJDK | `%s` |" % str(x["ours"])[:110].replace("|", "\\|"))
        L.append("")
    L.append("## What was not tested")
    L.append("")
    L.append("%d operations could not be reached: no case could be built for them, because an" % tot["unreachable"])
    L.append("argument type has no producer or because nothing in the result is comparable. These")
    L.append("are holes in coverage, not passes.")
    L.append("")
    for u in sorted(set(unreachable))[:30]:
        L.append("- `%s`" % u)
    L.append("")
    io.open(a.out, "w", encoding="utf-8", newline="").write("\n".join(L) + "\n")
    print("written %s (%d cases, %d divergences, %d campaigns)"
          % (a.out, tot["cases"], tot["diverge"], len(runs)))


if __name__ == "__main__":
    main()
