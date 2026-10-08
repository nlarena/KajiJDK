# -*- coding: utf-8 -*-
r"""Check the regex corpus in `libfuzz.py` against the reference JDK.

The corpus is an instrument, and nothing was checking it. When this was first run it found
eleven wrong entries out of sixty, including four subjects that were escape TEXT rather than the
characters they meant -- `(?m)^b$` had never seen a line break, and the Greek and Latin-1 classes
had never seen a non-ASCII character -- and one defect in `escape()` itself, which emitted a line
terminator as a unicode escape where Java expands those before it parses the literal, so the
batch would not have compiled at all.

Two levels, on purpose:

* **strict** -- whether a pattern compiles. That is exact, and `REGEX_UNCOMPILABLE` declares it.
  A disagreement here is a wrong seed and the exit status says so.
* **loose** -- which of the two subjects the engine accepts. Reported, never failed: the corpus
  feeds operations with different ideas of matching (`matches` anchors, `find` does not, `split`
  and `replaceAll` neither), so a subject that `find` accepts and `matches` rejects is useful
  rather than wrong. Three seeds are inherently loose and will always be listed: `(|a)*` and
  `[\s\S]*` accept everything including the empty string, so no subject rejects them, and `\1`
  accepts nothing at all, because a backreference to a group that never participated always
  fails.

The probe is emitted through libfuzz's own `escape`, so the escaping a campaign relies on is on
trial here too -- which is how the `escape()` defect surfaced.

    python test-fuzzing/fuzzer/audit_corpus.py --jdk "C:/Program Files/Java/jdk-25"
"""
import argparse
import io
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import libfuzz as L                                             # noqa: E402

NL = chr(10)

PROBE = [
    "public class Audit {",
    "  static int bad = 0;",
    "  static int loose = 0;",
    "",
    "  static void t(int i, String p, String yes, String no, boolean mustFail) {",
    "    java.util.regex.Pattern re;",
    "    try {",
    "      re = java.util.regex.Pattern.compile(p);",
    "    } catch (Throwable e) {",
    "      if (!mustFail) {",
    "        bad++;",
    "        System.out.println(\"WRONG #\" + i + \"  declared compilable, does not compile: \"",
    "            + p);",
    "      }",
    "      return;",
    "    }",
    "    if (mustFail) {",
    "      bad++;",
    "      System.out.println(\"WRONG #\" + i + \"  declared uncompilable, compiles: \" + p);",
    "      return;",
    "    }",
    "    boolean my = re.matcher(yes).matches();",
    "    boolean fy = re.matcher(yes).find();",
    "    boolean mn = re.matcher(no).matches();",
    "    boolean fn = re.matcher(no).find();",
    "    if (!(my || fy) || (mn && fn)) {",
    "      loose++;",
    "      System.out.println(\"loose #\" + i + \"  \" + p",
    "          + \"   accepted: matches=\" + my + \" find=\" + fy",
    "          + \"   rejected: matches=\" + mn + \" find=\" + fn);",
    "    }",
    "  }",
    "",
    "  public static void main(String[] a) {",
]

TAIL = [
    "    System.out.println(\"\");",
    "    System.out.println(\"wrong: \" + bad + \"   loose: \" + loose);",
    "    if (bad > 0) { System.exit(1); }",
    "  }",
    "}",
    "",
]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jdk", default=r"C:/Program Files/Java/jdk-25")
    ap.add_argument("--keep", default=None,
                    help="directory to leave the generated probe in, for reading it")
    a = ap.parse_args()

    jdk = Path(a.jdk)
    for exe in ("javac.exe", "java.exe"):
        if not (jdk / "bin" / exe).exists():
            raise SystemExit("no %s under %s" % (exe, jdk / "bin"))

    calls = []
    for i, (pat, yes, no) in enumerate(L.REGEX_SEEDS):
        calls.append("    t(%d, %s, %s, %s, %s);"
                     % (i, L.escape(pat), L.escape(yes), L.escape(no),
                        "true" if pat in L.REGEX_UNCOMPILABLE else "false"))

    unknown = sorted(u for u in L.REGEX_UNCOMPILABLE
                     if not any(t[0] == u for t in L.REGEX_SEEDS))
    if unknown:
        raise SystemExit("REGEX_UNCOMPILABLE names patterns that are not seeds: %s" % unknown)

    print("%d seeds, %d of them declared uncompilable"
          % (len(L.REGEX_SEEDS), len(L.REGEX_UNCOMPILABLE)))

    tmp = Path(a.keep) if a.keep else Path(tempfile.mkdtemp(prefix="audit-corpus-"))
    tmp.mkdir(parents=True, exist_ok=True)
    src = tmp / "Audit.java"
    io.open(str(src), "w", encoding="utf-8", newline="").write(
        NL.join(PROBE + calls + TAIL))

    c = subprocess.run([str(jdk / "bin" / "javac.exe"), "-encoding", "UTF-8",
                        "-d", str(tmp), str(src)],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if c.returncode != 0:
        # A probe that will not compile is itself a finding: it means `escape` produced
        # something that is not a Java string literal.
        print(c.stdout + c.stderr)
        raise SystemExit("the probe does not compile: escape() produced an invalid literal")

    r = subprocess.run([str(jdk / "bin" / "java.exe"), "-cp", str(tmp), "Audit"],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    print(r.stdout + r.stderr, end="")
    if a.keep:
        print("probe left in %s" % tmp)
    raise SystemExit(r.returncode)


if __name__ == "__main__":
    main()
