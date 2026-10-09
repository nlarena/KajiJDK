package java.util.regex;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

// KajiLibrary's java.util.regex.Pattern — a compiled representation of a regular
// expression. Immutable and thread-safe: compile() parses the source once into a node
// tree, and each matcher() spins a fresh Matcher over that shared tree. This class is the
// public entry point; the actual node hierarchy and the backtracking engine are
// package-private (Node.java).
//
// Flags: UNIX_LINES, CASE_INSENSITIVE, COMMENTS, MULTILINE, LITERAL and DOTALL are honored
// — the parser bakes them into the nodes it builds. UNICODE_CASE, CANON_EQ and
// UNICODE_CHARACTER_CLASS are declared (they are part of the API surface and callers' bit
// literals must keep their meaning) but compile() REJECTS them: silently ignoring a flag
// that changes what matches is worse than refusing it. See `unsupportedFlags`.
//
// Not implemented from the JDK's surface: Serializable (we model no serialization).
public final class Pattern {

    // Flag constants — bit positions identical to the JDK so a caller's literals port over.
    public static final int UNIX_LINES = 0x01;
    public static final int CASE_INSENSITIVE = 0x02;
    public static final int COMMENTS = 0x04;
    public static final int MULTILINE = 0x08;
    public static final int LITERAL = 0x10;
    public static final int DOTALL = 0x20;
    public static final int UNICODE_CASE = 0x40;
    public static final int CANON_EQ = 0x80;
    public static final int UNICODE_CHARACTER_CLASS = 0x100;

    // The flags whose behavior this engine cannot deliver. Written as a method rather than a
    // `private static final int` constant on purpose: our javac mis-compiles a `static final`
    // field whose initializer is a *compound* constant expression (`A | B`) — the field gets
    // neither a ConstantValue attribute nor a <clinit> assignment, so it reads back as 0 and
    // the check below would silently pass. In expression position the same `|` compiles to a
    // real `ior`, which is correct.
    private static int unsupportedFlags() {
        return UNICODE_CASE | CANON_EQ | UNICODE_CHARACTER_CLASS;
    }

    private final String pattern;
    private final int flags;

    // The compiled node tree — the head of the chain the engine walks. Package-private so
    // Matcher can reach it.
    Node root;

    // Number of capturing groups the pattern declares (group 0, the whole match, is
    // implicit and not counted).
    int groupCount;

    // Number of quantifiers — the size of Matcher's per-loop scratch arrays.
    int localCount;

    // name -> group number for the pattern's named capturing groups; null when it has none.
    private final Map<String, Integer> named;

    private Pattern(String p, int f) {
        if (p == null) {
            throw new NullPointerException("regex");
        }
        if ((f & unsupportedFlags()) != 0) {
            throw new UnsupportedOperationException(
                "UNICODE_CASE, CANON_EQ and UNICODE_CHARACTER_CLASS are not supported");
        }
        this.pattern = p;
        this.flags = f;
        RegexParser parser = new RegexParser(p, f);
        this.root = parser.parse();
        this.groupCount = parser.groupCount();
        this.localCount = parser.localCount();
        this.named = parser.namedGroups();
    }

    public static Pattern compile(String regex) {
        return new Pattern(regex, 0);
    }

    public static Pattern compile(String regex, int flags) {
        return new Pattern(regex, flags);
    }

    public Matcher matcher(CharSequence input) {
        return new Matcher(this, input);
    }

    public String pattern() {
        return this.pattern;
    }

    public int flags() {
        return this.flags;
    }

    public String toString() {
        return this.pattern;
    }

    // An unmodifiable-in-spirit view of the pattern's named capturing groups. The JDK wraps
    // it with Collections.unmodifiableMap; KajiLibrary's java.util.Collections does not
    // compile yet (finding #204) and Map has no keySet/entrySet to copy through, so this
    // hands back the parser's own map. Callers must treat it as read-only.
    public Map<String, Integer> namedGroups() {
        if (this.named == null) {
            return new HashMap<String, Integer>();
        }
        return this.named;
    }

    // Package-private: real javac keeps the group count internal to Pattern (Matcher
    // exposes the public groupCount()).
    int capturingGroupCount() {
        return this.groupCount;
    }

    // Scans the whole input against `regex` in one shot — the convenience shortcut for a
    // throwaway pattern.
    public static boolean matches(String regex, CharSequence input) {
        return Pattern.compile(regex).matcher(input).matches();
    }

    // Returns a literal-quoted version of `s`: any regex metacharacter in `s` is treated
    // as an ordinary character. Wraps in \Q..\E and breaks any embedded \E so the quoting
    // can't be closed early — the same trick real javac uses. (The parser grew a \Q..\E
    // pre-pass so that this actually round-trips; before that, quote() produced a pattern
    // the engine parsed as the literal letters 'Q' and 'E'.)
    public static String quote(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\Q");
        int len = s.length();
        int i = 0;
        while (i < len) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < len && s.charAt(i + 1) == 'E') {
                sb.append("\\E\\\\E\\Q");
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        sb.append("\\E");
        return sb.toString();
    }

    // --- split -------------------------------------------------------------------------

    // Splits `input` around matches of this pattern. `limit` controls how many times the
    // pattern is applied: positive means at most limit-1 splits (the last element holds the
    // rest), zero means unlimited with trailing empty strings dropped, negative means
    // unlimited with them kept. Same algorithm as the JDK's.
    public String[] split(CharSequence input, int limit) {
        int index = 0;
        boolean matchLimited = limit > 0;
        String[] list = new String[8];
        int size = 0;
        Matcher m = this.matcher(input);
        while (m.find()) {
            if (!matchLimited || size < limit - 1) {
                if (index == 0 && index == m.start() && m.start() == m.end()) {
                    // No empty leading substring for a zero-width match at the beginning.
                    continue;
                }
                if (size == list.length) {
                    list = grow(list, size);
                }
                list[size] = input.subSequence(index, m.start()).toString();
                size = size + 1;
                index = m.end();
            } else if (size == limit - 1) {
                if (size == list.length) {
                    list = grow(list, size);
                }
                list[size] = input.subSequence(index, input.length()).toString();
                size = size + 1;
                index = m.end();
            }
        }
        // No match at all: the input is returned whole.
        if (index == 0) {
            String[] one = new String[1];
            one[0] = input.toString();
            return one;
        }
        if (!matchLimited || size < limit) {
            if (size == list.length) {
                list = grow(list, size);
            }
            list[size] = input.subSequence(index, input.length()).toString();
            size = size + 1;
        }
        int resultSize = size;
        if (limit == 0) {
            while (resultSize > 0 && list[resultSize - 1].isEmpty()) {
                resultSize = resultSize - 1;
            }
        }
        String[] result = new String[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = list[i];
        }
        return result;
    }

    private static String[] grow(String[] a, int size) {
        String[] bigger = new String[a.length * 2];
        for (int i = 0; i < size; i++) {
            bigger[i] = a[i];
        }
        return bigger;
    }

    public String[] split(CharSequence input) {
        return this.split(input, 0);
    }

    // Like split, but the delimiters are kept: the result alternates substring, delimiter,
    // substring, ... and always begins and ends with a (possibly empty) substring.
    public String[] splitWithDelimiters(CharSequence input, int limit) {
        int index = 0;
        boolean matchLimited = limit > 0;
        String[] list = new String[8];
        int size = 0;
        Matcher m = this.matcher(input);
        while (m.find()) {
            if (!matchLimited || size / 2 < limit - 1) {
                if (index == 0 && index == m.start() && m.start() == m.end()) {
                    continue;
                }
                if (size + 2 > list.length) {
                    list = grow(list, size);
                }
                list[size] = input.subSequence(index, m.start()).toString();
                list[size + 1] = input.subSequence(m.start(), m.end()).toString();
                size = size + 2;
                index = m.end();
            }
        }
        if (index == 0 && size == 0) {
            String[] one = new String[1];
            one[0] = input.toString();
            return one;
        }
        if (size == list.length) {
            list = grow(list, size);
        }
        list[size] = input.subSequence(index, input.length()).toString();
        size = size + 1;
        int resultSize = size;
        if (limit == 0) {
            while (resultSize > 0 && list[resultSize - 1].isEmpty()) {
                resultSize = resultSize - 1;
            }
        }
        String[] result = new String[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = list[i];
        }
        return result;
    }

    // The split parts as a stream.
    //
    // DIFFERENCE FROM THE JDK: the JDK's splitAsStream is lazy; KajiLibrary's
    // java.util.stream has no Spliterator/StreamSupport and its only general source is
    // Stream.of(T[]), so ours splits eagerly and wraps the array. The elements are the same;
    // only the laziness is missing.
    public Stream<String> splitAsStream(CharSequence input) {
        return Stream.of(this.split(input, 0));
    }

    // --- predicates ----------------------------------------------------------------------

    // A predicate that tests whether the pattern is found anywhere in its argument.
    public Predicate<String> asPredicate() {
        return new PatternPredicate(this, false);
    }

    // A predicate that tests whether the pattern matches its argument entirely.
    public Predicate<String> asMatchPredicate() {
        return new PatternPredicate(this, true);
    }
}

// The Predicate returned by asPredicate()/asMatchPredicate(). The JDK uses a lambda; a named
// class is the same thing without leaning on invokedynamic + LambdaMetafactory, which keeps
// this usable on the interpreter as it stands. Package-private, so the API gate skips it.
final class PatternPredicate implements Predicate<String> {

    private final Pattern pattern;
    private final boolean whole;

    PatternPredicate(Pattern pattern, boolean whole) {
        this.pattern = pattern;
        this.whole = whole;
    }

    public boolean test(String s) {
        Matcher m = this.pattern.matcher(s);
        if (this.whole) {
            return m.matches();
        }
        return m.find();
    }
}
