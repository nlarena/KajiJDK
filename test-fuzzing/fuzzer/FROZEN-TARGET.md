# `frozen-target/` — the fixed subject the fuzzer is measured against

## Why it exists

A campaign measures two things at once, and they move independently:

- how well the generator finds defects, and
- how many defects are left to find.

Fixing a library bug lowers the divergence count without the generator changing at all. So
"the fuzzer got better" and "there is less left to find" produce the same number, and nothing in
a campaign's output tells them apart.

This became concrete on 2026-10-07. Three defects were fixed in `java.util.regex` that day
(the unvalidated flag mask, and the inline flag groups the parser rejected), and a regex-focused
campaign went from 135 divergences to 124. None of that drop was the fuzzer. In the same
session, a campaign had already been invalidated mid-run because another session rebuilt
`target/release/run-headless.exe` underneath it — `libfuzz.py` now checks the binary's digest
between batches and warns, which is how that was caught, but warning after the fact does not
recover the measurement.

`BASELINE.md`, which exists to be the reference every improvement is compared against, was
measured before any of this. Comparisons against it are confounded by every library fix since.

So the subject gets held still: one copy of the library, one VM binary, one surface map, taken
together, with a record of what they are.

## What it holds

| file | what it is |
| --- | --- |
| `KajiLibrary/` | the library at the frozen commit, extracted with `git archive` |
| `run-headless.exe` | the VM binary (copied from `target/release`, see **Pedigree** below) |
| `surface.json` | the operation pool, read with `javap` **against the frozen library** |
| `PROVENANCE.json` | the commit, the digests, and whether it can be rebuilt from the commit |

The reference JDK is deliberately **not** frozen. It is external, versioned by its own
installer, and already stable; `--jdk` names it on every campaign and `PROVENANCE.json` records
which one was used.

`surface.json` has to be the frozen library's own map, not the working tree's. The operation
pool is what the generator walks, so a map built from a different library silently changes which
operations exist — that is why `surface.py` takes `--lib`.

## Using it

```
python test-fuzzing/fuzzer/libfuzz.py --jdk "C:/Program Files/Java/jdk-25" \
    --vm    test-fuzzing/frozen-target/run-headless.exe \
    --boot  test-fuzzing/frozen-target/KajiLibrary \
    --surface test-fuzzing/frozen-target/surface.json \
    --seed 1 --batches 8 --per-batch 300
```

All three flags are needed. A campaign that passes `--vm` and `--boot` records
`"frozen_target": true` in its metadata; without that line a result measured against the
snapshot and one measured against the working tree are indistinguishable in the output, which
is the whole problem this is meant to fix.

Use the frozen target for **anything that compares generators**: a new mutation strategy, a
scheduling change, a different corpus, a claim that one bandit beats another. Use the working
tree for **finding defects to fix** — that is what a moving subject is for.

## Building and checking it

```
python test-fuzzing/fuzzer/freeze.py <commit>     # materialize
python test-fuzzing/fuzzer/freeze.py --verify     # has it drifted?
```

`--verify` re-digests the library tree, the binary and the surface map and compares them with
`PROVENANCE.json`. Run it before trusting a comparison: the directory is ordinary files and
nothing stops an `--emit` or a `cargo build` from writing into it.

The directory is **gitignored**. It is 85 MB of derived bytes, and the project's rule for those
is to keep the recipe and the provenance rather than the artifact — the same reasoning
`bin/FROZEN.md` writes down for the frozen tools. `freeze.py` is the recipe and
`PROVENANCE.json` is the provenance.

## Pedigree, and the one thing it cannot promise

The library is extracted from the commit, so it is exactly that commit's. The VM binary is
**copied from `target/release`**, because rebuilding it needs a clean checkout and several
minutes. `freeze.py` therefore checks whether `src/`, `Cargo.toml` or `Cargo.lock` differ from
the frozen commit, and refuses when they do — a snapshot whose two halves come from different
places is worse than none.

`--allow-dirty` overrides that refusal and records what it cost. There are two different
properties here and only one of them is given up:

- **fixed** — the snapshot does not change under a campaign. This always holds, and it is what
  makes comparisons between campaigns valid.
- **reproducible** — the same snapshot can be recreated from the commit alone. This is what
  `--allow-dirty` gives up, and `PROVENANCE.json` says so in `reproducible_from_commit`, with
  `vm_uncommitted` naming the files.

To get both, build the binary from a clean checkout first, the way `bin/FROZEN.md` does it:

```
git archive <commit> | tar -x -C /tmp/kaji-frozen-src
CARGO_TARGET_DIR=/tmp/kaji-frozen-target cargo build --release \
    --manifest-path /tmp/kaji-frozen-src/Cargo.toml
cp /tmp/kaji-frozen-target/release/run-headless.exe target/release/
python test-fuzzing/fuzzer/freeze.py <commit>
```

Note that `freeze.py` watches all of `src/`, which is coarser than it has to be: a change under
`src/javac/` cannot alter what the VM does, but it does change the binary's bytes and the check
cannot tell the difference. When that is the only thing dirty, `--allow-dirty` plus the file
list in `vm_uncommitted` is enough for a reader to judge.

## Provenance of the current snapshot

Read `PROVENANCE.json`; this table is a human-readable copy and can fall behind it.

| taken | commit | classes | reproducible | note |
| --- | --- | --- | --- | --- |
| 2026-10-07 | `be9855bd` | 7068 | no | VM built with `src/javac/codegen.rs` uncommitted (another session's javac work, which cannot affect the VM) |

### What should happen next

This snapshot is the first one, and it is already a compromise: `reproducible_from_commit` is
false. A clean one, built from a clean checkout, is worth taking the next time `src/` is
committed — and `BASELINE.md` is worth re-measuring against it, because the numbers in it today
were measured against a subject that has since changed underneath them.
