# -*- coding: utf-8 -*-
"""What is safe to invoke at random, across the whole of KajiLibrary.

A campaign over 38609 operations will, sooner or later, call every one of them with arbitrary
arguments. Most of the library is pure computation and that is fine. A minority of it talks to the
machine, and calling those at random is not a test, it is damage: `File.delete`, `Files.write`,
`System.exit` (which kills the campaign itself), `Runtime.exec`, sockets, and the AWT/Swing classes
that block waiting for a display that is not there.

So the pool is defined by subtraction: **everything is in except what is listed here**. The lists
are by class-name prefix and by member name, not by package, because the dangerous classes are
scattered -- `java.util` is safe except for its logging, preferences, jar, zip and concurrency
corners -- and because carving by package would throw away most of the library to protect a few
dozen classes.

Three reasons a member is excluded, kept separate because they are not the same thing:

* `UNSAFE_*` would touch the machine: files, processes, network, devices, the clock as a side
  effect. Nothing here is about correctness; these simply must not run.
* `NONDETERMINISTIC_*` would differ between two runs of the SAME implementation -- object
  identity, the clock, randomness, the default locale or timezone. A differential oracle reports
  those as divergences and they are noise.
* `HOSTILE_*` are safe and deterministic but ruin a campaign in practice: they block, allocate
  without bound, or take seconds per call.
"""

# ---------------------------------------------------------------------------- touches the machine
UNSAFE_CLASSES = [
    # Filesystem, in all its spellings.
    "java.io.File", "java.io.FileInputStream", "java.io.FileOutputStream", "java.io.FileReader",
    "java.io.FileWriter", "java.io.RandomAccessFile", "java.io.FileDescriptor",
    "java.io.FilePermission", "java.io.PrintStream", "java.io.PrintWriter",
    "java.nio.file", "java.nio.channels", "java.util.jar", "java.util.zip",
    "java.util.logging", "java.util.prefs", "javax.imageio", "java.io.Console",
    # The virtual machine itself. `System.exit` would kill the campaign from inside.
    "java.lang.System", "java.lang.Runtime", "java.lang.Process", "java.lang.ProcessBuilder",
    "java.lang.ProcessHandle", "java.lang.Shutdown", "java.lang.SecurityManager",
    "java.lang.Thread", "java.lang.ThreadGroup", "java.lang.ClassLoader", "java.lang.Module",
    "java.lang.ModuleLayer", "java.lang.StackWalker", "jdk.internal.misc",
    "jdk.internal.access", "jdk.internal.loader", "jdk.internal.ref", "jdk.internal.module",
    # Arbitrary code execution through reflection or handles.
    "java.lang.reflect", "java.lang.invoke", "java.lang.Class", "java.lang.Runtime",
    "javax.lang.model", "javax.annotation.processing", "javax.tools",
    # Network and anything that dials out. `com.sun.net.httpserver` was missing from the first
    # list and the fuzzer went straight for it: once attribution was fixed, `HttpServer` and
    # `com.sun.security.auth` (which looks up operating-system users) were the two operations
    # blamed most often in the whole library.
    "java.net", "javax.net", "java.rmi", "javax.rmi", "javax.naming", "javax.sql", "java.sql",
    "com.sun.net", "com.sun.security", "jdk.net", "jdk.httpserver", "jdk.javadoc",
    "jdk.internal.net", "sun.net",
    "javax.management", "javax.security.auth",
    # Devices and the desktop: these want a display, a printer or a sound card. The first
    # library-wide batch found three spellings the package list missed -- `jdk.swing.interop`
    # threw HeadlessException, and the accessibility utilities listen on the AWT event queue.
    "java.awt", "javax.swing", "javax.sound", "javax.print", "javax.accessibility",
    "java.applet", "javax.jnlp", "jdk.swing.interop", "com.sun.java.accessibility",
    "com.sun.awt", "com.sun.swing", "com.sun.media", "com.sun.imageio",
    # The debugger interface attaches to other virtual machines, and the management beans read
    # the live process: `getRuntimeMXBean().getSystemProperties()` answered 52 on one side and
    # 16 on the other, which is the environment talking, not the library.
    "com.sun.jdi", "com.sun.tools", "jdk.jdi", "java.lang.management",
    "javax.management", "jdk.management", "com.sun.management", "jdk.jfr",
    "jdk.jshell", "jdk.dynalink", "javax.script", "com.sun.source",
    # Parsers that will happily resolve an external entity over the network.
    "javax.xml", "org.w3c", "org.xml", "com.sun.org", "jakarta.xml",
    # Key generation and the like: slow, and some of it reaches for entropy devices.
    "java.security.KeyPairGenerator", "java.security.KeyStore", "java.security.SecureRandom",
    "java.security.Provider", "java.security.Security", "javax.crypto",
]

UNSAFE_MEMBERS = [
    "exit", "halt", "gc", "runFinalization", "load", "loadLibrary", "exec", "start", "stop",
    "destroy", "destroyForcibly", "interrupt", "suspend", "resume", "setDaemon", "setPriority",
    "delete", "deleteOnExit", "createNewFile", "mkdir", "mkdirs", "renameTo", "setReadOnly",
    "setWritable", "setReadable", "setExecutable", "setLastModified", "createTempFile",
    "setProperty", "clearProperty", "setProperties", "setSecurityManager", "setErr", "setOut",
    "setIn", "addShutdownHook", "removeShutdownHook", "newInstance", "invoke", "forName",
    "setAccessible", "defineClass", "arraycopy", "wait", "notify", "notifyAll", "join",
    "openConnection", "connect", "bind", "accept", "write", "flush", "close", "println", "print",
]

# ------------------------------------------------------------------- differs between two runs
NONDETERMINISTIC_CLASSES = [
    "java.util.Random", "java.security.SecureRandom", "java.lang.ThreadLocal",
    "java.time.Clock", "java.util.UUID",
    # Anything whose no-argument factory reads the clock or the ambient zone. A generated
    # `Calendar.getInstance()` is "now", so the two sides disagree for a reason that has nothing
    # to do with the library. Spotted in the first library-wide batch.
    "java.util.Calendar", "java.util.GregorianCalendar", "java.util.Date",
    "java.util.TimeZone", "java.util.SimpleTimeZone",
]

NONDETERMINISTIC_MEMBERS = [
    # Object identity, in every form it leaks out.
    "hashCode", "identityHashCode", "toString", "clone", "getClass",
    # The clock.
    "now", "currentTimeMillis", "nanoTime", "systemUTC", "systemDefaultZone", "instant",
    "millis", "getTime",
    # Randomness.
    "random", "nextInt", "nextLong", "nextDouble", "nextFloat", "nextBoolean", "nextGaussian",
    "nextBytes", "ints", "longs", "doubles", "randomUUID",
    # The ambient environment: default locale, default timezone, default charset, free memory.
    "getDefault", "setDefault", "availableProcessors", "freeMemory", "totalMemory", "maxMemory",
    "getenv", "getProperty", "getProperties", "lineSeparator", "getAvailableLocales",
    "getAvailableIDs", "getAvailableZoneIds",
]

# `hashCode` and `toString` are excluded above as MEMBERS, which is too blunt on its own: for a
# value class like BigInteger both are specified and comparable. These classes get them back --
# but ONLY those, listed here. The first version exempted a value class from the whole
# nondeterministic list, which handed `now` back to `java.time`: the baseline ended up with 24
# divergences from `LocalTime.now`, `Instant.now` and friends, 2.3% of the total, every one of
# them the clock rather than the library. A hand-maintained list has interactions like this, and
# that is the argument for deciding nondeterminism by running the reference twice instead.
EXEMPT_FOR_VALUE_CLASSES = ["hashCode", "toString", "clone", "equals", "compareTo"]
VALUE_CLASSES = [
    "java.lang.String", "java.lang.Integer", "java.lang.Long", "java.lang.Short",
    "java.lang.Byte", "java.lang.Character", "java.lang.Boolean", "java.lang.Double",
    "java.lang.Float", "java.lang.StringBuilder", "java.lang.StringBuffer",
    "java.math.BigInteger", "java.math.BigDecimal",
    "java.time.Duration", "java.time.Instant", "java.time.LocalDate", "java.time.LocalTime",
    "java.time.LocalDateTime", "java.time.Period", "java.time.Year", "java.time.YearMonth",
    "java.time.MonthDay", "java.time.ZoneOffset",
    "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble",
    "java.util.BitSet", "java.util.Locale", "java.util.Currency",
    "java.util.ArrayList", "java.util.LinkedList", "java.util.HashMap", "java.util.TreeMap",
    "java.util.HashSet", "java.util.TreeSet", "java.util.ArrayDeque", "java.util.EnumSet",
]

# --------------------------------------------------------------- safe, but ruins a campaign
HOSTILE_CLASSES = [
    "java.util.concurrent", "java.util.stream", "java.util.Spliterator",
    "java.lang.ref", "java.text.Bidi",
]

HOSTILE_MEMBERS = [
    "repeat", "pow", "modPow", "nextProbablePrime", "isProbablePrime", "sqrt", "sqrtAndRemainder",
    "parallelMultiply", "parallelStream", "stream", "spliterator", "iterator", "forEach",
    "sleep", "await", "park", "onSpinWait", "yield", "lines", "chars", "codePoints",
    "readLine", "readAllBytes", "transferTo", "copyOf", "fill",
]


def _prefix_match(name, prefixes):
    return any(name == p or name.startswith(p + ".") or name.startswith(p + "$")
               for p in prefixes)


class Policy(object):
    """Decides whether one discovered operation may be invoked, and why not when it may not."""

    def __init__(self, allow_unsafe=False, allow_hostile=False, extra_classes=(),
                 extra_members=()):
        self.allow_unsafe = allow_unsafe
        self.allow_hostile = allow_hostile
        self.extra_classes = list(extra_classes)
        self.extra_members = set(extra_members)
        self.value_classes = set(VALUE_CLASSES)

    def reject_class(self, name):
        if _prefix_match(name, self.extra_classes):
            return "excluded-by-request"
        if not self.allow_unsafe and _prefix_match(name, UNSAFE_CLASSES):
            return "unsafe"
        if _prefix_match(name, NONDETERMINISTIC_CLASSES):
            return "nondeterministic"
        if not self.allow_hostile and _prefix_match(name, HOSTILE_CLASSES):
            return "hostile"
        return None

    def reject_member(self, class_name, member):
        if member in self.extra_members:
            return "excluded-by-request"
        if not self.allow_unsafe and member in UNSAFE_MEMBERS:
            return "unsafe"
        if member in NONDETERMINISTIC_MEMBERS:
            if not (class_name in self.value_classes
                    and member in EXEMPT_FOR_VALUE_CLASSES):
                return "nondeterministic"
        if not self.allow_hostile and member in HOSTILE_MEMBERS:
            return "hostile"
        return None


# =============================================================================================
# declared gaps
# =============================================================================================

# Differences this library produces ON PURPOSE: a documented decision not to implement
# something, raising an exception instead of behaving the way the JDK does. A campaign finds
# them over and over, because the generator keeps handing the same operations values that walk
# into them -- four of the six divergences left in `java.util.regex.Pattern` after two real
# fixes were this and nothing else.
#
# They are NOT filtered out. Dropping a divergence because it matched a pattern is exactly how
# a deduplication heuristic buries a real defect, and we already watched one cause group turn
# out to hold two unrelated bugs. They get their own category instead: still counted, still
# written to the output, still visible in the summary, but kept out of the cause ranking so
# they stop spending reduction slots on an answer we already know.
#
# Each entry pins the operation AND the exception we raise, and on top of that the reference
# has to have got PAST the statement we died on -- see `declared_gap`. A genuine bug in the
# same class with the same exception is therefore still reported, which matters more than it
# sounds: the first draft of this rule would have swallowed the flag-mask defect fixed the same
# afternoon.
DECLARED_GAPS = [
    {
        "class": "java.util.regex.Pattern",
        "members": None,          # any member, `<receiver>` included
        "exception": "java.lang.UnsupportedOperationException",
        "note": "UNICODE_CASE, CANON_EQ and UNICODE_CHARACTER_CLASS are refused rather than "
                "silently ignored, in the flags argument and in the inline (?u)/(?U) forms; "
                "see the class comment on Pattern",
    },
]


def declared_gap(operation, ref_thrown, ours_thrown):
    """The note for the declared gap this divergence is, or None when it is not one.

    `operation` is what the blame charged, e.g. `java.util.regex.Pattern.<receiver>`; the other
    two are `(statement index, exception class)` pairs, or None when that side returned a value.

    What makes it a gap is not which exception we raised -- it is that we **refused where the
    reference carried on**. The reference must have got past the statement we died on: either
    it returned a value, or it threw later. When both sides die on the same statement they are
    arguing about *which* exception is right, and that is an ordinary disagreement worth
    reporting.

    That distinction is not academic. The first version of this function matched on the
    operation and the exception alone, and it would have labelled `Pattern.compile("x", -5)` --
    reference IllegalArgumentException, ours UnsupportedOperationException, a cause with ten
    members -- a declared gap and never reduced it. That was the missing flag-mask validation,
    a real defect fixed the same day. A deduplication rule that buries the bug it was written
    alongside is the exact failure Klees et al. warn about, so the rule asks the narrower
    question.
    """
    if not operation or not ours_thrown:
        return None
    if ref_thrown is not None and ref_thrown[0] <= ours_thrown[0]:
        return None
    cls, _, member = operation.rpartition(".")
    for g in DECLARED_GAPS:
        if g["exception"] != ours_thrown[1]:
            continue
        if cls != g["class"] and not cls.startswith(g["class"] + "."):
            continue
        if g["members"] is not None and member not in g["members"]:
            continue
        return g["note"]
    return None
