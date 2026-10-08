# -*- coding: utf-8 -*-
"""Materialize a frozen measurement target for the fuzzer.

A campaign measures two things at once -- how well the generator finds defects, and how many
defects are left to find -- and they move independently. Fixing a library bug lowers the
divergence count without the generator changing at all, so "the fuzzer improved" and "there is
less to find" are indistinguishable unless the subject is held still. This builds the subject:
a copy of KajiLibrary, the VM binary, and the surface map, all taken at one named commit, with
a provenance record of exactly what they are.

What it does NOT freeze is the reference JDK. That is external, versioned by its own installer
and already stable; `--jdk` names it in every campaign and the provenance records which one.

Usage:
    python test-fuzzing/fuzzer/freeze.py <commit> [--dest DIR] [--jdk PATH]
    python test-fuzzing/fuzzer/freeze.py --verify [--dest DIR]

The destination is gitignored on purpose: it is 85 MB of derived bytes, and the project's rule
for derived artifacts is to keep the recipe and the provenance rather than the bytes -- the same
reasoning `bin/FROZEN.md` writes down for the frozen tools. This script is the recipe and
`PROVENANCE.json` is the provenance.
"""
import argparse
import datetime
import hashlib
import io
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
DEFAULT_DEST = REPO / "test-fuzzing" / "frozen-target"


def run(cmd, cwd=None, check=True):
    p = subprocess.run(cmd, cwd=str(cwd or REPO), capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    if check and p.returncode != 0:
        raise SystemExit("failed: %s\n%s%s" % (" ".join(map(str, cmd)), p.stdout, p.stderr))
    return p


def digest(path):
    h = hashlib.sha256()
    with open(str(path), "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def tree_digest(root):
    """One digest over every .class under `root`, path included.

    Paths are part of it because a class moving between packages changes what the VM resolves
    even when no single file's bytes changed.
    """
    h = hashlib.sha256()
    for p in sorted(Path(root).rglob("*.class")):
        h.update(str(p.relative_to(root)).replace(os.sep, "/").encode("utf-8"))
        h.update(digest(p).encode("ascii"))
    return h.hexdigest()


def resolve_commit(ref):
    out = run(["git", "rev-parse", ref]).stdout.strip()
    if not out:
        raise SystemExit("cannot resolve commit: " + ref)
    return out


def dirty_paths(commit, paths):
    """Which of `paths` differ between the working tree and `commit`."""
    p = run(["git", "diff", "--name-only", commit, "--"] + list(paths), check=False)
    return [l for l in p.stdout.splitlines() if l.strip()]


def extract_library(commit, dest):
    lib = dest / "KajiLibrary"
    if lib.exists():
        shutil.rmtree(str(lib))
    dest.mkdir(parents=True, exist_ok=True)
    # `git archive | tar -x` rather than a copy of the working tree: the point is the library as
    # that commit has it, not as this checkout happens to have it.
    tar = subprocess.Popen(["tar", "-x", "-C", str(dest)], stdin=subprocess.PIPE)
    arch = subprocess.Popen(["git", "archive", commit, "KajiLibrary"], cwd=str(REPO),
                            stdout=tar.stdin)
    arch.wait()
    tar.stdin.close()
    tar.wait()
    if arch.returncode != 0 or tar.returncode != 0:
        raise SystemExit("git archive | tar failed")
    n = len(list(lib.rglob("*.class")))
    if n < 1000:
        raise SystemExit("only %d .class extracted; the archive step did not work" % n)
    return lib, n


def copy_vm(commit, dest):
    """The VM binary for the frozen target.

    It is copied from `target/release`, not rebuilt here: rebuilding needs a clean checkout of
    the commit and several minutes, and FROZEN-TARGET.md spells that out for when the snapshot
    has to be recreated from scratch. What this does enforce is that the binary is not silently
    from a different commit than the library -- when the VM sources differ from `commit` it
    refuses, because a snapshot whose two halves come from different places is worse than none.
    """
    src = REPO / "target" / "release" / "run-headless.exe"
    if not src.exists():
        raise SystemExit("no run-headless.exe in target/release; cargo build --release first")
    moved = dirty_paths(commit, ["src", "Cargo.toml", "Cargo.lock"])
    if moved and not ARGS.allow_dirty:
        raise SystemExit(
            "the VM sources differ from %s in %d files (%s...).\n"
            "The binary in target/release was not built from that commit, so its pedigree is\n"
            "not that commit and recording it as such would be a lie. Rebuild from a clean\n"
            "checkout (see FROZEN-TARGET.md), or pass --allow-dirty to freeze the binary as it\n"
            "is: the snapshot still works as a fixed target, and the provenance will say that\n"
            "it cannot be recreated from the commit alone."
            % (commit[:12], len(moved), ", ".join(moved[:3])))
    out = dest / "run-headless.exe"
    shutil.copy2(str(src), str(out))
    return out, moved


def build_surface(lib, jdk, dest):
    out = dest / "surface.json"
    print("  reading the surface with javap (a couple of minutes)...")
    p = run([sys.executable, str(HERE / "surface.py"), "--jdk", str(jdk),
             "--lib", str(lib), "--out", str(out)], check=False)
    if not out.exists():
        raise SystemExit("surface.py did not produce %s\n%s%s" % (out, p.stdout, p.stderr))
    return out


def smoke(dest, lib, vm):
    """Prove the snapshot runs before anyone measures against it."""
    probe = dest / "Probe.java"
    io.open(str(probe), "w", encoding="utf-8", newline="").write(
        "public class Probe {\n"
        "  public static void main(String[] a) {\n"
        "    System.out.println(\"frozen \" + \"ab\".length()\n"
        "        + java.util.regex.Pattern.compile(\"a\").matcher(\"a\").matches());\n"
        "  }\n"
        "}\n")
    run([str(Path(ARGS.jdk) / "bin" / "javac.exe"), "-d", str(dest), str(probe)])
    p = run([str(vm), str(dest / "Probe.class"), "main", "--boot", str(lib)], check=False)
    if "frozen 2true" not in (p.stdout + p.stderr):
        raise SystemExit("the frozen target does not run:\n%s%s" % (p.stdout, p.stderr))
    return "frozen 2true"


def main():
    global ARGS
    ap = argparse.ArgumentParser()
    ap.add_argument("commit", nargs="?", default="HEAD")
    ap.add_argument("--dest", default=str(DEFAULT_DEST))
    ap.add_argument("--jdk", default=r"C:/Program Files/Java/jdk-25")
    ap.add_argument("--allow-dirty", action="store_true",
                    help="freeze the VM binary even when it was not built from `commit`. The "
                         "snapshot still works as a fixed target; the provenance records that "
                         "it is not reproducible from the commit alone")
    ap.add_argument("--verify", action="store_true",
                    help="re-check an existing snapshot against its own provenance")
    ARGS = ap.parse_args()
    dest = Path(ARGS.dest).resolve()

    if ARGS.verify:
        rec = json.load(io.open(str(dest / "PROVENANCE.json"), encoding="utf-8"))
        lib, vm = dest / "KajiLibrary", dest / "run-headless.exe"
        bad = []
        if tree_digest(lib) != rec["library_sha256"]:
            bad.append("the library has changed")
        if digest(vm) != rec["vm_sha256"]:
            bad.append("the VM binary has changed")
        if digest(dest / "surface.json") != rec["surface_sha256"]:
            bad.append("surface.json has changed")
        if bad:
            raise SystemExit("NOT frozen any more: " + "; ".join(bad))
        print("frozen target intact: commit %s, %d classes, taken %s"
              % (rec["commit"][:12], rec["library_classes"], rec["when"]))
        return

    commit = resolve_commit(ARGS.commit)
    print("freezing %s into %s" % (commit[:12], dest))
    lib, n = extract_library(commit, dest)
    print("  library: %d classes" % n)
    vm, vm_moved = copy_vm(commit, dest)
    print("  VM: %s%s"
          % (digest(vm)[:12],
             "  (built from a tree with %d uncommitted files)" % len(vm_moved)
             if vm_moved else ""))
    surf = build_surface(lib, ARGS.jdk, dest)
    print("  surface: %s" % surf.name)
    print("  smoke test: %s" % smoke(dest, lib, vm))

    rec = {
        "when": datetime.datetime.now().isoformat(timespec="seconds"),
        "commit": commit,
        "commit_subject": run(["git", "log", "-1", "--format=%s", commit]).stdout.strip(),
        "library_classes": n,
        "library_sha256": tree_digest(lib),
        "vm_sha256": digest(vm),
        # Empty means the binary really is `commit`, so the snapshot can be rebuilt from it.
        # Non-empty names the files that had uncommitted changes when it was built, which is
        # the difference between "fixed" and "reproducible": still a stable subject to measure
        # against, but recreating this exact one needs these bytes, not just the commit.
        "vm_uncommitted": sorted(vm_moved),
        "reproducible_from_commit": not vm_moved,
        "surface_sha256": digest(surf),
        "reference_jdk": ARGS.jdk,
        "built_by": "test-fuzzing/fuzzer/freeze.py",
    }
    io.open(str(dest / "PROVENANCE.json"), "w", encoding="utf-8", newline="").write(
        json.dumps(rec, indent=2) + "\n")
    print("\nwritten %s" % (dest / "PROVENANCE.json"))
    print("\nmeasure against it with:")
    print("  python test-fuzzing/fuzzer/libfuzz.py --jdk \"%s\" \\" % ARGS.jdk)
    print("      --vm %s \\" % (dest / "run-headless.exe"))
    print("      --boot %s \\" % (dest / "KajiLibrary"))
    print("      --surface %s --seed 1 --batches 8 --per-batch 300" % (dest / "surface.json"))


ARGS = None

if __name__ == "__main__":
    main()
