package java.util.regex;

// KajiLibrary's java.util.regex.Matcher — the stateful engine that runs a compiled Pattern
// against an input sequence. A Matcher tracks where the last match landed (group 0) and
// each capturing group's bounds, so group()/start()/end() can report them. The three entry
// points — matches(), lookingAt(), find() — drive the backtracking node tree (H5-T3). The
// node classes reach into this Matcher's package-private state (the region end, the group
// bounds, the per-quantifier scratch slots) as they walk. A KajiLibrary subset (no
// replaceAll/replaceFirst yet — those arrive with H5-T4).
public final class Matcher {

    private final Pattern parentPattern;
    private CharSequence text;

    // The search region [from, to). Full input for now (region() is out of subset scope).
    private int from;
    int to;

    // Where the next find() begins; advances past each successful match.
    private int searchFrom;

    // Group bounds from the most recent match: groups[2g] start, groups[2g+1] end (-1 unset).
    // Index 0 is the whole match. Nodes record captures here as they walk.
    int[] groups;

    // Per-quantifier scratch: the current repetition count and the position at which the
    // current iteration began (for the zero-width guard). One slot per quantifier.
    int[] counts;
    int[] loopStart;

    // During matches(), the accept node is only valid when it lands exactly at `to`.
    boolean requireEnd;

    private boolean matched;

    Matcher(Pattern parent, CharSequence text) {
        this.parentPattern = parent;
        this.text = text;
        this.groups = new int[(parent.groupCount + 1) * 2];
        int loops = parent.localCount;
        this.counts = new int[loops];
        this.loopStart = new int[loops];
        this.reset();
    }

    public Pattern pattern() {
        return this.parentPattern;
    }

    // Rewinds all match state so this Matcher can be reused from the start of the input.
    public Matcher reset() {
        this.from = 0;
        this.to = this.text.length();
        this.searchFrom = 0;
        this.matched = false;
        this.clearGroups();
        return this;
    }

    // Rewinds and re-points this Matcher at a new input sequence.
    public Matcher reset(CharSequence input) {
        this.text = input;
        return this.reset();
    }

    private void clearGroups() {
        for (int i = 0; i < this.groups.length; i++) {
            this.groups[i] = -1;
        }
    }

    // Runs the engine anchored at `start`. `requireEnd` forces the match to consume the whole
    // region (the matches() contract). Leaves the group bounds set on success.
    private boolean matchAt(int start, boolean requireEnd) {
        this.clearGroups();
        for (int i = 0; i < this.counts.length; i++) {
            this.counts[i] = 0;
            this.loopStart[i] = -1;
        }
        this.requireEnd = requireEnd;
        this.groups[0] = start;
        boolean ok = this.parentPattern.root.match(this, start, this.text);
        this.matched = ok;
        return ok;
    }

    // Attempts to match the entire input region against the pattern.
    public boolean matches() {
        return this.matchAt(this.from, true);
    }

    // Attempts to match the pattern against the start of the input region (a prefix match;
    // unlike matches(), the whole region need not be consumed).
    public boolean lookingAt() {
        return this.matchAt(this.from, false);
    }

    // Scans forward from the end of the previous match for the next subsequence that matches.
    public boolean find() {
        int start = this.searchFrom;
        if (start < this.from) {
            start = this.from;
        }
        while (start <= this.to) {
            if (this.matchAt(start, false)) {
                int end = this.groups[1];
                if (end > start) {
                    this.searchFrom = end;
                } else {
                    this.searchFrom = end + 1;
                }
                return true;
            }
            start = start + 1;
        }
        this.matched = false;
        this.searchFrom = this.to + 1;
        return false;
    }

    // Resets this Matcher, then scans from `start` for the next match.
    public boolean find(int start) {
        this.reset();
        this.searchFrom = start;
        return this.find();
    }

    // --- Result accessors (read the state the engine leaves behind) ---

    public int groupCount() {
        return this.groups.length / 2 - 1;
    }

    // NOTE: real javac throws IllegalStateException ("No match available") here and
    // IndexOutOfBoundsException for a bad group index; KajiLibrary doesn't model those
    // java.lang subtypes yet, so a plain RuntimeException stands in for now.
    public String group(int group) {
        if (!this.matched) {
            throw new RuntimeException("No match available");
        }
        int s = this.groups[group * 2];
        int e = this.groups[group * 2 + 1];
        if (s < 0 || e < 0) {
            return null;
        }
        return this.text.subSequence(s, e).toString();
    }

    public String group() {
        return this.group(0);
    }

    public int start(int group) {
        if (!this.matched) {
            throw new RuntimeException("No match available");
        }
        return this.groups[group * 2];
    }

    public int start() {
        return this.start(0);
    }

    public int end(int group) {
        if (!this.matched) {
            throw new RuntimeException("No match available");
        }
        return this.groups[group * 2 + 1];
    }

    public int end() {
        return this.end(0);
    }

    // --- Replacement (H5-T4) ---

    // Replaces every match of the pattern in the input with `replacement`, expanding group
    // references, and returns the resulting string. The original input is left untouched.
    public String replaceAll(String replacement) {
        this.reset();
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (this.find()) {
            result.append(this.text.subSequence(last, this.start()).toString());
            this.appendExpanded(result, replacement);
            last = this.end();
        }
        result.append(this.text.subSequence(last, this.text.length()).toString());
        return result.toString();
    }

    // Like replaceAll, but only the first match is replaced.
    public String replaceFirst(String replacement) {
        this.reset();
        if (!this.find()) {
            return this.text.toString();
        }
        StringBuilder result = new StringBuilder();
        result.append(this.text.subSequence(0, this.start()).toString());
        this.appendExpanded(result, replacement);
        result.append(this.text.subSequence(this.end(), this.text.length()).toString());
        return result.toString();
    }

    // Expands one replacement string against the current match into `result`: `$n` is
    // replaced by group n (digits are read greedily but capped so the group exists, the way
    // real javac does), and `\` escapes the next character. Named refs `${name}` are H5-T5.
    private void appendExpanded(StringBuilder result, String replacement) {
        int i = 0;
        int len = replacement.length();
        while (i < len) {
            char c = replacement.charAt(i);
            if (c == '\\') {
                i = i + 1;
                if (i < len) {
                    result.append(replacement.charAt(i));
                    i = i + 1;
                }
            } else if (c == '$') {
                i = i + 1;
                int gnum = 0;
                boolean any = false;
                while (i < len && replacement.charAt(i) >= '0' && replacement.charAt(i) <= '9') {
                    int nextNum = gnum * 10 + (replacement.charAt(i) - '0');
                    if (nextNum > this.groupCount()) {
                        break;
                    }
                    gnum = nextNum;
                    i = i + 1;
                    any = true;
                }
                if (!any) {
                    throw new RuntimeException("no group after '$' in replacement");
                }
                String g = this.group(gnum);
                if (g != null) {
                    result.append(g);
                }
            } else {
                result.append(c);
                i = i + 1;
            }
        }
    }

    // Returns a literal replacement string: any '\' or '$' in `s` is escaped so the result
    // has no special meaning to replaceAll/replaceFirst.
    public static String quoteReplacement(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '$') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
