package java.util.regex;

// KajiLibrary's java.util.regex.Pattern — a compiled representation of a regular
// expression. Immutable and thread-safe: compile() parses the source once into a node
// tree (the parser lands in H5-T2), and each matcher() spins a fresh Matcher over that
// shared tree. This class is the public entry point; the actual node hierarchy and the
// backtracking engine are package-private. A KajiLibrary subset (no flags beyond the
// constants below, no Serializable, no split() yet).
public final class Pattern {

    // Flag constants (bit positions match real javac so a caller's literals port over).
    public static final int UNIX_LINES = 0x01;
    public static final int CASE_INSENSITIVE = 0x02;
    public static final int COMMENTS = 0x04;
    public static final int MULTILINE = 0x08;
    public static final int DOTALL = 0x20;

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

    private Pattern(String p, int f) {
        this.pattern = p;
        this.flags = f;
        RegexParser parser = new RegexParser(p);
        this.root = parser.parse();
        this.groupCount = parser.groupCount();
        this.localCount = parser.localCount();
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

    // Package-private: real javac keeps the group count internal to Pattern (Matcher
    // exposes the public groupCount()). The parser (H5-T2) sets it.
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
    // can't be closed early — the same trick real javac uses.
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
}
