# -*- coding: utf-8 -*-
"""Differential fuzzing of the WHOLE KajiLibrary against a reference JDK.

    python test-fuzzing/fuzzer/surface.py --jdk "$OPENJDK"        # once, ~2 min
    python test-fuzzing/fuzzer/libfuzz.py --jdk "$OPENJDK" --seed 1 --batches 5 --per-batch 400

Where this sits. `bigfuzz.py` and `strfuzz.py` had the method list of one class written by hand.
`surface.py` replaced that with discovery: 4297 classes and 38609 comparable public members, read
with javap from both sides. `policy.py` then subtracts what must not be invoked at random. What
is left -- about 1500 classes and 12400 operations -- is **one single pool**, not a pool per
package: carving by package would throw away most of the library to protect a few dozen classes,
and would mean the campaign never finishes a pass.

The budget problem that scale creates. 12400 operations and a few thousand cases per campaign
means a uniform draw gives each operation a fraction of a case, so most of the library is never
touched and the campaign cannot say so. Instead the pool is walked **in order, with the cursor
kept on disk**: a 2000-case campaign exercises 2000 distinct operations, and the next one resumes
where it stopped rather than starting over. A full pass takes about six campaigns, and after the
pass the cursor wraps and the second pass sees different argument values.

What a case looks like. A sequence of declarations, each able to use the previous ones, with the
target operation last so it can consume what they built:

    java.lang.String v0 = s(931, 927, 934);
    java.lang.String v1 = v0.toLowerCase();
    int v2 = v1.indexOf(v0, 3);

Strings go in as code points, `s(931, 927)`, and come out as the list of their code units,
`S[963,959]`, so neither the source encoding nor the console can invent a difference.

What gets compared. Only values whose text is fixed by the specification: primitives, `String`,
arrays of those, and the classes in `policy.VALUE_CLASSES`. Comparing the `toString()` of
anything else reports a difference that both implementations are entitled to -- for an array it is
the object identity, which is how an earlier version produced a campaign full of invented bugs.
"""
import argparse
import datetime
import hashlib
import io
import json
import os
import random
import re
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from policy import Policy, VALUE_CLASSES          # noqa: E402

REPO = HERE.parent.parent
KAJILIB = REPO / "KajiLibrary"
HEADLESS = REPO / "target" / "release" / "run-headless.exe"
WORK = HERE / "runs"
JAVAC = JAVA = None
BOOT = None
MIN_SPLIT = 4
MAX_SPLITS = 6


# =============================================================================================
# descriptors
# =============================================================================================
PRIMS = {"B": "byte", "C": "char", "D": "double", "F": "float", "I": "int", "J": "long",
         "S": "short", "Z": "boolean", "V": "void"}


def read_type(d, i):
    c = d[i]
    if c in PRIMS:
        return c, i + 1
    if c == "[":
        inner, j = read_type(d, i + 1)
        return "[" + inner, j
    if c == "L":
        j = d.index(";", i)
        return d[i:j + 1], j + 1
    raise ValueError("bad descriptor at " + d[i:])


def split_desc(d):
    args, i = [], 1
    while d[i] != ")":
        t, i = read_type(d, i)
        args.append(t)
    ret, _ = read_type(d, i + 1)
    return args, ret


def java_name(t):
    if t in PRIMS:
        return PRIMS[t]
    if t.startswith("["):
        return java_name(t[1:]) + "[]"
    return t[1:-1].replace("/", ".")


def jvm_name(cls):
    return "L" + cls.replace(".", "/") + ";"


def is_ref(t):
    return t not in PRIMS


# =============================================================================================
# seeds: the part that a signature cannot tell you
# =============================================================================================
DEFAULTS = {
    "I": [0, 1, -1, 2, 3, 5, 8, 31, 32, 33, 63, 64, 65, 100, 127, 128, 255, 256, 65535,
          65536, 2147483647, -2147483648],
    "J": [0, 1, -1, 2, 255, 256, 2147483647, 2147483648, -2147483649,
          9223372036854775807, -9223372036854775808],
    "S": [0, 1, -1, 32767, -32768],
    "B": [0, 1, -1, 127, -128],
    "C": [0, 9, 32, 65, 97, 223, 304, 305, 931, 963, 962, 8486, 55296, 56320, 65535],
    "Z": [True, False],
    "F": ["0.0f", "1.0f", "-1.0f", "0.5f", "Float.NaN", "Float.POSITIVE_INFINITY",
          "Float.MIN_VALUE", "Float.MAX_VALUE"],
    "D": ["0.0d", "-0.0d", "1.0d", "-1.0d", "0.1d", "Double.NaN", "Double.POSITIVE_INFINITY",
          "Double.NEGATIVE_INFINITY", "Double.MIN_VALUE", "Double.MAX_VALUE", "4.9E-324d"],
}

# Characters where an implementation of text goes wrong. Some occupy two code units, which is
# why each entry is a list rather than a number.
EDGE_TEXT = [
    [0], [9], [10], [32], [160], [8195], [12288], [65279],
    [97], [65], [122], [90], [48],
    [223], [7838],                      # the sharp s, and its capital
    [304], [305],                       # Turkish dotted and dotless i
    [233], [769],                       # precomposed e-acute, and the combining accent
    [931], [963], [962],                # capital sigma, sigma, final sigma
    [453],                              # a title-case letter
    [8486],                             # the ohm sign
    [64259],                            # an ffi ligature: three letters in upper case
    [55296], [56320], [65535],          # lone surrogates, and the last code unit
    [55357, 56832], [55297, 56320], [55297, 56360],
]

# Strings that MEAN something to whoever receives them. Without these every String comes out of
# the Unicode edge pool, and a parser-like API rejects all of them: the class gets discovered,
# scheduled, invoked, and never exercised past its argument check.
USEFUL_TEXT = [
    "", "0", "1", "-1", "2", "10", "255", "-256", "65536", "2147483647", "-2147483648",
    "9223372036854775807", "18446744073709551616", "123456789012345678901234567890",
    "007", "+5", " 1", "1 ", "0x10", "1e3", "NaN", "Infinity", "1.5", "-0.0",
    "true", "false", "null", "a", "A", "abc", "ABC", "hello world", "  ", "a,b,,c", "a.b.c",
    "UTF-8", "US-ASCII", "ISO-8859-1", "en", "en_US", "es-AR", "America/Argentina/Buenos_Aires",
    "yyyy-MM-dd", "2026-10-03", "2026-10-03T12:34:56", "PT1H30M", "P1Y2M3D", "#,##0.00",
    "[a-z]+", "(a|b)*", "\\\\d{2,}", "%s %d", "java.lang.String", "/", ".", "..",
]

# Letters of one script. The generator builds a "word" from a single range instead of mixing
# alphabets at random, because the case rules that depend on context -- Greek final sigma is the
# one we know about -- only fire when there are letters around.
WORD_RANGES = [[97, 122], [65, 90], [224, 255], [192, 222], [945, 969], [913, 937],
               [1072, 1103], [1040, 1071], [1488, 1514], [12354, 12435]]

# Expressions for types that cannot be written as a literal and are needed as arguments.
FACTORIES = {
    "Ljava/lang/Object;": ["s(97)", "java.lang.Integer.valueOf(1)"],
    "Ljava/lang/CharSequence;": ["__text__"],
    "Ljava/lang/Comparable;": ["s(97)"],
    "Ljava/lang/Iterable;": ["java.util.Arrays.asList(s(97), s(98))"],
    "Ljava/util/Collection;": ["java.util.Arrays.asList(s(97), s(98))"],
    "Ljava/util/List;": ["java.util.Arrays.asList(s(97), s(98))"],
    "Ljava/util/Map;": ["new java.util.HashMap()"],
    "Ljava/util/Set;": ["new java.util.HashSet()"],
}

STATEMENT_OPS = []  # the operation behind each declaration of the case being built
CACHE = {}          # arguments that already worked, by type
USED = []           # literals this case used, so they can be rewarded if it runs clean
COLLECT = False
FOCUS = None
BIG = re.compile(r"\d{10,}")


def remember(t, expr, cfg):
    # Huge values are not cached. The cache is for learning the SHAPE an API accepts -- that
    # `new BigInteger(String)` wants digits -- not for repeating the extreme value that once got
    # through. Without this guard the first batch ran in 91 seconds and the second never
    # finished: reusing valid arguments means calls stop failing instantly and start doing real
    # work, and one `repeat(2147483647)` eats the whole timeout. The extremes still enter through
    # the seeds, which is where they belong: they get tried, not multiplied.
    if len(expr) > cfg["cache_max_len"] or BIG.search(expr):
        return
    pool = CACHE.setdefault(t, [])
    if expr in pool:
        return
    pool.append(expr)
    if len(pool) > cfg["cache_per_type"]:
        pool.pop(0)


def escape(text):
    out = []
    for ch in text:
        if ch == chr(92):
            out.append(chr(92) * 2)
        elif ch == '"':
            out.append(chr(92) + '"')
        elif 32 <= ord(ch) < 127:
            out.append(ch)
        else:
            out.append(chr(92) + "u%04x" % ord(ch))
    return '"' + "".join(out) + '"'


def word_with(focus, r):
    lo, hi = r.choice(WORD_RANGES)
    n = r.randint(1, 4)
    cs = [r.randint(lo, hi) for _ in range(n)]
    x = r.random()
    if x < 0.55:
        cs = cs + list(focus)              # at the end: where the closing rules live
    elif x < 0.85:
        cs = cs[:n // 2] + list(focus) + cs[n // 2:]
    else:
        cs = list(focus) + cs
    return "s(" + ", ".join(str(c) for c in cs) + ")"


def text_literal(r, cfg):
    if FOCUS is not None and r.random() < cfg["focus_weight"]:
        return word_with(FOCUS, r)
    if r.random() < cfg["useful_weight"]:
        return escape(r.choice(USEFUL_TEXT))
    if r.random() < cfg["word_weight"]:
        lo, hi = r.choice(WORD_RANGES)
        cs = [r.randint(lo, hi) for _ in range(r.randint(1, 7))]
        return "s(" + ", ".join(str(c) for c in cs) + ")"
    n = r.randint(0, 8) if r.random() < 0.85 else r.randint(9, 12)
    cs = []
    while len(cs) < n:
        x = r.random()
        if x < 0.45:
            cs.extend(r.choice(EDGE_TEXT))
        elif x < 0.80:
            cs.append(r.randint(0x20, 0x7E))
        else:
            cs.append(r.randint(0, 0xFFFF))
    return "s(" + ", ".join(str(c) for c in cs) + ")"


def literal(t, r, cfg):
    if t == "Ljava/lang/String;":
        return text_literal(r, cfg)
    if t == "Z":
        return "true" if r.choice(DEFAULTS["Z"]) else "false"
    if t == "I":
        return str(r.choice(DEFAULTS["I"]))
    if t == "J":
        return str(r.choice(DEFAULTS["J"])) + "L"
    if t == "S":
        return "(short) " + str(r.choice(DEFAULTS["S"]))
    if t == "B":
        return "(byte) " + str(r.choice(DEFAULTS["B"]))
    if t == "C":
        return "(char) " + str(r.choice(DEFAULTS["C"]))
    if t in ("F", "D"):
        return str(r.choice(DEFAULTS[t]))
    if t.startswith("["):
        elems = [literal(t[1:], r, cfg) for _ in range(r.randint(0, 4))]
        if any(e is None for e in elems):
            return None
        return "new " + java_name(t[1:]) + "[] {" + ", ".join(elems) + "}"
    return None


ACCEPTS = {
    "Ljava/lang/Object;": is_ref,
    "Ljava/lang/CharSequence;": lambda t: t in ("Ljava/lang/String;",
                                                "Ljava/lang/StringBuilder;"),
}


def compatible(want, have):
    if want == have:
        return True
    f = ACCEPTS.get(want)
    return bool(f and f(have))


# =============================================================================================
# the pool
# =============================================================================================
class Pool(object):
    """The operations that may be invoked, indexed by what they produce."""

    def __init__(self, ops, cfg, value_types, usable_classes):
        self.cfg = cfg
        self.ops = ops
        self.value_types = value_types
        self.usable_classes = usable_classes
        self.by_return = {}
        for o in ops:
            self.by_return.setdefault(o["produces"], []).append(o)
        self.index()

    def index(self):
        # Per receiver type, the instance operations that return something comparable. Sorted so
        # the ones needing no arguments and with the shortest name come first, which is a decent
        # proxy for the simplest accessor.
        self.observers = {}
        for o in self.ops:
            if o["static"] or o["ctor"] or not self.observable(o["ret"]):
                continue
            self.observers.setdefault(jvm_name(o["cls"]), []).append(o)
        for v in self.observers.values():
            v.sort(key=lambda o: (len(o["args"]), len(o["name"])))

    def observable(self, t):
        if t == "V":
            return False
        if t in PRIMS:
            return True
        if t == "Ljava/lang/String;":
            return True
        if t.startswith("["):
            return self.observable(t[1:])
        return t in self.value_types


def argument(t, r, pool, vars_, depth):
    live = [i for i, (vt, _) in enumerate(vars_) if compatible(t, vt)]
    if live and r.random() < 0.6:
        return "v" + str(r.choice(live))
    cached = CACHE.get(t)
    if cached and r.random() < pool.cfg["cache_weight"]:
        e = r.choice(cached)
        if COLLECT:
            USED.append((t, e))
        return e
    lit = literal(t, r, pool.cfg)
    if lit is not None:
        if COLLECT:
            USED.append((t, lit))
        return lit
    fab = FACTORIES.get(t)
    if fab:
        e = r.choice(fab)
        return text_literal(r, pool.cfg) if e == "__text__" else e
    if live:
        return "v" + str(r.choice(live))
    if depth > 0:
        makers = pool.by_return.get(t, [])
        for o in r.sample(makers, min(3, len(makers))):
            e = call(o, r, pool, vars_, depth - 1)
            if e is not None:
                return e
    return None


def call(o, r, pool, vars_, depth):
    args = []
    for t in o["args"]:
        e = argument(t, r, pool, vars_, depth)
        if e is None:
            return None
        args.append(e)
    joined = ", ".join(args)
    if o["ctor"]:
        return "new " + o["cls"] + "(" + joined + ")"
    if o["static"]:
        return o["cls"] + "." + o["name"] + "(" + joined + ")"
    recv = argument(jvm_name(o["cls"]), r, pool, vars_, depth)
    if recv is None:
        return None
    return recv + "." + o["name"] + "(" + joined + ")"


NUMS = re.compile(r"-?\d+")


def skeleton(vars_):
    """The case with its numbers blanked. Two cases with the same skeleton test the same thing."""
    return tuple(NUMS.sub("#", e) for _, e in vars_)


def make_case(r, pool, length, target):
    vars_ = []
    tries = 0
    while len(vars_) < length and tries < length * 12:
        tries += 1
        remaining = length - len(vars_)
        o = target if (target is not None and remaining == 1) else r.choice(pool.ops)
        e = call(o, r, pool, vars_, 1)
        if e is None:
            if o is target and tries > length * 6:
                break
            continue
        vars_.append((o["produces"], e))
        STATEMENT_OPS.append(o["cls"] + "." + o["name"])
    if vars_ and not any(pool.observable(t) for t, _ in vars_):
        add_observer(vars_, r, pool)
    return vars_


def add_observer(vars_, r, pool):
    """Append one call whose only job is to make the case comparable.

    Most of the library does not return a value we can compare: `void`, or an object whose
    `toString()` the specification does not fix. At library scale that is the common case rather
    than the exception. In the first run, 60 of 100 generated cases had nothing observable and
    were thrown away, which wastes the operation turn as well as the case. So when nothing is
    observable, one accessor is appended: an operation that takes a receiver we already have and
    returns something the oracle can read. It is the observer method of the feedback-directed
    random testing literature, and here it is what makes the yield usable.
    """
    for i in range(len(vars_) - 1, -1, -1):
        t = vars_[i][0]
        if t == "V":
            continue
        for o in pool.observers.get(t, ()):
            e = call_on(o, "v" + str(i), r, pool, vars_)
            if e is not None:
                vars_.append((o["ret"], e))
                STATEMENT_OPS.append(o["cls"] + "." + o["name"])
                return True
    return False


def call_on(o, receiver, r, pool, vars_):
    """`o` invoked on an expression we already have, rather than on a fresh one."""
    args = []
    for t in o["args"]:
        e = argument(t, r, pool, vars_, 0)
        if e is None:
            return None
        args.append(e)
    return receiver + "." + o["name"] + "(" + ", ".join(args) + ")"


# =============================================================================================
# the program both sides run
# =============================================================================================
HELPERS = r"""
  static String s(int... c) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < c.length; i++) { b.append((char) c[i]); }
    return b.toString();
  }
  static String flat(String x) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < x.length(); i++) {
      char ch = x.charAt(i);
      b.append(ch == '\n' || ch == '\r' ? ' ' : ch);
    }
    return b.toString();
  }
  static String enc(String v) {
    if (v == null) { return "null"; }
    StringBuilder b = new StringBuilder("S[");
    for (int i = 0; i < v.length(); i++) {
      if (i > 0) { b.append(','); }
      b.append((int) v.charAt(i));
    }
    return b.append(']').toString();
  }
  static String enc(boolean v) { return String.valueOf(v); }
  static String enc(byte v) { return String.valueOf(v); }
  static String enc(short v) { return String.valueOf(v); }
  static String enc(int v) { return String.valueOf(v); }
  static String enc(long v) { return String.valueOf(v); }
  static String enc(char v) { return "c" + (int) v; }
  static String enc(float v) { return "f" + Float.toString(v) + "/" + Float.floatToRawIntBits(v); }
  static String enc(double v) { return "d" + Double.toString(v) + "/" + Double.doubleToRawLongBits(v); }
  static String enc(Object v) { return v == null ? "null" : "T[" + flat(v.toString()) + "]"; }
  static String enc(Object[] v) {
    if (v == null) { return "null"; }
    StringBuilder b = new StringBuilder("A[");
    for (int i = 0; i < v.length; i++) {
      if (i > 0) { b.append(';'); }
      b.append(enc(v[i]));
    }
    return b.append(']').toString();
  }
  static String enc(String[] v) {
    if (v == null) { return "null"; }
    StringBuilder b = new StringBuilder("A[");
    for (int i = 0; i < v.length; i++) {
      if (i > 0) { b.append(';'); }
      b.append(enc(v[i]));
    }
    return b.append(']').toString();
  }
  static String arr(String v) { return v == null ? "null" : "A" + v; }
  static String enc(boolean[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(byte[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(short[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(int[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(long[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(char[] v) { return v == null ? "null" : enc(new String(v)); }
  static String enc(float[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
  static String enc(double[] v) { return arr(v == null ? null : java.util.Arrays.toString(v)); }
"""


def render_case(vars_, pool):
    body, seen = [], []
    for i, (t, e) in enumerate(vars_):
        if t == "V":
            body.append("      " + e + ";")
        else:
            body.append("      " + java_name(t) + " v" + str(i) + " = " + e + ";")
            if pool.observable(t):
                seen.append("v" + str(i))
        body.append("      at = " + str(i + 1) + ";")
    if not seen:
        return None
    return body, ' + "|" + '.join("enc(" + v + ")" for v in seen)


def program(name, cases, pool):
    out = ["public class " + name + " {", HELPERS]
    live = []
    for k, vars_ in enumerate(cases):
        made = render_case(vars_, pool) if vars_ else None
        if made is None:
            continue
        body, ret = made
        live.append(k)
        out.append("  static String e" + str(k) + "() {")
        out.append("    int at = 0;")
        out.append("    try {")
        out.extend(body)
        out.append("      return " + ret + ";")
        out.append("    } catch (Throwable t) {")
        # Which declaration threw, not just that something did. Without this, a case that dies
        # on its first statement is indistinguishable from one that dies on its last, and the
        # divergence gets charged to whichever operation was scheduled rather than to the one
        # that actually misbehaved. That mis-attribution produced a whole report pointing at the
        # wrong classes.
        out.append('      return "EXC@" + at + ":" + t.getClass().getName();')
        out.append("    }")
        out.append("  }")
    out.append("  static int run() {")
    for k in live:
        out.append('    System.out.println("' + str(k) + '=" + e' + str(k) + "());")
    out.append("    return " + str(len(live)) + ";")
    out.append("  }")
    out.append("  public static void main(String[] a) { System.out.println(run()); }")
    out.append("}")
    return "\n".join(out) + "\n", live


# =============================================================================================
# running
# =============================================================================================
LINE = re.compile(r"^(\d+)=(.*)$")
_splits = [0]


def collect(text):
    got = {}
    for ln in text.splitlines():
        m = LINE.match(ln.rstrip("\r"))
        if m:
            got[int(m.group(1))] = m.group(2)
    return got


def run_once(name, cases, pool, limit):
    d = WORK / name
    d.mkdir(parents=True, exist_ok=True)
    src = d / (name + ".java")
    text, live = program(name, cases, pool)
    src.write_text(text, encoding="utf-8")
    c = subprocess.run([str(JAVAC), "-nowarn", "-d", str(d), str(src)], capture_output=True,
                       text=True, encoding="utf-8", errors="replace", timeout=600)
    if c.returncode != 0:
        return None, None, {"javac": c.stderr[-6000:], "live": []}
    def decode(b):
        return b.decode("utf-8", "replace") if isinstance(b, bytes) else (b or "")

    # **Both sides can hang, and the reference hanging is not a bug in our library.** The first
    # version only guarded our VM, because on two hand-picked classes the reference never
    # blocked. Across the whole library it does -- something waits on a lock, a stream or a
    # device -- and an unguarded timeout took the campaign down with a traceback. A reference
    # that produced no answer leaves nothing to compare: those cases are dropped, counted, and
    # named, not scored.
    t0 = time.time()
    ref_timeout = False
    try:
        ref = subprocess.run([str(JAVA), "-cp", str(d), name], capture_output=True, text=True,
                             encoding="utf-8", errors="replace", timeout=limit)
        ref_out = ref.stdout
    except subprocess.TimeoutExpired as e:
        ref_out, ref_timeout = decode(e.stdout), True
    t1 = time.time()
    cmd = [str(HEADLESS), str(d / (name + ".class")), "run"]
    if BOOT:
        cmd += ["--boot", str(BOOT)]
    try:
        ours = subprocess.run(cmd, cwd=str(REPO), capture_output=True, text=True,
                              encoding="utf-8", errors="replace", timeout=limit)
        out, code, err = ours.stdout, ours.returncode, ours.stderr
    except subprocess.TimeoutExpired as e:
        out, code, err = decode(e.stdout), "timeout", decode(e.stderr)
    t2 = time.time()
    # Our VM writes System.out to stderr and the reference writes it to stdout: read both.
    tail = "\n".join(l for l in err.splitlines()
                     if not l.startswith("[gc]") and not LINE.match(l))[-600:]
    return collect(ref_out), collect(out + "\n" + err), {
        "ref_s": t1 - t0, "ours_s": t2 - t1, "exit": code, "stderr": tail, "live": live,
        "ref_timeout": ref_timeout}


def run_split(name, cases, pool, limit, idx=None, floor=None):
    """Run a batch and, when our VM died partway, split and retry the halves.

    One case that kills the VM takes the rest of the batch with it, and without this the cases
    behind it are reported as divergences that are really one. The split is bounded, because
    every level recompiles the batch: an unbounded one spent 474 seconds on a batch where its
    neighbours took 55. What a bounded split loses is precision -- some neighbours of the real
    killer stay marked as "no output" -- and that is information the attribution discards anyway,
    since with no output there is no position to compare.
    """
    if floor is None:
        floor = MIN_SPLIT
    if idx is None:
        idx = list(range(len(cases)))
        _splits[0] = 0
    chosen = set(idx)
    sub = [cases[i] if i in chosen else [] for i in range(len(cases))]
    ref, ours, info = run_once(name, sub, pool, limit)
    if ref is None:
        if len(idx) <= 1:
            return {}, {}, {"ref_s": 0.0, "ours_s": 0.0, "exit": "javac", "live": [],
                            "stderr": "", "javac": info.get("javac", "")[:600]}
        half = len(idx) // 2
        ref, ours, info = {}, {}, {"ref_s": 0.0, "ours_s": 0.0, "exit": 0, "live": [],
                                   "stderr": ""}
        for suffix, part in (("a", idx[:half]), ("b", idx[half:])):
            if not part:
                continue
            r2, o2, i2 = run_split(name + suffix, cases, pool, limit, part, floor)
            ref.update(r2 or {})
            ours.update(o2 or {})
            info["live"] = info["live"] + i2.get("live", [])
            info["ref_s"] += i2.get("ref_s", 0.0)
            info["ours_s"] += i2.get("ours_s", 0.0)
        return ref, ours, info
    missing = [k for k in info["live"] if k in ref and k not in ours]
    if info.get("ref_timeout"):
        missing = missing + [k for k in info["live"] if k not in ref]
    if not missing or len(idx) <= floor or _splits[0] >= MAX_SPLITS:
        return ref, ours, info
    _splits[0] += 1
    half = len(idx) // 2
    for suffix, part in (("a", idx[:half]), ("b", idx[half:])):
        if not part:
            continue
        r2, o2, _ = run_split(name + suffix, cases, pool, limit, part, floor)
        ref.update(r2 or {})
        ours.update(o2 or {})
    return ref, ours, info


# =============================================================================================
# oracle
# =============================================================================================
ABSENT = {"java.lang.NoSuchMethodError", "java.lang.AbstractMethodError",
          "java.lang.UnsatisfiedLinkError", "java.lang.NoClassDefFoundError"}

# Throwing a SUBCLASS of what the specification promises satisfies the specification: `charAt`
# promises IndexOutOfBoundsException and an ArrayIndexOutOfBoundsException is one. Without this,
# 126 of 387 divergences in one campaign were that single pair, drowning everything else.
PARENT = {
    "java.lang.ArrayIndexOutOfBoundsException": "java.lang.IndexOutOfBoundsException",
    "java.lang.StringIndexOutOfBoundsException": "java.lang.IndexOutOfBoundsException",
    "java.lang.IndexOutOfBoundsException": "java.lang.RuntimeException",
    "java.lang.NumberFormatException": "java.lang.IllegalArgumentException",
    "java.lang.IllegalArgumentException": "java.lang.RuntimeException",
    "java.lang.NegativeArraySizeException": "java.lang.RuntimeException",
    "java.lang.ArithmeticException": "java.lang.RuntimeException",
    "java.lang.NullPointerException": "java.lang.RuntimeException",
    "java.lang.ClassCastException": "java.lang.RuntimeException",
    "java.lang.IllegalStateException": "java.lang.RuntimeException",
    "java.lang.UnsupportedOperationException": "java.lang.RuntimeException",
    "java.util.NoSuchElementException": "java.lang.RuntimeException",
    "java.util.ConcurrentModificationException": "java.lang.RuntimeException",
    "java.time.DateTimeException": "java.lang.RuntimeException",
    "java.time.format.DateTimeParseException": "java.time.DateTimeException",
    "java.lang.RuntimeException": "java.lang.Exception",
    "java.io.UnsupportedEncodingException": "java.io.IOException",
    "java.io.IOException": "java.lang.Exception",
}


def descends(child, parent):
    seen = set()
    while child and child not in seen:
        if child == parent:
            return True
        seen.add(child)
        child = PARENT.get(child)
    return False


THROWN = re.compile(r"^EXC@(\d+):(.*)$")


def thrown(v):
    """(statement index, exception class) when the case threw, else None."""
    if v is None:
        return None
    m = THROWN.match(v)
    return (int(m.group(1)), m.group(2)) if m else None


def classify(ref, ours):
    if ref == ours:
        return None
    if ours is None:
        return "no output (crash or hang)"
    a, b = thrown(ref), thrown(ours)
    if b and b[1] in ABSENT:
        return "missing from the library"
    if a and b:
        if a[0] != b[0]:
            return "threw at a different statement"
        if descends(b[1], a[1]):
            return "more specific exception"
        return "different exception"
    if bool(a) != bool(b):
        return "exception versus value"
    return "different value"


def blame(ref, ours, ops):
    """Which statement the divergence belongs to, and the operation that produced it.

    For a thrown case it is the statement that threw; for values it is the first field that
    differs. `None` when the two sides cannot be lined up -- then nothing is charged, because a
    wrong attribution is worse than no attribution.
    """
    a, b = thrown(ref), thrown(ours)
    at = None
    if a and b:
        at = min(a[0], b[0])
    elif a and not b:
        at = a[0]
    elif b and not a:
        at = b[0]
    else:
        fa, fb = str(ref).split("|"), str(ours).split("|")
        if len(fa) != len(fb):
            return None, None
        for i, (x, y) in enumerate(zip(fa, fb)):
            if x != y:
                at = i
                break
    if at is None or at >= len(ops):
        return at, None
    return at, ops[at]


# =============================================================================================
# reduction
# =============================================================================================
def size(vars_):
    return sum(1000 + len(e) for _, e in vars_)


TEXT_CALL = re.compile(r"s\(([-\d,\s]*)\)")
INTEGER = re.compile(r"(?<![\w.])(-?\d{2,})(?![\w.])")


def simpler(e):
    out = []
    for m in TEXT_CALL.finditer(e):
        cs = [c.strip() for c in m.group(1).split(",") if c.strip()]
        for fewer in (cs[:len(cs) // 2], cs[len(cs) // 2:], cs[:-1], cs[1:]):
            if len(fewer) < len(cs):
                out.append(e[:m.start()] + "s(" + ", ".join(fewer) + ")" + e[m.end():])
    for m in INTEGER.finditer(e):
        v = int(m.group(1))
        for smaller in (0, 1, -1, v // 2):
            if abs(smaller) < abs(v):
                out.append(e[:m.start()] + str(smaller) + e[m.end():])
    return out


def shrink(vars_, r, pool):
    out = []
    for i in range(len(vars_)):
        used = re.compile(r"\bv" + str(i) + r"\b")
        if not any(used.search(e) for _, e in vars_[i + 1:]):
            out.append(vars_[:i] + vars_[i + 1:])
    for i, (t, e) in enumerate(vars_):
        for _ in range(3):
            lit = literal(t, r, pool.cfg)
            if lit is not None and len(lit) < len(e):
                out.append(vars_[:i] + [(t, lit)] + vars_[i + 1:])
        for e2 in simpler(e):
            if e2 != e:
                out.append(vars_[:i] + [(t, e2)] + vars_[i + 1:])
    return out


def case_text(vars_):
    out = []
    for i, (t, e) in enumerate(vars_):
        out.append(e + ";" if t == "V" else java_name(t) + " v" + str(i) + " = " + e + ";")
    return " ".join(out)


def reduce_case(vars_, kind, pool, r, tag, limit, rounds=20):
    cur, steps = vars_, []
    for n in range(rounds):
        cands = sorted([c for c in shrink(cur, r, pool) if c and size(c) < size(cur)], key=size)
        cands = cands[:80]
        if not cands:
            break
        ref, ours, info = run_once("R" + tag + "x" + str(n), cands, pool, limit)
        if ref is None:
            break
        ok = [i for i in info["live"] if i in ref and classify(ref[i], ours.get(i)) == kind]
        if not ok:
            break
        i = min(ok, key=lambda k: size(cands[k]))
        steps.append({"java": case_text(cands[i]), "ref": ref[i], "ours": ours.get(i)})
        cur = cands[i]
    ref, ours, _ = run_once("R" + tag + "end", [cur], pool, limit)
    return cur, steps, (ref or {}).get(0), (ours or {}).get(0)


# =============================================================================================
# building the pool from the discovered surface
# =============================================================================================
def load_pool(surface_path, pol, cfg, report=print):
    doc = json.load(io.open(surface_path, encoding="utf-8"))
    ops, dropped = [], {}
    def drop(why):
        dropped[why] = dropped.get(why, 0) + 1
    classes = set()
    for cls, c in doc["classes"].items():
        why = pol.reject_class(cls)
        if why:
            drop("class:" + why)
            continue
        for o in c["ops"]:
            why = pol.reject_member(cls, o["name"])
            if why:
                drop("member:" + why)
                continue
            try:
                args, ret = split_desc(o["desc"])
            except Exception:
                drop("member:bad-descriptor")
                continue
            produces = jvm_name(cls) if o["ctor"] else ret
            if o["ctor"] and (c["abstract"] or c["kind"] == "interface"):
                drop("member:not-instantiable")
                continue
            ops.append({"cls": cls, "name": o["name"], "desc": o["desc"], "args": args,
                        "ret": ret, "static": o["static"], "ctor": o["ctor"],
                        "produces": produces})
            classes.add(cls)
    value_types = {jvm_name(c) for c in VALUE_CLASSES
                   if doc["classes"].get(c, {}).get("declares_toString")}
    pool = Pool(ops, cfg, value_types, classes)
    report("pool: %d operations over %d classes" % (len(ops), len(classes)))
    for k in sorted(dropped):
        report("  dropped %-28s %6d" % (k, dropped[k]))
    return pool, dropped, doc


def keep_referable(pool, cfg, report=print):
    """Drop the classes javac will not let a normal program name.

    Discovery reads `.class` files, so it finds plenty that no ordinary compilation unit may
    mention: `jdk.internal.*` is not exported, `jdk.incubator.*` needs `--add-modules`. Rather
    than guess which, this compiles one probe that names every class and reads javac's own
    complaints: the compiler is the authority on what it will accept.
    """
    names = sorted(pool.usable_classes)
    d = WORK / "probe"
    d.mkdir(parents=True, exist_ok=True)
    src = d / "Probe.java"
    lines = ["public class Probe {"]
    for i, n in enumerate(names):
        lines.append("  Object f" + str(i) + "() { return " + n + ".class; }")
    lines.append("}")
    src.write_text("\n".join(lines) + "\n", encoding="utf-8")
    p = subprocess.run([str(JAVAC), "-nowarn", "-d", str(d), str(src)], capture_output=True,
                       text=True, encoding="utf-8", errors="replace", timeout=900)
    bad = set()
    for m in re.finditer(r"Probe\.java:(\d+): error", p.stdout + p.stderr):
        i = int(m.group(1)) - 2          # line 1 is the class header
        if 0 <= i < len(names):
            bad.add(names[i])
    if bad:
        pool.ops = [o for o in pool.ops if o["cls"] not in bad]
        pool.usable_classes = pool.usable_classes - bad
        pool.by_return = {}
        for o in pool.ops:
            pool.by_return.setdefault(o["produces"], []).append(o)
        pool.index()
    report("referable: %d classes javac refuses to name, %d operations left"
           % (len(bad), len(pool.ops)))
    return sorted(bad)


# =============================================================================================
DEFAULT_CFG = {
    "cache_weight": 0.35, "cache_per_type": 60, "cache_max_len": 40,
    "focus_weight": 0.85, "useful_weight": 0.30, "word_weight": 0.25,
}


def digest_of(path):
    try:
        h = hashlib.sha256()
        with open(str(path), "rb") as f:
            for block in iter(lambda: f.read(1 << 20), b""):
                h.update(block)
        return h.hexdigest()
    except Exception as e:
        return "unreadable: " + e.__class__.__name__


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jdk", required=True)
    ap.add_argument("--surface", default=str(HERE / "surface.json"))
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--batches", type=int, default=5)
    ap.add_argument("--per-batch", type=int, default=400)
    ap.add_argument("--length", type=int, default=2, help="declarations per case")
    ap.add_argument("--reduce", type=int, default=0, help="how many divergences to minimise")
    ap.add_argument("--limit", type=int, default=30, help="seconds per batch per side")
    ap.add_argument("--min-split", type=int, default=4)
    ap.add_argument("--max-splits", type=int, default=6)
    ap.add_argument("--boot", default=None, help="library our VM runs against")
    ap.add_argument("--allow-unsafe", action="store_true",
                    help="include classes that touch files, processes, network or devices")
    ap.add_argument("--allow-hostile", action="store_true",
                    help="include classes that are safe but stall a campaign")
    ap.add_argument("--exclude", nargs="*", default=[], help="extra class prefixes to drop")
    # Several queues can run at once, each taking every n-th operation. Splitting by index
    # rather than by package keeps every queue mixed: one queue would otherwise get all of
    # `java.time` and the other all of `javax`, and their divergence rates would not be
    # comparable. Each queue needs its OWN cursor file -- two campaigns sharing one is how an
    # earlier run ended up walking the first few operations of the library over and over.
    ap.add_argument("--shard", type=int, default=0, help="which queue this is, from 0")
    ap.add_argument("--shards", type=int, default=1, help="how many queues in total")
    ap.add_argument("--cursor", default=None,
                    help="where the position in the operation pool is kept between campaigns")
    ap.add_argument("--restart", action="store_true", help="ignore the stored cursor")
    ap.add_argument("--pool-only", action="store_true", help="report the pool and stop")
    ap.add_argument("--out", default=None)
    a = ap.parse_args()

    global JAVAC, JAVA, BOOT, MIN_SPLIT, MAX_SPLITS
    b = Path(a.jdk) / "bin"
    JAVAC, JAVA = b / "javac.exe", b / "java.exe"
    BOOT, MIN_SPLIT, MAX_SPLITS = a.boot, a.min_split, a.max_splits

    pol = Policy(a.allow_unsafe, a.allow_hostile, a.exclude)
    pool, dropped, doc = load_pool(a.surface, pol, DEFAULT_CFG)
    refused = keep_referable(pool, DEFAULT_CFG)
    if not pool.ops:
        print("nothing left to invoke", file=sys.stderr)
        sys.exit(1)
    if a.pool_only:
        print(json.dumps({"operations": len(pool.ops), "classes": len(pool.usable_classes),
                          "dropped": dropped, "refused_by_javac": refused[:40]},
                         ensure_ascii=False, indent=1))
        return

    ordered = sorted(pool.ops, key=lambda o: (o["cls"], o["name"], o["desc"]))
    if a.shards > 1:
        ordered = [o for i, o in enumerate(ordered) if i % a.shards == a.shard]
        print("shard %d of %d: %d operations" % (a.shard, a.shards, len(ordered)))
    if a.cursor is None:
        a.cursor = str(HERE / ("cursor-%d-%d.json" % (a.shard, a.shards)))
    start = 0
    if os.path.exists(a.cursor) and not a.restart:
        try:
            c = json.load(io.open(a.cursor, encoding="utf-8"))
            if c.get("total") == len(ordered):
                start = c.get("at", 0)
                print("cursor: resuming at operation %d of %d" % (start, len(ordered)))
        except Exception:
            pass

    out = Path(a.out) if a.out else (HERE.parent / "datos" /
                                     ("lib-%d.json" % a.seed))
    out.parent.mkdir(parents=True, exist_ok=True)
    # Which binary was actually measured. `target/release/run-headless.exe` is rebuilt by any
    # `cargo build --release`, so a campaign that spans someone else is compiling javac silently
    # changes what it is testing. The digest goes in the metadata, and it is re-checked between
    # batches: a result whose subject moved underneath it is worth nothing, and the only thing
    # worse than losing it is not knowing.
    vm_digest = digest_of(HEADLESS)
    meta = {"when": datetime.datetime.now().isoformat(timespec="seconds"),
            "vm_binary": str(HEADLESS), "vm_sha256": vm_digest,
            "shard": a.shard, "shards": a.shards,
            "tool": "libfuzz.py (whole library, surface discovered with javap)",
            "reference": a.jdk, "under_test": str(a.boot or "KajiLibrary"),
            "seed": a.seed, "batches": a.batches, "per_batch": a.per_batch,
            "length": a.length, "limit": a.limit,
            "pool_operations": len(ordered), "pool_classes": len(pool.usable_classes),
            "cursor_start": start}
    stats = {"cases": 0, "agree": 0, "diverge": 0, "repeats": 0, "rewarded": 0,
             "unreachable": 0, "no_reference": 0, "unattributed": 0,
             "by_kind": {}, "by_operation": {}, "ref_s": 0.0, "ours_s": 0.0}
    batches, divs, reduced = [], [], []
    seen = set()
    t0 = time.time()

    def save(state):
        stats["total_s"] = round(time.time() - t0, 1)
        io.open(str(out), "w", encoding="utf-8", newline="").write(json.dumps({
            "state": state, "meta": meta,
            "stats": {k: (round(v, 1) if isinstance(v, float) else v)
                      for k, v in stats.items()},
            "batches": batches, "unreachable_sample": unreachable,
            "divergences": [{k: v for k, v in d.items() if k != "vars"} for d in divs],
            "reduced": reduced}, ensure_ascii=False, indent=2) + "\n")

    def bump(table, key):
        table[key] = table.get(key, 0) + 1

    global FOCUS, COLLECT
    r = random.Random(a.seed)
    vm_digest = meta["vm_sha256"]
    at = start
    unreachable = []
    for bi in range(a.batches):
        batch_start = at
        cases, lits, targets, case_ops = [], {}, {}, {}
        COLLECT = True
        # The cursor advances one operation at a time, and each gets a few attempts before being
        # written off. The first version advanced it per ATTEMPT, so an operation whose case
        # could not be built was consumed without ever being tested: 100 operations spent to
        # test 40. Those are now counted and named, because an operation the fuzzer cannot
        # reach is a hole in coverage, not a detail.
        while len(cases) < a.per_batch and at - batch_start < a.per_batch * 3:
            o = ordered[at % len(ordered)]
            at += 1
            built = None
            for attempt in range(3):
                FOCUS = tuple(EDGE_TEXT[(len(cases) + attempt) % len(EDGE_TEXT)])
                del USED[:]
                del STATEMENT_OPS[:]
                case = make_case(r, pool, a.length, o)
                if case and any(pool.observable(t) for t, _ in case):
                    built, built_ops = case, list(STATEMENT_OPS)
                    break
            if built is None:
                stats["unreachable"] += 1
                if len(unreachable) < 200:
                    unreachable.append(o["cls"] + "." + o["name"] + o["desc"])
                continue
            sk = skeleton(built)
            if sk in seen:
                stats["repeats"] += 1
                continue
            seen.add(sk)
            lits[len(cases)] = list(USED)
            targets[len(cases)] = o
            case_ops[len(cases)] = list(built_ops)
            cases.append(built)
        COLLECT = False
        FOCUS = None

        ref, ours, info = run_split("L%dx%d" % (a.seed, bi), cases, pool, a.limit)
        if ref is None:
            batches.append({"batch": bi, "javac": info.get("javac", "")[:2000]})
            print("batch %d: javac refused it" % bi, flush=True)
            continue
        stats["ref_s"] += info["ref_s"]
        stats["ours_s"] += info["ours_s"]
        batches.append({"batch": bi, "exit": info["exit"], "stderr": info["stderr"],
                        "ref_s": round(info["ref_s"], 2), "ours_s": round(info["ours_s"], 2)})
        for k in info["live"]:
            if k not in ref:
                stats["no_reference"] += 1
                continue
            text = case_text(cases[k])
            o = targets[k]
            stats["cases"] += 1
            if not str(ref[k]).startswith("EXC:"):
                for t, e in lits.get(k, []):
                    remember(t, e, DEFAULT_CFG)
                stats["rewarded"] += len(lits.get(k, []))
            kind = classify(ref[k], ours.get(k))
            if kind is None:
                stats["agree"] += 1
                continue
            stats["diverge"] += 1
            bump(stats["by_kind"], kind)
            # `where`, not `at`: the first version unpacked this into `at`, which is the
            # cursor over the operation pool. One divergence set the cursor to a statement
            # index -- 0 or 1 -- so the campaign walked the first few operations of the library
            # over and over and reported a cursor that never moved.
            where, culprit = blame(ref[k], ours.get(k), case_ops.get(k, []))
            if culprit:
                bump(stats["by_operation"], culprit)
            else:
                stats["unattributed"] += 1
            divs.append({"batch": bi, "k": k, "kind": kind,
                         "scheduled": o["cls"] + "." + o["name"],
                         "blamed": culprit, "statement": where,
                         "vars": cases[k], "java": text, "ref": ref[k], "ours": ours.get(k)})
        print("batch %d: %d cases, %d divergences, cursor at %d/%d"
              % (bi, len(info["live"]), stats["diverge"], at % len(ordered), len(ordered)),
              flush=True)
        now = digest_of(HEADLESS)
        if now != vm_digest:
            stats["vm_changed"] = stats.get("vm_changed", 0) + 1
            print("WARNING: the VM binary changed mid-campaign (%s -> %s). Everything after "
                  "this batch measures a different build." % (vm_digest[:12], now[:12]),
                  flush=True)
            meta.setdefault("vm_sha256_later", []).append(now)
            vm_digest = now
        save("running")
        io.open(a.cursor, "w", encoding="utf-8", newline="").write(
            json.dumps({"at": at % len(ordered), "total": len(ordered)}) + "\n")

    picked = set()
    for i, d in enumerate(divs):
        key = (d["kind"], d["blamed"])
        if key in picked or len(reduced) >= a.reduce:
            continue
        picked.add(key)
        print("reducing [%s] %s" % (d["kind"], d["java"][:90]), flush=True)
        t = time.time()
        mn, steps, rv, ov = reduce_case(d["vars"], d["kind"], pool, r,
                                        "%dx%d" % (a.seed, len(reduced)), a.limit)
        reduced.append({"divergence": i, "kind": d["kind"], "blamed": d["blamed"],
                        "original": d["java"], "minimal": case_text(mn),
                        "minimal_ref": rv, "minimal_ours": ov, "steps": steps,
                        "seconds": round(time.time() - t, 1)})
        save("reducing")

    save("done")
    print("")
    print("cases %d · agree %d · diverge %d · repeats %d · unreachable %d"
          % (stats["cases"], stats["agree"], stats["diverge"],
             stats["repeats"], stats["unreachable"]))
    for k, v in sorted(stats["by_kind"].items(), key=lambda kv: -kv[1]):
        print("  %-34s %5d" % (k, v))
    print("")
    print("operations blamed most often (charged to the statement that diverged):")
    for k, v in sorted(stats["by_operation"].items(), key=lambda kv: -kv[1])[:15]:
        print("  %-52s %4d" % (k, v))
    print("unattributed: %d" % stats["unattributed"])
    print("written", out)


if __name__ == "__main__":
    main()
