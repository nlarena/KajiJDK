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
import math
import os
import random
import re
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from policy import Policy, VALUE_CLASSES, declared_gap   # noqa: E402

REPO = HERE.parent.parent
KAJILIB = REPO / "KajiLibrary"
HEADLESS = REPO / "target" / "release" / "run-headless.exe"
WORK = HERE / "runs"
NEWLINE = chr(10)
JAVAC = JAVA = None
BOOT = None
SELF_CHECK = True
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
MUTATIONS = [0]     # how many arguments were perturbed rather than drawn fresh
SAM = {}            # functional interfaces: JVM type -> its single abstract method
CONSTANTS = {}      # public static final values, by the JVM type they have
SUPERS = {}         # every type a given type can stand in for, transitively
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
    if REGEX_HINT[0] and r.random() < cfg["regex_weight"]:
        return escape(r.choice(REGEX_TEXT))
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



# =============================================================================================
# value mutation
# =============================================================================================
# Drawing a fresh value from the seed pool explores the extremes but never the NEIGHBOURHOOD of
# a valid one, and that is where a range check lives: the interesting minute is not 2147483647,
# it is 60. So a value already in hand gets perturbed -- one step, one bit, one character --
# which is the type-aware value mutation of Hopper, with the string cases adapted to how this
# harness writes text. Anything whose shape is not recognised comes back untouched: a mutation
# that does not compile costs the whole batch.
INT_TAIL = re.compile(r"^(.*?)(-?\d+)(L?)$")
CODES = re.compile(r"^s\(([-\d,\s]*)\)$")
QUOTED = re.compile(r'^"(.*)"$')
ARRAY = re.compile(r"^new (.+)\[\] \{(.*)\}$")
DOTTED = re.compile(r"^([\w.$]+)\.([A-Z][A-Z0-9_]*)$")

SPECIAL_D = ["Double.NaN", "Double.POSITIVE_INFINITY", "Double.NEGATIVE_INFINITY",
             "Double.MIN_VALUE", "Double.MAX_VALUE", "0.0d", "-0.0d"]
SPECIAL_F = ["Float.NaN", "Float.POSITIVE_INFINITY", "Float.NEGATIVE_INFINITY",
             "Float.MIN_VALUE", "Float.MAX_VALUE", "0.0f", "-0.0f"]


def nudge_int(v, r):
    x = r.random()
    if x < 0.34:
        return v + r.choice([1, -1, 2, -2])
    if x < 0.5:
        return v ^ (1 << r.randint(0, 31))          # one bit
    if x < 0.62:
        return -v
    if x < 0.74:
        return v // 2
    if x < 0.86:
        return v * 2
    return r.choice([0, 1, -1, 2147483647, -2147483648])


def mutate(t, expr, r, pool):
    """A small perturbation of `expr`, which has type `t`. Returns `expr` when unsure."""
    if t == "Z":
        return "false" if expr == "true" else "true"
    if t in ("I", "J", "S", "B", "C"):
        m = INT_TAIL.match(expr)
        if not m:
            return expr
        head, num, suffix = m.group(1), int(m.group(2)), m.group(3)
        v = nudge_int(num, r)
        if t == "C":
            v = v & 0xFFFF                          # a char cannot leave its range
        return head + str(v) + suffix
    if t in ("D", "F"):
        pool_special = SPECIAL_D if t == "D" else SPECIAL_F
        if not re.match(r"^-?[\d.]+(E-?\d+)?[dDfF]?$", expr):
            return r.choice(pool_special)
        try:
            v = float(expr.rstrip("dDfF"))
        except ValueError:
            return r.choice(pool_special)
        x = r.random()
        if x < 0.3:
            v = -v
        elif x < 0.5:
            v = v * 2.0
        elif x < 0.7:
            v = v / 2.0
        else:
            v = v + r.choice([1.0, -1.0, 0.5, -0.5])
        return repr(v) + ("d" if t == "D" else "f")
    if t == "Ljava/lang/String;":
        m = CODES.match(expr)
        if m:
            cs = [int(c) for c in m.group(1).split(",") if c.strip()]
            if not cs:
                return "s(" + str(r.choice([0, 32, 65, 931])) + ")"
            i = r.randrange(len(cs))
            x = r.random()
            if x < 0.3:
                cs[i] = max(0, min(0xFFFF, cs[i] + r.choice([1, -1])))
            elif x < 0.5:
                cs.pop(i)
            elif x < 0.7:
                cs.insert(i, r.choice([c for cc in EDGE_TEXT for c in cc]))
            elif x < 0.85 and len(cs) > 1:
                j = r.randrange(len(cs))
                cs[i], cs[j] = cs[j], cs[i]
            else:
                cs = cs + cs
            return "s(" + ", ".join(str(c) for c in cs) + ")"
        m = QUOTED.match(expr)
        if m:
            # A text seed that MEANS something -- a date, a pattern, a charset name. Moving one
            # digit is how a valid input becomes a boundary input: "2026-10-03" into
            # "2026-13-03" tests the month check, which no amount of random Unicode reaches.
            body = list(m.group(1))
            if not body:
                return '"0"'
            i = r.randrange(len(body))
            if body[i].isdigit() and r.random() < 0.7:
                body[i] = str((int(body[i]) + r.choice([1, -1, 3])) % 10)
            elif r.random() < 0.5:
                body.pop(i)
            else:
                body.insert(i, r.choice("0123456789-:./ TZ"))
            out = "".join(body).replace(chr(92), "").replace('"', "")
            return '"' + out + '"'
        return expr
    if t.startswith("["):
        m = ARRAY.match(expr)
        if not m:
            return expr
        inner, body = m.group(1), m.group(2)
        parts = [x.strip() for x in body.split(",")] if body.strip() else []
        x = r.random()
        if parts and x < 0.4:
            i = r.randrange(len(parts))
            parts[i] = mutate(t[1:], parts[i], r, pool)
        elif parts and x < 0.6:
            parts.pop(r.randrange(len(parts)))
        else:
            fresh = literal(t[1:], r, pool.cfg)
            if fresh is not None:
                parts.append(fresh)
        return "new " + inner + "[] {" + ", ".join(parts) + "}"
    m = DOTTED.match(expr)
    if m:
        # A named constant cannot be nudged, but its SIBLINGS are the natural neighbours: the
        # next enum value is exactly one step away in the space that matters.
        siblings = [c for c in pool.constants_for(t) if c != expr]
        if siblings:
            return r.choice(siblings)
    return expr


# Patterns and the subjects that go with them. A regex API fed random Unicode gets rejected at
# the parser and the engine is never reached -- the same failure the numeric strings fixed for
# `BigInteger`, one level deeper. ReTest seeds from half a million patterns mined from real
# projects; this is the same idea at the scale a hand-written list allows, and the subjects are
# paired so that both the accepting and the rejecting path through the engine get walked.
# Patterns that reach the engine's corners are included on purpose: nested quantifiers,
# backreferences, lookarounds, possessive and reluctant forms, boundaries, named groups.
REGEX_SEEDS = [
    ('[a-z]+', 'abc', '123'),
    ('[A-Za-z0-9_]{3,8}', 'ab_12', '!!'),
    ('[A-Z][a-z]+', 'Hola', 'hola'),
    ('\\d+', '12345', 'abc'),
    ('\\d{4}-\\d{2}-\\d{2}', '2026-10-06', '2026-10'),
    ('(\\d{1,3}\\.){3}\\d{1,3}', '192.168.0.1', '192.168'),
    ('[\\w.-]+@[\\w.-]+\\.[a-z]{2,}', 'a.b@c.com', 'a@b'),
    ('https?://[\\w./-]+', 'http://x.y/z', 'ftp://x'),
    ('-?\\d+(\\.\\d+)?', '-3.14', '.'),
    ('[+-]?\\d+[eE][+-]?\\d+', '1e-5', '1e'),
    ('#[0-9a-fA-F]{6}', '#A1b2C3', '#GGGGGG'),
    ('^\\s*$', '   ', ' x '),
    ('^\\s+|\\s+$', '  x', 'x'),
    ('[^,]+', 'abc', ','),
    ('(\\w+)\\s*=\\s*(\\w+)', 'k = v', '= ='),
    ('<([a-z]+)[^>]*>.*?</\\1>', '<b>x</b>', '<b>x</i>'),
    ('(a|b)*c', 'ababc', 'abd'),
    ('a*a*a*b', 'aaab', 'aaac'),
    ('(a+)+b', 'aaab', 'aaac'),
    ('a++b', 'aaab', 'aac'),
    ('a+?b', 'aab', 'aa'),
    ('a{2,}', 'aaa', 'a'),
    ('a{0}', '', 'a'),
    ('(?:ab)+', 'abab', 'aba'),
    ('(?<year>\\d{4})-(\\d{2})', '2026-10', '26-10'),
    ('(a)\\1', 'aa', 'ab'),
    ('(?=.*x).*', 'axb', 'ab'),
    ('(?!\\d)\\w+', 'abc', '1abc'),
    ('(?<=a)b', 'ab', 'cb'),
    ('(?<!a)b', 'cb', 'ab'),
    ('(?i)hola', 'HOLA', 'chau'),
    ('(?s).+', 'a\\nb', ''),
    ('(?m)^b$', 'a\\nb', 'ab'),
    ('(?x) a b', 'ab', 'a b'),
    ('\\b\\w+\\b', 'una', ' '),
    ('\\B\\w', 'bc', ' c'),
    ('^\\p{Lu}\\p{Ll}+$', 'Hola', 'HOLA'),
    ('\\p{InGreek}+', '\\u03b1\\u03b2', 'ab'),
    ('[\\p{L}&&[^\\p{Lu}]]+', 'abc', 'ABC'),
    ('[a-c&&[b-d]]', 'b', 'a'),
    ('[^\\x00-\\x7F]+', '\\u00e9', 'abc'),
    ('\\Qa+b\\E', 'a+b', 'aab'),
    ('\\.\\*', '.x', 'x'),
    ('()\\1*', '', 'a'),
    ('(|a)*', 'aaa', ''),
    ('[\\s\\S]*', 'a\\nb', ''),
    ('x{2,3}?', 'xx', 'x'),
    ('\\G[a-z]', 'ab', '1a'),
    ('[', '', 'a'),
    ('[]', '', 'a'),
    ('(', '', 'a'),
    (')', '', 'a'),
    ('*', '', 'a'),
    ('a{2,1}', '', 'a'),
    ('(?<n>a)(?<n>b)', '', 'ab'),
    ('\\1', '', 'a'),
    ('(?<', '', 'a'),
    ('[z-a]', '', 'a'),
    ('\\p{NoSuchThing}', '', 'a'),
    ('(?#comment)a', 'a', 'b'),
]
REGEX_TEXT = [x for triple in REGEX_SEEDS for x in triple]

# Operations whose String arguments are patterns, or whose receiver is the subject.
REGEX_OPS = {
    ("java.util.regex.Pattern", "compile"), ("java.util.regex.Pattern", "matches"),
    ("java.util.regex.Pattern", "quote"), ("java.util.regex.Pattern", "split"),
    ("java.util.regex.Pattern", "splitAsStream"), ("java.util.regex.Pattern", "asPredicate"),
    ("java.lang.String", "matches"), ("java.lang.String", "split"),
    ("java.lang.String", "replaceAll"), ("java.lang.String", "replaceFirst"),
    ("java.util.regex.Matcher", "appendReplacement"),
    ("java.util.regex.Matcher", "replaceAll"), ("java.util.regex.Matcher", "replaceFirst"),
    ("java.util.Scanner", "useDelimiter"), ("java.util.Scanner", "hasNext"),
}
REGEX_HINT = [False]

REGEX_PAIR = [None]


def regex_string(r):
    """The pattern on the first String of a hinted call, one of ITS subjects afterwards.

    Drawing both independently from one flat list -- what the first version did -- gives
    `"abc".matches("[0-9]{4}")`: the engine rejects at the first character and the matching path
    is never walked, which is the whole failure the corpus was meant to fix. The seeds are
    triples for exactly that reason, and `call_inner` builds the arguments before the receiver,
    so the pattern is asked for first in both shapes that matter: `Pattern.matches(regex, input)`
    and `subject.matches(regex)`. Both subjects stay in play: the one that matches and the one
    that does not walk different paths through the engine, and only one of them is a path the
    pattern's author ever tested.
    """
    if REGEX_PAIR[0] is None:
        pat, yes, no = REGEX_SEEDS[r.randrange(len(REGEX_SEEDS))]
        REGEX_PAIR[0] = (yes, no)
        return pat
    yes, no = REGEX_PAIR[0]
    return yes if r.random() < 0.5 else no



ACCEPTS = {
    "Ljava/lang/Object;": is_ref,
    "Ljava/lang/CharSequence;": lambda t: t in ("Ljava/lang/String;",
                                                "Ljava/lang/StringBuilder;"),
}


def compatible(want, have):
    """Whether a value of `have` may be passed where `want` is expected.

    The first version compared types for equality, with a two-entry table for `Object` and
    `CharSequence`. That is why constants were nearly worthless: `ChronoUnit.DAYS` has type
    `ChronoUnit` and almost nothing asks for a `ChronoUnit` -- the parameters ask for
    `TemporalUnit`. With the hierarchy read from javap, a subtype satisfies its supertypes and
    every producer in the pool becomes worth more, not just the constants.
    """
    if want == have:
        return True
    if want in SUPERS.get(have, ()):
        return True
    f = ACCEPTS.get(want)
    return bool(f and f(have))


def producers_for(pool, t):
    """Operations whose result can be passed where `t` is expected, subtypes included.

    The subtypes are walked in sorted order, and that is not cosmetic. They live in a `set`,
    and Python randomises string hashing per process, so iterating one gives a different order
    in every run: the same seed produced different cases, and two runs of the identical command
    gave 18 and 8 divergences. Every A/B that assumed the same seed meant the same cases was
    comparing unrelated sequences.
    """
    out = list(pool.by_return.get(t, ()))
    for sub in sorted(pool.subtypes.get(t, ())):
        out.extend(pool.by_return.get(sub, ()))
    return out


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
        self.subtypes = {}
        for sub in sorted(SUPERS):
            for sup in sorted(SUPERS[sub]):
                self.subtypes.setdefault(sup, set()).add(sub)
        self.index()
        self.index_mutators()

    def index_mutators(self):
        """Per receiver type, the instance operations worth calling BEFORE the target.

        Two shapes, because two kinds of class need different treatment. A `void` method is a
        real mutation: call it and the receiver is left changed, which is what finds the bug
        that only shows up on the third call to a `StringBuilder`. A method that returns the
        receiver's own type is useless as a bare statement on an immutable class -- discarding
        the result of `BigInteger.add` tests nothing -- so it is chained instead, and the case
        walks the value through several steps.
        """
        self.changers = {}
        self.chainers = {}
        for o in self.ops:
            if o["static"] or o["ctor"]:
                continue
            recv = jvm_name(o["cls"])
            if o["ret"] == "V":
                self.changers.setdefault(recv, []).append(o)
            elif o["ret"] == recv:
                self.chainers.setdefault(recv, []).append(o)

    def constants_for(self, t):
        """Named constants usable where `t` is expected, the subtypes of `t` included."""
        out = list(CONSTANTS.get(t, ()))
        for sub in sorted(self.subtypes.get(t, ())):
            out.extend(CONSTANTS.get(sub, ()))
        return out

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


def perturb(t, e, r, pool):
    """Maybe move the value one step. Counted, so the weight can be judged against results."""
    if e is None or r.random() >= pool.cfg["mutate_weight"]:
        return e
    out = mutate(t, e, r, pool)
    if out != e:
        MUTATIONS[0] += 1
    return out


def argument(t, r, pool, vars_, depth):
    # The regex corpus is consulted FIRST, before live variables, the cache and the named
    # constants. It was last, inside `text_literal`, and never ran: the pool holds hundreds of
    # String constants, so `Pattern.compile` kept being handed things like
    # `Attributes.NAME_CONSTANT_VALUE` and the corpus sat unused while the generated programs
    # were full of regex calls.
    if REGEX_HINT[0] and t == "Ljava/lang/String;" and r.random() < pool.cfg["regex_weight"]:
        return escape(regex_string(r))
    live = [i for i, (vt, _) in enumerate(vars_) if compatible(t, vt)]
    if live and r.random() < 0.6:
        return "v" + str(r.choice(live))
    cached = CACHE.get(t)
    if cached and r.random() < pool.cfg["cache_weight"]:
        e = perturb(t, r.choice(cached), r, pool)
        if COLLECT:
            USED.append((t, e))
        return e
    named = pool.constants_for(t)
    if named and r.random() < pool.cfg["constant_weight"]:
        return perturb(t, r.choice(named), r, pool)
    lit = perturb(t, literal(t, r, pool.cfg), r, pool)
    if lit is not None:
        if COLLECT:
            USED.append((t, lit))
        return lit
    lam = lambda_for(t, r, pool) if (t in SAM or any(sub in SAM for sub in
                                                    sorted(pool.subtypes.get(t, ())))) else None
    if lam is not None and r.random() < pool.cfg["lambda_weight"]:
        return lam
    fab = FACTORIES.get(t)
    if fab:
        e = r.choice(fab)
        return text_literal(r, pool.cfg) if e == "__text__" else e
    if named:
        return r.choice(named)
    if lam is not None:
        return lam
    if live:
        return "v" + str(r.choice(live))
    if depth > 0:
        # The recipe first: these are the producers the graph says are cheapest for `t`, so the
        # construction is known to terminate instead of being hoped for. Random producers stay
        # as a fallback, because variety matters once the value can be built at all.
        makers = list(RECIPE.get(t, ()))
        rest = [o for o in producers_for(pool, t) if o not in makers]
        r.shuffle(makers)
        for o in makers[:3] + rest[:2]:
            e = call(o, r, pool, vars_, depth - 1)
            if e is not None:
                return e
    return None


def call(o, r, pool, vars_, depth):
    # While this operation's arguments are being built, String literals come from the regex
    # corpus. The receiver is drawn under the same hint on purpose: for `"abc".matches(re)` the
    # subject is the receiver and the pattern is the argument, so both roles need to come from
    # the paired corpus rather than from Unicode noise.
    was, held = REGEX_HINT[0], REGEX_PAIR[0]
    REGEX_HINT[0] = was or (o["cls"], o["name"]) in REGEX_OPS
    if REGEX_HINT[0] and not was:
        REGEX_PAIR[0] = None
    try:
        return call_inner(o, r, pool, vars_, depth)
    finally:
        REGEX_HINT[0], REGEX_PAIR[0] = was, held


def call_inner(o, r, pool, vars_, depth):
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
    recv_t = jvm_name(o["cls"])
    recv = argument(recv_t, r, pool, vars_, depth)
    if recv is None:
        return None
    return as_receiver(recv, recv_t) + "." + o["name"] + "(" + joined + ")"


def as_receiver(expr, t):
    """An expression safe to put to the left of a dot.

    A lambda is not: `() -> null.association()` parses as `() -> (null.association())` and javac
    rejects it, which costs the whole batch. It needs a cast to its target type, in parentheses.
    Spotted in a generated case once the receiver-centred sequences started reusing producers
    whose own receiver was a functional interface.
    """
    if "->" in expr and expr.lstrip().startswith("("):
        return "((" + java_name(t) + ") " + expr + ")"
    return expr


NUMS = re.compile(r"-?\d+")


def skeleton(vars_):
    """The case with its numbers blanked. Two cases with the same skeleton test the same thing."""
    return tuple(NUMS.sub("#", e) for _, e in vars_)


def make_case(r, pool, length, target):
    """A sequence of declarations ending in `target`.

    When the target is an instance method, the receiver is materialised as its own declaration
    first and then **worked on** before the target runs: a couple of state-changing or chaining
    calls on the same object. Building the receiver inline, as the first version did, meant the
    target always saw a freshly constructed value and the whole class of bugs that needs a
    second or third call -- a `StringBuilder` that has been appended to, a buffer whose position
    moved, a collection that has had something removed -- was unreachable by construction, no
    matter how long the campaign ran.
    """
    vars_ = []
    del STATEMENT_OPS[:]
    recv_at = None
    if target is not None and not target["static"] and not target["ctor"]:
        recv_t = jvm_name(target["cls"])
        e = argument(recv_t, r, pool, vars_, 1)
        if e is not None and e.startswith("v") is False:
            vars_.append((recv_t, e))
            STATEMENT_OPS.append(target["cls"] + ".<receiver>")
            recv_at = 0
    if recv_at is not None:
        steps_left = max(0, length - 1)
        current, current_t = recv_at, vars_[recv_at][0]
        for _ in range(steps_left):
            changers = pool.changers.get(current_t, ())
            chainers = pool.chainers.get(current_t, ())
            use_chain = bool(chainers) and (not changers or r.random() < 0.5)
            pick = chainers if use_chain else changers
            if not pick:
                break
            o = r.choice(pick)
            e = call_on(o, "v" + str(current), r, pool, vars_)
            if e is None:
                continue
            if use_chain:
                vars_.append((o["ret"], e))
                STATEMENT_OPS.append(o["cls"] + "." + o["name"])
                current = len(vars_) - 1
            else:
                vars_.append(("V", e))
                STATEMENT_OPS.append(o["cls"] + "." + o["name"])
        e = call_on(target, "v" + str(current), r, pool, vars_)
        if e is not None:
            vars_.append((target["ret"], e))
            STATEMENT_OPS.append(target["cls"] + "." + target["name"])
        else:
            recv_at = None
    if recv_at is None:
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
    """`o` invoked on an expression we already have, rather than on a fresh one.

    `receiver` is already a variable name, so it needs no parentheses. The regex hint is set
    here as well as in `call`: the receiver-centred sequences reach their target through this
    path, so a hint placed only in `call` never fired for the operations that matter --
    `"abc".matches(re)` is built here, and the corpus went unused while the generated programs
    were full of `matches`, `split` and `replaceAll` calls fed with Unicode noise.
    """
    was, held = REGEX_HINT[0], REGEX_PAIR[0]
    REGEX_HINT[0] = was or (o["cls"], o["name"]) in REGEX_OPS
    if REGEX_HINT[0] and not was:
        REGEX_PAIR[0] = None
    try:
        args = []
        for t in o["args"]:
            e = argument(t, r, pool, vars_, 0)
            if e is None:
                return None
            args.append(e)
        return receiver + "." + o["name"] + "(" + ", ".join(args) + ")"
    finally:
        REGEX_HINT[0], REGEX_PAIR[0] = was, held


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
    # **The reference runs TWICE, in separate processes, and a case that disagrees with itself
    # is dropped before anything is compared.** That single step decides nondeterminism by
    # experiment instead of by a list somebody maintains: the clock, object identity,
    # randomness, the default locale and the ambient environment all fail to reproduce and
    # remove themselves. The hand-written list in `policy.py` failed six times -- `Calendar`,
    # `getSystemProperties`, `jdk.swing.interop`, `HttpServer`, `com.sun.jdi`,
    # `com.sun.security.auth` -- and then once more through an exemption that handed `now` back
    # to `java.time`, which put 24 clock readings into the baseline. It costs one extra run of
    # the fast side per batch.
    unstable = set()
    if SELF_CHECK and not ref_timeout:
        try:
            again = subprocess.run([str(JAVA), "-cp", str(d), name], capture_output=True,
                                   text=True, encoding="utf-8", errors="replace", timeout=limit)
            first, second = collect(ref_out), collect(again.stdout)
            for k, v in first.items():
                if second.get(k) != v:
                    unstable.add(k)
        except subprocess.TimeoutExpired:
            pass
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
        "ref_timeout": ref_timeout, "unstable": unstable}


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
            info["unstable"] = set(info.get("unstable", ())) | set(i2.get("unstable", ()))
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

# The category a declared gap is relabelled to. It stays a divergence -- it is counted in
# `diverge` like any other, so the numbers remain comparable with campaigns run before this
# existed -- but it is held out of the cause ranking and of reduction.
DECLARED = "declared gap"

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
    if "->" in e:
        # A lambda is shrunk as a whole or not at all. Taking a character out of `(p0, p1) ->`
        # produces something that does not compile, and one bad candidate in a round makes
        # javac reject the batch and the reduction stop.
        return out
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


VAR_REF = re.compile(r"\bv(\d+)\b")


def drop_at(vars_, i):
    """`vars_` without declaration `i`, with every later reference renumbered.

    Variables are named by POSITION, so removing one from the middle shifts all the others
    down and leaves their expressions pointing at the wrong place. The first version did the
    removal with a slice and nothing else, which has two failure modes and the quiet one is
    the dangerous one: a reference that falls outside the list makes javac reject the batch and
    the reduction gives up, but a reference that still lands inside now names a DIFFERENT value
    and compiles. The reduced case then diverges for another reason and we believe it.
    """
    out = []
    for j, (t, e) in enumerate(vars_):
        if j == i:
            continue
        out.append((t, VAR_REF.sub(
            lambda m: "v" + str(int(m.group(1)) - 1) if int(m.group(1)) > i else m.group(0), e)))
    return out


def dangling(vars_):
    """Whether any expression names a variable that does not exist before it."""
    for j, (_, e) in enumerate(vars_):
        for m in VAR_REF.finditer(e):
            if int(m.group(1)) >= j:
                return True
    return False


def shrink(vars_, r, pool):
    out = []
    for i in range(len(vars_)):
        # Both guards are needed, and finding that out took a unit test. Renumbering alone is
        # not enough: a later statement that referenced the DROPPED variable keeps its name and
        # silently points at whatever slid into that slot -- `v0 + v2` became `v0 + v1`, which
        # compiles as string concatenation and means something else entirely. So a statement is
        # only droppable when nothing after it names it, AND the survivors get renumbered.
        used = re.compile(r"\bv" + str(i) + r"\b")
        if any(used.search(e) for _, e in vars_[i + 1:]):
            continue
        cand = drop_at(vars_, i)
        if cand and not dangling(cand):
            out.append(cand)
    for i, (t, e) in enumerate(vars_):
        # A named constant is the simplest value a library type can have, and for most of the
        # library it is the ONLY simplification available: `literal()` answers None for
        # everything that is not a primitive, a String or an array of those, so without this a
        # case built out of constructors and producers could only ever be shortened, never
        # simplified. Tried before the literals because it applies far more often.
        for c in pool.constants_for(t)[:4]:
            if len(c) < len(e):
                out.append(vars_[:i] + [(t, c)] + vars_[i + 1:])
        for _ in range(3):
            lit = literal(t, r, pool.cfg)
            if lit is not None and len(lit) < len(e):
                out.append(vars_[:i] + [(t, lit)] + vars_[i + 1:])
        for e2 in simpler(e):
            if e2 != e:
                out.append(vars_[:i] + [(t, e2)] + vars_[i + 1:])
    return [c for c in out if not dangling(c)]


def case_text(vars_):
    out = []
    for i, (t, e) in enumerate(vars_):
        out.append(e + ";" if t == "V" else java_name(t) + " v" + str(i) + " = " + e + ";")
    return " ".join(out)



def keeps_failing(ref_v, ours_v, kind, want_cause, mode):
    """Whether a candidate still shows the failure we are reducing.

    Requiring the exact same category -- what the first version did -- ends a reduction the
    moment shrinking changes it, and shrinking changes it often: drop a declaration and a case
    that `threw at a different statement` now throws at the same one, so the category moves
    while the defect does not. Delta debugging without a usable oracle solves this by testing a
    weaker relation than equality, and ours is ready made: the cause key, which is the pair of
    exception classes with the statement index deliberately dropped.

    `strict` is the old behaviour, `any` accepts any divergence at all and is only safe when
    the case has one plausible defect in it.
    """
    k = classify(ref_v, ours_v)
    if k is None:
        return False
    if mode == "any":
        return True
    if mode == "strict":
        return k == kind
    return cause_key({"ref": ref_v, "ours": ours_v, "kind": k}) == want_cause

def holds(cand, kind, pool, tag, limit, times, want_cause=None, mode="cause"):
    """Whether `cand` still diverges the same way, over `times` separate executions.

    Our VM crashes and hangs intermittently, so a single execution is not evidence. The
    delta-debugging-under-flakiness work replaces "the failure is preserved" with a statistical
    assessment over repeated runs, and -- the part that keeps it affordable -- only pays for the
    repeats when the decision depends on them. So candidates are screened once, in bulk, and
    only the one about to be accepted (or the ones about to end the reduction) are re-run.
    """
    for j in range(times):
        ref, ours, info = run_once(tag + "c" + str(j), [cand], pool, limit)
        if ref is None or 0 not in ref:
            return False
        if not keeps_failing(ref[0], ours.get(0), kind, want_cause, mode):
            return False
    return True


def reduce_case(vars_, kind, pool, r, tag, limit, rounds=20, confirm=2,
                want_cause=None, mode="cause"):
    cur, steps = vars_, []
    flaky = 0
    for n in range(rounds):
        cands = sorted([c for c in shrink(cur, r, pool) if c and size(c) < size(cur)], key=size)
        cands = cands[:80]
        if not cands:
            break
        ref, ours, info = run_once("R" + tag + "x" + str(n), cands, pool, limit)
        if ref is None:
            break
        ok = sorted([i for i in info["live"]
                     if i in ref and keeps_failing(ref[i], ours.get(i), kind, want_cause, mode)],
                    key=lambda k: size(cands[k]))
        # A candidate that looked good once gets confirmed before it is committed; if it does
        # not hold up, the next best is tried instead of ending the reduction.
        chosen = None
        for i in ok[:3]:
            assert not dangling(cands[i]), "a reduced case names a variable that does not exist"
            if confirm <= 0 or holds(cands[i], kind, pool, "R" + tag + "x" + str(n), limit,
                                     confirm, want_cause, mode):
                chosen = i
                break
            flaky += 1
        if chosen is None:
            # Nothing survived. Before concluding the case is minimal, give the smallest few
            # another chance: this is exactly where an intermittent failure ends a reduction
            # early and leaves a case bigger than it needed to be.
            revived = None
            for i in sorted(range(len(cands)), key=lambda k: size(cands[k]))[:3]:
                if i in ok:
                    continue
                if holds(cands[i], kind, pool, "R" + tag + "r" + str(n), limit, 1,
                         want_cause, mode):
                    revived = i
                    break
            if revived is None:
                break
            chosen = revived
            flaky += 1
        steps.append({"java": case_text(cands[chosen]), "ref": ref.get(chosen),
                      "ours": ours.get(chosen)})
        cur = cands[chosen]
    ref, ours, _ = run_once("R" + tag + "end", [cur], pool, limit)
    return cur, steps, (ref or {}).get(0), (ours or {}).get(0), flaky


# =============================================================================================
# building the pool from the discovered surface
# =============================================================================================

def lambda_body(ret, r, pool):
    """An expression of type `ret` for the inside of a synthesised lambda.

    Deliberately simple. The point of the lambda is to let the operation under test RUN -- a
    `sort` that receives a comparator, a `computeIfAbsent` that receives a function -- not to
    fuzz the lambda itself. A body that fails to compile costs the whole batch, so anything
    uncertain falls back to a constant of the right shape.
    """
    if ret == "V":
        return None
    if ret == "Z":
        return r.choice(["true", "false"])
    if ret in ("I", "S", "B"):
        return r.choice(["0", "1", "-1"])
    if ret == "J":
        return "0L"
    if ret == "C":
        return "(char) 97"
    if ret == "D":
        return "0.0d"
    if ret == "F":
        return "0.0f"
    if ret == "Ljava/lang/String;":
        return text_literal(r, pool.cfg)
    named = pool.constants_for(ret)
    if named:
        return r.choice(named)
    lit = literal(ret, r, pool.cfg)
    if lit is not None:
        return lit
    return "null"


def lambda_for(t, r, pool):
    """`(p0, p1) -> body` for a functional interface, or None when `t` is not one."""
    m = SAM.get(t)
    if m is None:
        for sub in sorted(pool.subtypes.get(t, ())):
            if sub in SAM:
                t, m = sub, SAM[sub]
                break
    if m is None:
        return None
    args, ret = split_desc(m["desc"])
    params = ", ".join("p" + str(i) for i in range(len(args)))
    head = "(" + params + ")"
    body = lambda_body(ret, r, pool)
    if body is None:
        return head + " -> { }"
    # A parameter is reused as the body now and then, which is how an identity function or a
    # comparator that actually looks at its arguments gets generated at all.
    if args and r.random() < 0.3:
        i = r.randrange(len(args))
        if args[i] == ret:
            return head + " -> p" + str(i)
    return head + " -> " + body


def usable_lambdas(pool, cfg, report=print):
    """Ask javac which synthesised lambdas compile, and keep only those.

    javap tells us a type has one abstract method; it does not tell us javac will accept a
    lambda for it. Generic bounds, inherited abstract members and types that are not really
    functional all break at compile time, and one bad case takes its whole batch with it. So the
    list is validated the same way the referable classes are: write one probe, read the
    compiler's complaints, believe the compiler.
    """
    import random as _random
    names = sorted(SAM)
    if not names:
        return []
    key = hashlib.sha256(("|".join(names)).encode("utf-8")).hexdigest()[:16]
    memo = HERE / ("lambdas-%s.json" % key)
    if memo.exists():
        try:
            ok = set(json.load(io.open(str(memo), encoding="utf-8")))
            for t in list(SAM):
                if t not in ok:
                    del SAM[t]
            report("lambdas: %d functional interfaces javac accepts (cached)" % len(SAM))
            return sorted(SAM)
        except Exception:
            pass
    r = _random.Random(0)
    d = WORK / "lambdaprobe"
    d.mkdir(parents=True, exist_ok=True)
    lines = ["public class LambdaProbe {"]
    for i, t in enumerate(names):
        expr = lambda_for(t, r, pool)
        lines.append("  Object g" + str(i) + "() { " + java_name(t) + " x = " + expr
                     + "; return x; }")
    lines.append("}")
    src = d / "LambdaProbe.java"
    src.write_text(NEWLINE.join(lines) + NEWLINE, encoding="utf-8")
    pr = subprocess.run([str(JAVAC), "-nowarn", "-d", str(d), str(src)], capture_output=True,
                        text=True, encoding="utf-8", errors="replace", timeout=900)
    bad = set()
    for mm in re.finditer(r"LambdaProbe\.java:(\d+): error", pr.stdout + pr.stderr):
        i = int(mm.group(1)) - 2
        if 0 <= i < len(names):
            bad.add(names[i])
    for t in bad:
        SAM.pop(t, None)
    io.open(str(memo), "w", encoding="utf-8", newline="").write(
        json.dumps(sorted(SAM)) + NEWLINE)
    report("lambdas: %d of %d functional interfaces javac accepts"
           % (len(SAM), len(names)))
    return sorted(SAM)


# =============================================================================================
# the API dependency graph
# =============================================================================================
# What it costs to obtain a value of each type, and which operation to use. The first version
# asked for an argument by trying three producers at random at depth one and giving up: a type
# that needs two steps to build was unreachable in practice even though the pool could express
# it, and nothing said so. This is the reachability of RULF -- an operation is reachable when
# every one of its parameters is -- computed as a fixpoint, with the cheapest producers kept as
# the recipe so the generator follows a known path instead of guessing.
COST = {}           # type -> statements needed to obtain one, or absent when unreachable
RECIPE = {}         # type -> the producers that achieve that cost
FREE = 0            # a literal, a constant, a factory or a lambda: no statement needed


def plan_types(pool, report=print, rounds=6):
    COST.clear()
    RECIPE.clear()

    def free(t):
        if t in PRIMS or t == "Ljava/lang/String;":
            return True
        if t.startswith("["):
            return free(t[1:])
        return (t in FACTORIES or bool(pool.constants_for(t)) or t in SAM
                or any(sub in SAM for sub in pool.subtypes.get(t, ())))

    universe = sorted(set(list(pool.by_return) + [a for o in pool.ops for a in o["args"]]))
    for t in universe:
        if free(t):
            COST[t] = FREE
    changed = True
    rnd = 0
    while changed and rnd < rounds:
        changed, rnd = False, rnd + 1
        for t in universe:
            best, winners = None, []
            for o in producers_for(pool, t):
                need = [COST.get(a) for a in o["args"]]
                if not o["static"] and not o["ctor"]:
                    need.append(COST.get(jvm_name(o["cls"])))
                if any(x is None for x in need):
                    continue
                c = 1 + (max(need) if need else 0)
                if best is None or c < best:
                    best, winners = c, [o]
                elif c == best:
                    winners.append(o)
            if best is None:
                continue
            if t not in COST or best < COST[t]:
                COST[t] = min(best, COST.get(t, best))
                RECIPE[t] = winners
                changed = True
            elif COST[t] == best and t not in RECIPE:
                RECIPE[t] = winners
    usable = 0
    for o in pool.ops:
        need = [COST.get(a) for a in o["args"]]
        if not o["static"] and not o["ctor"]:
            need.append(COST.get(jvm_name(o["cls"])))
        if all(x is not None for x in need):
            usable += 1
    report("reachability: %d of %d operations have every argument obtainable; "
           "%d types reachable, %d of them for free"
           % (usable, len(pool.ops), len(COST),
              sum(1 for v in COST.values() if v == FREE)))
    return usable

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
    # Constants and the hierarchy, from the same discovery pass. A constant is only usable if
    # its own declaring class survived the policy, and the javac probe later drops the ones
    # whose class the compiler refuses to name.
    CONSTANTS.clear()
    SUPERS.clear()
    for cls, c in doc["classes"].items():
        for sup in c.get("supers", []):
            SUPERS.setdefault(jvm_name(cls), set()).add(jvm_name(sup))
    # Transitive: `ChronoUnit` is an `Enum` and a `TemporalUnit`, and whatever those are.
    for t in sorted(SUPERS):
        seen, stack = set(), sorted(SUPERS[t])
        while stack:
            x = stack.pop()
            if x in seen:
                continue
            seen.add(x)
            stack.extend(SUPERS.get(x, ()))
        SUPERS[t] = seen
    SAM.clear()
    for cls, c in doc["classes"].items():
        if pol.reject_class(cls) or not c.get("sam"):
            continue
        SAM[jvm_name(cls)] = c["sam"]
    kept_constants = 0
    for cls, c in doc["classes"].items():
        if pol.reject_class(cls):
            continue
        for k in c.get("constants", []):
            CONSTANTS.setdefault(k["desc"], []).append(cls + "." + k["name"])
            kept_constants += 1
    value_types = {jvm_name(c) for c in VALUE_CLASSES
                   if doc["classes"].get(c, {}).get("declares_toString")}
    pool = Pool(ops, cfg, value_types, classes)
    report("pool: %d operations over %d classes, %d named constants, %d hierarchy edges"
           % (len(ops), len(classes), kept_constants,
              sum(len(v) for v in SUPERS.values())))
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
    # Cached: the answer depends only on the class list and the compiler, and compiling a probe
    # with a method per class costs minutes. The A/B over three seeds paid that toll six times
    # and finished one run of six inside the time it had.
    key = hashlib.sha256(("\n".join(names)).encode("utf-8")).hexdigest()[:16]
    memo = HERE / ("referable-%s.json" % key)
    if memo.exists():
        try:
            bad = set(json.load(io.open(str(memo), encoding="utf-8")))
            if bad:
                drop_classes(pool, bad)
            report("referable: %d classes javac refuses to name (cached), %d operations left"
                   % (len(bad), len(pool.ops)))
            return sorted(bad)
        except Exception:
            pass
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
        drop_classes(pool, bad)
    io.open(str(memo), "w", encoding="utf-8", newline="").write(
        json.dumps(sorted(bad)) + "\n")
    report("referable: %d classes javac refuses to name, %d operations left"
           % (len(bad), len(pool.ops)))
    return sorted(bad)


def drop_classes(pool, bad):
    for t in list(CONSTANTS):
        CONSTANTS[t] = [e for e in CONSTANTS[t] if e.rsplit(".", 1)[0] not in bad]
        if not CONSTANTS[t]:
            del CONSTANTS[t]
    pool.ops = [o for o in pool.ops if o["cls"] not in bad]
    pool.usable_classes = pool.usable_classes - set(bad)
    pool.by_return = {}
    for o in pool.ops:
        pool.by_return.setdefault(o["produces"], []).append(o)
    pool.index()
    pool.index_mutators()


# =============================================================================================
DEFAULT_CFG = {
    "regex_weight": 0.75,
    "lambda_weight": 0.6,
    "mutate_weight": 0.3,
    "constant_weight": 0.5,
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



# =============================================================================================
# guidance
# =============================================================================================
# The walk over the operation pool spends the same budget on every operation, which means it
# spends as much on `String.indexOf` as on a `CodeBuilder` method that cannot even be built.
# This is the missing third component: deciding what deserves attention. Three pieces, each
# from a paper, and one substitution of our own.
#
# **Thompson sampling over a Beta-Bernoulli bandit** (T-Scheduler). One arm per operation, two
# counters per arm: how often it produced something new, how often it did not. Draw from
# Beta(1+a, 1+b) and take the largest. It was chosen over the alternatives for a reason that
# matters here more than its results: it has **no hyperparameters**. Every measurement in this
# project costs about an hour, and a scheduler that needed tuning would never get tuned.
#
# **Retiring saturated arms** (MABFuzz). An arm that has produced nothing new in its last W
# pulls is set aside, and the budget moves to arms with no evidence against them. Untried arms
# sit at Beta(1,1) with mean 0.5, so they naturally outrank arms that have accumulated failures
# -- which is the "replace it with a fresh arm" behaviour, for free.
#
# **Rareness correction** (T-Scheduler 3.4). Novelty is not worth the same everywhere. An arm
# whose new outcomes are outcomes half the library also produces -- one more
# NullPointerException -- is worth less than one that produced something seen nowhere else.
#
# **The reward signal is ours.** Every paper here rewards new code coverage, and we have no
# instrumentation and no way to get it without rewriting the VM. What we do have is the outcome
# we already record: the exception class and which statement threw, or the shape of the value.
# Coarsened into a signature, "an outcome this operation had not produced before" is a coverage
# proxy that costs nothing, and it measures behaviour observed rather than lines executed --
# which is all our oracle can see anyway.
SIG_NUM = re.compile(r"^-?\d+$")


def signature(enc):
    """A coarse shape of an outcome, loose enough that repetition actually happens.

    Using the value itself would make every outcome novel and the reward meaningless: the
    bandit would reward every arm equally and degenerate into the round-robin it replaces.
    """
    if enc is None:
        return "none"
    t = thrown(enc)
    if t is not None:
        return "X%d:%s" % (t[0], t[1].rsplit(".", 1)[-1])
    out = []
    for field in str(enc).split("|"):
        if field.startswith("S["):
            out.append("S%d" % field.count(",") if "," in field else "S1")
        elif field.startswith("A["):
            out.append("A%d" % field.count(";"))
        elif field.startswith("A["):
            out.append("A")
        elif field.startswith("T["):
            out.append("T")
        elif field in ("true", "false"):
            out.append("b")
        elif field == "null":
            out.append("0")
        elif field.startswith("c"):
            out.append("c")
        elif field.startswith("d") or field.startswith("f"):
            out.append(field[0] + ("nan" if "NaN" in field else
                                   "inf" if "Infinity" in field else "n"))
        elif SIG_NUM.match(field):
            v = int(field)
            out.append("n0" if v == 0 else ("n+" if v > 0 else "n-"))
        else:
            out.append("?")
    return "|".join(out)


class Bandit(object):
    """Beta-Bernoulli per operation, with retirement and a rareness penalty."""

    def __init__(self, window=8, rare_weight=0.5, arm="class", top_arms=25,
                 reward_mode="global", blind=False):
        self.blind = blind
        self.arm = arm
        self.top_arms = top_arms
        self.reward_mode = reward_mode
        self.a = {}
        self.b = {}
        self.seen = {}            # arm -> the signatures it has produced
        self.quiet = {}           # arm -> pulls since the last novel signature
        self.retired = set()
        self.global_sig = {}
        self.window = window
        self.rare_weight = rare_weight
        self.pulls = 0
        self.novel = 0
        self.cursor = {}       # per arm, how far into its operations we have gone
        self._groups = None
        self.fprints = {}      # divergence fingerprints seen, for the fingerprint reward
        self.found = 0         # how many of them were new
        self.cold_credit = 0.25

    def key(self, o):
        """What counts as one arm.

        **Per operation does not work, and the arithmetic says so before any experiment.** With
        8060 operations and ~235 cases in a batch, an arm is pulled at most once, and a first
        pull is novel by definition: the first run came back with 233 novel outcomes out of 233
        pulls, zero failures and zero retirements. A bandit with no repeated pulls has no
        evidence to learn from, and the published formulations all assume the opposite shape --
        a few hundred seeds pulled thousands of times.
        
        Per class gives ~1000 arms and a handful of pulls each per batch, which accumulates
        across campaigns into something a posterior can actually move on. Per package is
        coarser still, for when even that is too thin.
        """
        if self.arm == "class":
            return o["cls"]
        if self.arm == "package":
            return ".".join(o["cls"].split(".")[:3])
        return o["cls"] + "." + o["name"] + o["desc"]

    def load(self, path):
        try:
            d = json.load(io.open(path, encoding="utf-8"))
        except Exception:
            return False
        self.a = d.get("a", {})
        self.b = d.get("b", {})
        self.seen = {k: set(v) for k, v in d.get("seen", {}).items()}
        self.quiet = d.get("quiet", {})
        self.retired = set(d.get("retired", ()))
        self.global_sig = d.get("global_sig", {})
        self.pulls = d.get("pulls", 0)
        self.novel = d.get("novel", 0)
        self.cursor = d.get("cursor", {})
        self.fprints = {tuple(k.split("\u0001")): v
                        for k, v in d.get("fprints", {}).items()}
        self.found = d.get("found", 0)
        return True

    def save(self, path):
        io.open(path, "w", encoding="utf-8", newline="").write(json.dumps({
            "a": self.a, "b": self.b,
            "seen": {k: sorted(v) for k, v in self.seen.items()},
            "quiet": self.quiet, "retired": sorted(self.retired),
            "global_sig": self.global_sig, "pulls": self.pulls, "novel": self.novel,
            "cursor": self.cursor, "found": self.found,
            "fprints": {"\u0001".join(str(x) for x in k): v
                        for k, v in self.fprints.items()}}) + NEWLINE)

    def rareness(self, arm):
        # With global novelty the correction already lives in the reward, so applying it again
        # at selection time would charge the same arm twice for the same thing. It stays only
        # for the per-class mode, which has no correction of its own.
        if self.reward_mode != "class":
            return 1.0
        sigs = self.seen.get(arm)
        if not sigs:
            return 1.0
        avg = sum(self.global_sig.get(x, 1) for x in sigs) / float(len(sigs))
        return 1.0 / (1.0 + self.rare_weight * math.log(1.0 + avg))

    def group(self, ops):
        """Operations bucketed by arm, so a chosen arm can hand back one of its own."""
        if self._groups is None:
            self._groups = {}
            for o in ops:
                self._groups.setdefault(self.key(o), []).append(o)
        return self._groups

    def choose(self, ops, n, r):
        """`n` operations to spend the next batch on: sample the arms, then draw from them.

        With a coarse arm the scheduler decides WHICH CLASS deserves the budget, and the
        operation inside it is still taken in order, so every operation of a favoured class
        gets its turn rather than one of them getting all the attention.
        """
        groups = self.group(ops)
        live = [k for k in groups if k not in self.retired]
        if not live:
            live = list(groups)
        if self.blind:
            # **Concentration without learning.** Three seeds said the bandit breaks even
            # overall because its two halves cancel: concentrating on few classes gains about
            # six fingerprints per batch, and the learning loses about seven by sending the
            # budget back to classes whose easy outcomes are already spent. This mode keeps the
            # half that works -- pick the classes at random, walk them deeply -- and drops the
            # half that does not. No state, no posterior, no parameters beyond how many
            # classes share a batch. If it matches the bandit, the learning was the problem.
            scored = [(0.0, k) for k in live]
            r.shuffle(scored)
        else:
            scored = []
            for k in live:
                theta = r.betavariate(1.0 + self.a.get(k, 0), 1.0 + self.b.get(k, 0))
                scored.append((theta * self.rareness(k), k))
            scored.sort(key=lambda x: -x[0])
        # **Concentrate.** Taking one operation from each of the top `n` arms spreads the budget
        # exactly as thinly as the round-robin it is supposed to replace, and leaves every arm
        # with a single pull -- which is how two runs in a row reported 100% novelty, zero
        # failures and zero retirements. The budget goes to a FEW arms, deeply: `top` of them,
        # `n // top` cases each. That is what makes a posterior move at all.
        top = max(1, min(self.top_arms, len(scored)))
        per = max(1, n // top)
        out = []
        for _, k in scored[:top]:
            bucket = groups[k]
            take = self.cursor.get(k, 0)
            for j in range(per):
                out.append(bucket[(take + j) % len(bucket)])
            self.cursor[k] = take + per
            if len(out) >= n:
                break
        return out[:n]

    def update(self, o, enc, r, kind=None, ours=None, blamed=None):
        """Credit the arm for what this pull produced.

        **Novelty is global, not per arm.** Rewarding "an outcome new to THIS class" let one
        arm keep collecting credit for the same defect: `SegmentAllocator.allocateFrom` was
        pulled 19 times and produced 2 fingerprints, both the same bug -- it throws
        IllegalArgumentException where the reference throws IndexOutOfBoundsException. The arm
        stayed at the top because its accumulated successes outweighed the slow trickle of
        failures, so the scheduler dug the same hole nineteen times.
        
        The fix is the rareness correction of T-Scheduler, applied to the reward: a signature
        seen once pays in full, one seen seventeen times pays about a seventeenth. That keeps
        the depth -- the arm is still a class and the budget is still concentrated -- while an
        arm whose ways of failing are exhausted stops being paid and retires itself.
        """
        k = self.key(o)
        sig = signature(enc)
        before = self.global_sig.get(sig, 0)
        self.global_sig[sig] = before + 1
        self.seen.setdefault(k, set()).add(sig)
        self.pulls += 1
        if self.reward_mode == "class":
            paid = sig not in self.seen.get(k, set()) or before == 0
        elif self.reward_mode == "fingerprint":
            # **Pay for what we actually count.** The other two modes reward a new outcome
            # shape, while every comparison in this project is scored in distinct divergence
            # fingerprints. Rewarding one thing and measuring another is why the learning
            # looked useless: an arm could collect credit for producing a new kind of
            # *agreement*. Here a case pays in full only when it produces a divergence
            # fingerprint nobody has produced before.
            #
            # That reward is sparse -- about one case in six diverges, and a repeat fingerprint
            # pays nothing -- so a new outcome shape still pays a little, to keep some signal
            # flowing while the posterior is cold. Without that trickle the arms would all look
            # identical until the first divergence landed.
            paid = False
            if kind is not None:
                fprint = (kind, blamed or k, sig, signature(ours))
                seen_before = self.fprints.get(fprint, 0)
                self.fprints[fprint] = seen_before + 1
                if seen_before == 0:
                    paid = True
                    self.found += 1
            if not paid and before == 0:
                paid = r.random() < self.cold_credit
        elif self.reward_mode == "unseen":
            # **Pay only the first visit.** Four measured configurations said the same thing:
            # any reward computed from what an arm produced sends the budget back to classes
            # whose few defects are already spent, and loses to simply advancing. This mode
            # inverts it -- an arm pays on its first pull and never again, so its posterior
            # sinks below the untouched ones and the frontier moves. It keeps the half that
            # did measure well (concentration, by `top_arms`) and spends it on classes nobody
            # has visited, which is the one combination the four runs suggested and none of
            # them tested.
            paid = len(self.seen.get(k, ())) <= 1 and self.a.get(k, 0) == 0
        else:
            paid = before == 0 or r.random() < 1.0 / (1.0 + before)
        if paid:
            self.a[k] = self.a.get(k, 0) + 1
            self.quiet[k] = 0
            self.novel += 1
        else:
            self.b[k] = self.b.get(k, 0) + 1
            self.quiet[k] = self.quiet.get(k, 0) + 1
            if self.quiet[k] >= self.window:
                self.retired.add(k)


# =============================================================================================
# sibling amplification
# =============================================================================================
# MirrorFuzz starts from the observation that a defect in one API is usually shared with the
# APIs that do the same kind of thing, and spends an LLM plus documentation on finding those
# pairs. Here the relation is exact and free: the overloads of the same method, the same method
# name on classes that share a supertype, the same name elsewhere in the package. When a
# divergence lands, its relatives are queued and tried in the next batch instead of waiting for
# the schedule to reach them -- which, at one pass per 8060 operations, can be an hour away.
#
# It reports its own hit rate: the divergence rate among amplified cases next to the rate among
# the rest. If relatives are no more likely to diverge than anything else, the premise is wrong
# for this library and the number will say so.
AMPLIFIED = set()        # operation keys already expanded, so nothing is queued twice
QUEUE = []               # operations waiting to be tried because a relative diverged


def op_key(o):
    return o["cls"] + "." + o["name"] + o["desc"]


def build_family(pool, doc):
    """name -> operations with that name, and class -> the classes it shares a supertype with."""
    by_name = {}
    for o in pool.ops:
        by_name.setdefault(o["name"], []).append(o)
    kin = {}
    for cls in pool.usable_classes:
        sups = set(doc["classes"].get(cls, {}).get("supers", ()))
        sups.discard("java.lang.Object")
        kin[cls] = sups
    return by_name, kin


def siblings(o, pool, by_name, kin, limit=12):
    """The relatives of `o`, nearest first: overloads, then kin, then the package."""
    out, seen = [], {op_key(o)}

    def add(cand):
        k = op_key(cand)
        if k not in seen and k not in AMPLIFIED:
            seen.add(k)
            out.append(cand)

    same_name = by_name.get(o["name"], ())
    for c in same_name:                       # overloads of the very same method
        if c["cls"] == o["cls"]:
            add(c)
    mine = kin.get(o["cls"], set())
    for c in same_name:                       # the same name where a supertype is shared
        if c["cls"] != o["cls"] and (mine & kin.get(c["cls"], set()) or c["cls"] in mine):
            add(c)
    pkg = ".".join(o["cls"].split(".")[:3])
    for c in same_name:                       # the same name elsewhere in the package
        if c["cls"] != o["cls"] and ".".join(c["cls"].split(".")[:3]) == pkg:
            add(c)
    for c in pool.ops:                        # other methods of the class that diverged
        if c["cls"] == o["cls"] and c["name"] != o["name"]:
            add(c)
        if len(out) >= limit:
            break
    return out[:limit]


def cause_key(x):
    """What makes two divergences the same defect, for the purpose of filing one report.

    Not the operation and not the class. Three minimal cases from one batch -- in
    `ByteBuffer.getFloat`, `FloatBuffer.wrap` and `SegmentAllocator.allocateFrom` -- all showed
    `IllegalArgumentException` where the reference throws `IndexOutOfBoundsException`, which is
    almost certainly one wrong decision about range validation replicated across `java.nio`.
    Grouping by operation would have filed three reports and spent three reductions; grouping
    by the pair of exception classes files one.

    The statement index is deliberately dropped: whether the case died on its first or second
    declaration is a property of the generated case, not of the defect.
    """
    a, b = thrown(x["ref"]), thrown(x["ours"])
    if a and b:
        return ("exc", a[1], b[1])
    if a or b:
        side = a or b
        return ("exc-vs-value", x["kind"], side[1])
    return ("value", x["kind"], signature(x["ref"]), signature(x["ours"]))


def pick_representatives(divs, limit):
    """One case per cause, biggest causes first, shortest case within each.

    Biggest first because a pair seen thirty times is more likely to be a systematic defect
    than a one-off, and shortest first because the reducer has less to undo.
    """
    groups = {}
    for x in divs:
        if x.get("declared_gap"):
            # Counted and written out elsewhere; it must not take a reduction slot from a
            # defect nobody chose.
            continue
        if x.get("blamed"):
            groups.setdefault(cause_key(x), []).append(x)
    ordered = sorted(groups.items(), key=lambda kv: (-len(kv[1]), str(kv[0])))
    out = []
    for key, members in ordered[:limit]:
        rep = min(members, key=lambda y: (y["java"].count(";"), len(y["java"])))
        out.append((key, len(members), rep))
    return out, len(groups)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jdk", required=True)
    ap.add_argument("--surface", default=str(HERE / "surface.json"))
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--batches", type=int, default=5)
    ap.add_argument("--per-batch", type=int, default=400)
    ap.add_argument("--length", type=int, default=3, help="declarations per case")
    ap.add_argument("--reduce-predicate", choices=["strict", "cause", "any"], default="cause",
                    help="what a candidate must still show to count as reducing the same "
                         "failure")
    ap.add_argument("--reduce-confirm", type=int, default=2,
                    help="extra executions used to confirm a candidate before accepting it; "
                         "0 trusts a single run")
    ap.add_argument("--reduce", type=int, default=8,
                    help="how many distinct causes to minimise (0 turns reduction off)")
    ap.add_argument("--limit", type=int, default=30, help="seconds per batch per side")
    ap.add_argument("--min-split", type=int, default=4)
    ap.add_argument("--max-splits", type=int, default=6)
    ap.add_argument("--boot", default=None, help="library our VM runs against")
    ap.add_argument("--vm", default=None,
                    help="the run-headless binary to measure. Together with --boot and "
                         "--surface this aims a campaign at a frozen target: see "
                         "FROZEN-TARGET.md for why a moving one makes fuzzer progress "
                         "unmeasurable")
    ap.add_argument("--allow-unsafe", action="store_true",
                    help="include classes that touch files, processes, network or devices")
    ap.add_argument("--allow-hostile", action="store_true",
                    help="include classes that are safe but stall a campaign")
    ap.add_argument("--exclude", nargs="*", default=[], help="extra class prefixes to drop")
    ap.add_argument("--only", nargs="*", default=[],
                    help="restrict the walk to these class prefixes. Without it a corner of "
                         "the library cannot be reached in reasonable time: the cursor walks "
                         "8060 operations in order, so `java.util.regex` is an hour away from "
                         "the start and a change aimed at it cannot be tested at all")
    # Several queues can run at once, each taking every n-th operation. Splitting by index
    # rather than by package keeps every queue mixed: one queue would otherwise get all of
    # `java.time` and the other all of `javax`, and their divergence rates would not be
    # comparable. Each queue needs its OWN cursor file -- two campaigns sharing one is how an
    # earlier run ended up walking the first few operations of the library over and over.
    ap.add_argument("--amplify", type=float, default=0.0,
                    help="share of each batch reserved for relatives of operations that "
                         "diverged; 0 turns it off")
    ap.add_argument("--schedule", choices=["roundrobin", "bandit", "concentrated"],
                    default="roundrobin",
                    help="how the budget is spent across operations")
    ap.add_argument("--bandit-state", default=None,
                    help="where the arm counters live between campaigns")
    ap.add_argument("--bandit-arm", choices=["operation", "class", "package"],
                    default="class", help="granularity of one arm")
    ap.add_argument("--bandit-reward",
                    choices=["global", "class", "fingerprint", "unseen"], default="global",
                    help="whether a new outcome counts library-wide or only for its class")
    ap.add_argument("--bandit-top", type=int, default=25,
                    help="how many arms share one batch; fewer means deeper per arm")
    ap.add_argument("--bandit-window", type=int, default=8,
                    help="pulls without a new outcome before an arm is retired")
    ap.add_argument("--no-self-check", action="store_true",
                    help="do not run the reference twice; trust the exclusion lists instead")
    ap.add_argument("--no-mutate", action="store_true",
                    help="draw every value fresh instead of perturbing one in hand")
    ap.add_argument("--shard", type=int, default=0, help="which queue this is, from 0")
    ap.add_argument("--shards", type=int, default=1, help="how many queues in total")
    ap.add_argument("--cursor", default=None,
                    help="where the position in the operation pool is kept between campaigns")
    ap.add_argument("--restart", action="store_true", help="ignore the stored cursor")
    ap.add_argument("--pool-only", action="store_true", help="report the pool and stop")
    ap.add_argument("--out", default=None)
    a = ap.parse_args()

    global JAVAC, JAVA, BOOT, MIN_SPLIT, MAX_SPLITS, SELF_CHECK, HEADLESS
    b = Path(a.jdk) / "bin"
    JAVAC, JAVA = b / "javac.exe", b / "java.exe"
    BOOT, MIN_SPLIT, MAX_SPLITS = a.boot, a.min_split, a.max_splits
    if a.vm:
        HEADLESS = Path(a.vm).resolve()
    if not HEADLESS.exists():
        raise SystemExit("no VM binary at " + str(HEADLESS))
    SELF_CHECK = not a.no_self_check

    if a.no_mutate:
        DEFAULT_CFG["mutate_weight"] = 0.0
    pol = Policy(a.allow_unsafe, a.allow_hostile, a.exclude)
    pool, dropped, doc = load_pool(a.surface, pol, DEFAULT_CFG)
    doc_for_family = doc
    refused = keep_referable(pool, DEFAULT_CFG)
    for t in list(SAM):
        if java_name(t) not in pool.usable_classes:
            del SAM[t]
    usable_lambdas(pool, DEFAULT_CFG)
    plan_types(pool)
    if not pool.ops:
        print("nothing left to invoke", file=sys.stderr)
        sys.exit(1)
    if a.pool_only:
        print(json.dumps({"operations": len(pool.ops), "classes": len(pool.usable_classes),
                          "lambdas": len(SAM), "types_reachable": len(COST),
                          "dropped": dropped, "refused_by_javac": refused[:40]},
                         ensure_ascii=False, indent=1))
        return

    ordered = sorted(pool.ops, key=lambda o: (o["cls"], o["name"], o["desc"]))
    # The graph can tell which operations have an unobtainable argument, and an earlier version
    # took them out of the walk to save the 13.5% of the budget they consume. That was wrong:
    # **an operation that is never reached has to show up as not reached.** Skipping it in
    # advance turns a measured coverage hole into a silent one, and the whole point of the
    # `unreachable` count is to say which operations the fuzzer cannot exercise. They stay in
    # the walk, they fail their three attempts, and they get counted and named.
    def buildable(o):
        need = [COST.get(x) for x in o["args"]]
        if not o["static"] and not o["ctor"]:
            need.append(COST.get(jvm_name(o["cls"])))
        return all(x is not None for x in need)

    unbuildable = [o for o in ordered if not buildable(o)]
    print("walk: %d operations, %d of them predicted unbuildable (kept, so they get counted)"
          % (len(ordered), len(unbuildable)))
    if a.only:
        # Exact class or a package prefix, never a bare startswith: `java.lang.String` would
        # otherwise also select `StringBuffer` and `StringBuilder`, which is how a run meant to
        # exercise regex came back dominated by `StringBuffer.insert`.
        ordered = [o for o in ordered
                   if any(o["cls"] == q or o["cls"].startswith(q + ".") for q in a.only)]
        print("only: %d operations match %s" % (len(ordered), " ".join(a.only)))
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
            "unbuildable": len(unbuildable),
            "unbuildable_sample": sorted(o["cls"] + "." + o["name"]
                                         for o in unbuildable)[:40],
            "vm_binary": str(HEADLESS), "vm_sha256": vm_digest,
            "shard": a.shard, "shards": a.shards,
            "tool": "libfuzz.py (whole library, surface discovered with javap)",
            "reference": a.jdk, "under_test": str(a.boot or "KajiLibrary"),
            # A frozen target is only frozen if the record says so. Without this a result
            # measured against the snapshot and one measured against the working tree are
            # indistinguishable in the output, which is the whole problem this is meant to fix.
            "frozen_target": bool(a.vm and a.boot),
            "seed": a.seed, "batches": a.batches, "per_batch": a.per_batch,
            "length": a.length, "limit": a.limit,
            "pool_operations": len(ordered), "pool_classes": len(pool.usable_classes),
            "cursor_start": start}
    stats = {"cases": 0, "agree": 0, "diverge": 0, "repeats": 0, "rewarded": 0,
             "unreachable": 0, "no_reference": 0, "unattributed": 0,
             "ended_with_value": 0, "threw_at_first": 0, "threw_later": 0, "unstable": 0,
             "amp_cases": 0, "amp_diverge": 0, "amp_queue": 0, "causes": 0,
             "by_kind": {}, "by_operation": {}, "by_gap": {}, "op_declared": {},
             "declared": 0,
             "ref_s": 0.0, "ours_s": 0.0}
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
            "unstable_sample": unstable_sample,
            "divergences": [{k: v for k, v in d.items() if k != "vars"} for d in divs],
            "reduced": reduced}, ensure_ascii=False, indent=2) + "\n")

    def bump(table, key):
        table[key] = table.get(key, 0) + 1

    global FOCUS, COLLECT
    r = random.Random(a.seed)
    vm_digest = meta["vm_sha256"]
    at = start
    bandit = None
    state = None
    meta["schedule"] = a.schedule
    if a.schedule in ("bandit", "concentrated"):
        bandit = Bandit(window=a.bandit_window, arm=a.bandit_arm, top_arms=a.bandit_top,
                        reward_mode=a.bandit_reward, blind=a.schedule == "concentrated")
        if a.schedule == "bandit":
            state = a.bandit_state or str(HERE / ("bandit-%d-%d.json" % (a.shard, a.shards)))
            if not a.restart and bandit.load(state):
                print("bandit: resuming with %d pulls, %d novel, %d retired"
                      % (bandit.pulls, bandit.novel, len(bandit.retired)))
    by_name, kin = build_family(pool, doc_for_family)
    unreachable, unstable_sample = [], []
    for bi in range(a.batches):
        batch_start = at
        cases, lits, targets, case_ops = [], {}, {}, {}
        COLLECT = True
        # The cursor advances one operation at a time, and each gets a few attempts before being
        # written off. The first version advanced it per ATTEMPT, so an operation whose case
        # could not be built was consumed without ever being tested: 100 operations spent to
        # test 40. Those are now counted and named, because an operation the fuzzer cannot
        # reach is a hole in coverage, not a detail.
        # Relatives first, up to their share of the batch.
        quota = int(a.per_batch * a.amplify) if a.amplify > 0 else 0
        from_queue, taken = [], 0
        while QUEUE and taken < quota:
            from_queue.append(QUEUE.pop(0))
            taken += 1
        amplified_idx = set()
        picked = bandit.choose(ordered, a.per_batch * 2, r) if bandit else None
        pick_at = 0
        while len(cases) < a.per_batch and at - batch_start < a.per_batch * 3:
            if from_queue:
                o = from_queue.pop(0)
                amplified_idx.add(len(cases))
            elif picked is not None:
                if pick_at >= len(picked):
                    break
                o = picked[pick_at]
                pick_at += 1
                at += 1
            else:
                o = ordered[at % len(ordered)]
                at += 1
            # The budget for building this case comes from the graph: an operation whose
            # arguments need two steps gets two steps, instead of always being asked for at
            # depth one and quietly failing.
            need = [COST.get(x, 1) for x in o["args"]]
            if not o["static"] and not o["ctor"]:
                need.append(COST.get(jvm_name(o["cls"]), 1))
            depth_for = min(3, max([1] + need))
            built = None
            for attempt in range(3):
                FOCUS = tuple(EDGE_TEXT[(len(cases) + attempt) % len(EDGE_TEXT)])
                del USED[:]
                case = make_case(r, pool, max(a.length, depth_for + 1), o)
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
        unstable = set(info.get("unstable", ()))
        for k in info["live"]:
            if k in unstable:
                stats["unstable"] += 1
                if len(unstable_sample) < 60 and k in targets:
                    o = targets[k]
                    unstable_sample.append(o["cls"] + "." + o["name"])
                continue
            if k not in ref:
                stats["no_reference"] += 1
                continue
            text = case_text(cases[k])
            o = targets[k]
            stats["cases"] += 1
            if k in amplified_idx:
                stats["amp_cases"] += 1
            if not str(ref[k]).startswith("EXC:"):
                for t, e in lits.get(k, []):
                    remember(t, e, DEFAULT_CFG)
                stats["rewarded"] += len(lits.get(k, []))
            # Cuanto del presupuesto se gasta en casos que no ejercitan nada: el que lanza en
            # la primera declaracion murio en la validacion de argumentos, antes de llegar a la
            # operacion a la que le tocaba el turno. Sin esta cuenta, "6% de operaciones
            # bloqueadas" parece una buena noticia y esconde que el resto se invoca con basura.
            t_ref = thrown(ref[k])
            if t_ref is None:
                stats["ended_with_value"] += 1
            elif t_ref[0] == 0:
                stats["threw_at_first"] += 1
            else:
                stats["threw_later"] += 1
            kind = classify(ref[k], ours.get(k))
            where, culprit = (None, None)
            gap = None
            if kind is not None:
                where, culprit = blame(ref[k], ours.get(k), case_ops.get(k, []))
                gap = declared_gap(culprit, thrown(ref[k]), thrown(ours.get(k)))
                if gap is not None:
                    kind = DECLARED
            if bandit is not None and not bandit.blind:
                bandit.update(targets[k], ref[k], r, kind, ours.get(k), culprit)
            if kind is None:
                stats["agree"] += 1
                continue
            stats["diverge"] += 1
            if k in amplified_idx:
                stats["amp_diverge"] += 1
            bump(stats["by_kind"], kind)
            if gap is not None:
                stats["declared"] = stats.get("declared", 0) + 1
                bump(stats["by_gap"], gap)
                if culprit:
                    bump(stats["op_declared"], culprit)
            # `where`, not `at`: the first version unpacked this into `at`, which is the
            # cursor over the operation pool. One divergence set the cursor to a statement
            # index -- 0 or 1 -- so the campaign walked the first few operations of the library
            # over and over and reported a cursor that never moved.
            if culprit:
                bump(stats["by_operation"], culprit)
            else:
                stats["unattributed"] += 1
            divs.append({"batch": bi, "k": k, "kind": kind,
                         "scheduled": o["cls"] + "." + o["name"],
                         "blamed": culprit, "statement": where, "declared_gap": gap,
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
        if a.amplify > 0:
            for x in divs:
                if x["batch"] != bi or not x.get("blamed"):
                    continue
                cls, _, name = x["blamed"].rpartition(".")
                for o in pool.ops:
                    if o["cls"] == cls and o["name"] == name:
                        if op_key(o) in AMPLIFIED:
                            continue
                        AMPLIFIED.add(op_key(o))
                        for sib in siblings(o, pool, by_name, kin):
                            AMPLIFIED.add(op_key(sib))
                            QUEUE.append(sib)
                        break
            stats["amp_queue"] = len(QUEUE)
        stats["mutations"] = MUTATIONS[0]
        if bandit is not None and state:
            bandit.save(state)
            stats["bandit"] = {"pulls": bandit.pulls, "novel": bandit.novel,
                               "fingerprints_found": bandit.found,
                               "retired": len(bandit.retired),
                               "signatures": len(bandit.global_sig)}
        save("running")
        io.open(a.cursor, "w", encoding="utf-8", newline="").write(
            json.dumps({"at": at % len(ordered), "total": len(ordered)}) + "\n")

    reps, n_causes = pick_representatives(divs, a.reduce) if a.reduce else ([], 0)
    stats["causes"] = n_causes
    if divs:
        print("")
        print("%d divergences fall into %d distinct causes; reducing the %d biggest"
              % (len(divs), n_causes, len(reps)), flush=True)
    for key, members, d in reps:
        print("reducing [%s] x%d %s" % (d["kind"], members, d["java"][:80]), flush=True)
        t = time.time()
        mn, rsteps, rv, ov, flaky = reduce_case(d["vars"], d["kind"], pool, r,
                                                "%dx%d" % (a.seed, len(reduced)), a.limit,
                                                confirm=a.reduce_confirm,
                                                want_cause=key, mode=a.reduce_predicate)
        reduced.append({"cause": list(key), "covers": members, "kind": d["kind"],
                        "blamed": d["blamed"],
                        "original": d["java"], "minimal": case_text(mn),
                        "minimal_ref": rv, "minimal_ours": ov, "steps": rsteps,
                        "flaky_candidates": flaky,
                        "minimal_kind": classify(rv, ov) if rv is not None else None,
                        "seconds": round(time.time() - t, 1)})
        save("reducing")

    save("done")
    print("")
    print("cases %d · agree %d · diverge %d · repeats %d · unreachable %d"
          % (stats["cases"], stats["agree"], stats["diverge"],
             stats["repeats"], stats["unreachable"]))
    for k, v in sorted(stats["by_kind"].items(), key=lambda kv: -kv[1]):
        print("  %-34s %5d" % (k, v))
    if stats.get("declared"):
        print("declared gaps: %d of the divergences above are differences this library makes "
              "on purpose" % stats["declared"])
        for k, v in sorted(stats["by_gap"].items(), key=lambda kv: -kv[1]):
            print("  x%-4d %s" % (v, k))
    if a.amplify > 0:
        amp_c, amp_d = stats["amp_cases"], stats["amp_diverge"]
        rest_c = stats["cases"] - amp_c
        rest_d = stats["diverge"] - amp_d
        print("amplification: %d relatives tried, %d diverged (%.1f%%); "
              "%d others, %d diverged (%.1f%%)"
              % (amp_c, amp_d, 100.0 * amp_d / max(amp_c, 1),
                 rest_c, rest_d, 100.0 * rest_d / max(rest_c, 1)))
    if bandit is not None:
        print("bandit: %d pulls, %d novel outcomes (%.0f%%), %d arms retired, %d signatures"
              % (bandit.pulls, bandit.novel,
                 100.0 * bandit.novel / max(bandit.pulls, 1),
                 len(bandit.retired), len(bandit.global_sig)))
    print("")
    print("operations blamed most often (charged to the statement that diverged):")
    for k, v in sorted(stats["by_operation"].items(), key=lambda kv: -kv[1])[:15]:
        known = stats["op_declared"].get(k, 0)
        note = "   (%d declared)" % known if known else ""
        print("  %-52s %4d%s" % (k, v, note))
    print("unattributed: %d" % stats["unattributed"])
    print("written", out)


if __name__ == "__main__":
    main()
