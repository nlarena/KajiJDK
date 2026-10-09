package java.util.regex;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

// KajiLibrary's java.util.regex.Matcher — the stateful engine that runs a compiled Pattern
// against an input sequence. A Matcher tracks where the last match landed (group 0) and
// each capturing group's bounds, so group()/start()/end() can report them. The three entry
// points — matches(), lookingAt(), find() — drive the backtracking node tree. The node
// classes reach into this Matcher's package-private state (the region bounds, the group
// bounds, the per-quantifier scratch slots) as they walk.
//
// Matcher implements MatchResult, so a live Matcher can be passed anywhere a MatchResult is
// expected; toMatchResult() freezes the current state into an immutable snapshot.
//
// Left out of this subset, deliberately:
//   * useTransparentBounds/hasTransparentBounds — declared and honest: our lookahead runs
//     inside the region, so transparent bounds would be a lie; setting it throws.
//   * usePattern() — the JDK keeps the current match position and swaps the node tree; our
//     group/scratch arrays are sized in the constructor, so we re-derive them and document
//     that the append position survives (which is the documented contract).
public final class Matcher implements MatchResult {

    private Pattern parentPattern;
    private CharSequence text;

    // The search region [from, to). Package-private: the anchor nodes consult them.
    int from;
    int to;

    // Where the next find() begins; advances past each successful match.
    private int searchFrom;

    // Where appendReplacement() last stopped copying from the input.
    private int lastAppendPosition;

    // Group bounds from the most recent match: groups[2g] start, groups[2g+1] end (-1 unset).
    // Index 0 is the whole match. Nodes record captures here as they walk.
    int[] groups;

    // Per-quantifier scratch: the current repetition count and the position at which the
    // current iteration began (for the zero-width guard). One slot per quantifier.
    int[] counts;
    int[] loopStart;

    // During matches(), the accept node is only valid when it lands exactly at `to`. (The
    // JDK spells this acceptMode == ENDANCHOR; it is *not* the same thing as requireEnd.)
    boolean anchorEnd;

    // Set by any node that looked at, or past, the end of the region — the input hint the
    // JDK exposes through hitEnd().
    boolean hitEnd;

    // Set when the match succeeded only because it reached the end of input, so more input
    // could have changed the outcome. Exposed through requireEnd().
    boolean requireEnd;

    // Anchoring bounds (the default) make ^ and $ match at the region edges. Transparent
    // bounds are not modelled — see useTransparentBounds.
    private boolean anchoringBounds;

    private boolean matched;

    Matcher(Pattern parent, CharSequence text) {
        this.parentPattern = parent;
        this.text = text;
        this.anchoringBounds = true;
        this.sizeScratch(parent);
        this.reset();
    }

    private void sizeScratch(Pattern parent) {
        this.groups = new int[(parent.groupCount + 1) * 2];
        int loops = parent.localCount;
        this.counts = new int[loops];
        this.loopStart = new int[loops];
    }

    public Pattern pattern() {
        return this.parentPattern;
    }

    // Swaps in a different Pattern without losing the current append position, as the JDK
    // does. The group and scratch arrays are re-sized for the new pattern, so any group
    // information from the previous match is lost (also the JDK's documented behavior).
    public Matcher usePattern(Pattern newPattern) {
        if (newPattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        this.parentPattern = newPattern;
        this.sizeScratch(newPattern);
        this.matched = false;
        this.clearGroups();
        return this;
    }

    // Rewinds all match state so this Matcher can be reused from the start of the input.
    public Matcher reset() {
        this.from = 0;
        this.to = this.text.length();
        this.searchFrom = 0;
        this.lastAppendPosition = 0;
        this.matched = false;
        this.hitEnd = false;
        this.requireEnd = false;
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

    // --- Region -----------------------------------------------------------------------

    // Restricts this Matcher to the range [start, end). Resets the match state, as the JDK
    // does, but does not restore `from`/`to` to the full input.
    public Matcher region(int start, int end) {
        int length = this.text.length();
        if (start < 0 || start > length) {
            throw new IndexOutOfBoundsException("start");
        }
        if (end < 0 || end > length) {
            throw new IndexOutOfBoundsException("end");
        }
        if (start > end) {
            throw new IndexOutOfBoundsException("start > end");
        }
        this.reset();
        this.from = start;
        this.to = end;
        this.searchFrom = start;
        return this;
    }

    public int regionStart() {
        return this.from;
    }

    public int regionEnd() {
        return this.to;
    }

    public boolean hasAnchoringBounds() {
        return this.anchoringBounds;
    }

    public Matcher useAnchoringBounds(boolean b) {
        this.anchoringBounds = b;
        return this;
    }

    public boolean hasTransparentBounds() {
        return false;
    }

    // NOT SUPPORTED. Transparent bounds let lookahead/lookbehind and \b see text outside the
    // region; every node in our engine is bounded by `to`, so honoring the setter would be a
    // silent lie. Declared (it is part of the public API) but refuses the only value it
    // cannot deliver.
    public Matcher useTransparentBounds(boolean b) {
        if (b) {
            throw new UnsupportedOperationException("transparent bounds are not supported");
        }
        return this;
    }

    // The lower anchor bound: the region start with anchoring bounds, otherwise the true
    // start of the input. Consulted by Begin and WordBoundary.
    int anchorStart() {
        return this.anchoringBounds ? this.from : 0;
    }

    // The upper anchor bound. Note that the *consuming* nodes always stop at `to`; only the
    // anchors widen when anchoring bounds are switched off.
    int anchorLimit(CharSequence seq) {
        return this.anchoringBounds ? this.to : seq.length();
    }

    // --- Match entry points ------------------------------------------------------------

    // Runs the engine anchored at `start`. `anchorEnd` forces the match to consume the whole
    // region (the matches() contract). Leaves the group bounds set on success.
    private boolean matchAt(int start, boolean anchorEnd) {
        this.clearGroups();
        for (int i = 0; i < this.counts.length; i++) {
            this.counts[i] = 0;
            this.loopStart[i] = -1;
        }
        this.anchorEnd = anchorEnd;
        this.groups[0] = start;
        boolean ok = this.parentPattern.root.match(this, start, this.text);
        this.matched = ok;
        return ok;
    }

    // Attempts to match the entire input region against the pattern.
    public boolean matches() {
        this.hitEnd = false;
        this.requireEnd = false;
        return this.matchAt(this.from, true);
    }

    // Attempts to match the pattern against the start of the input region (a prefix match;
    // unlike matches(), the whole region need not be consumed).
    public boolean lookingAt() {
        this.hitEnd = false;
        this.requireEnd = false;
        return this.matchAt(this.from, false);
    }

    // Scans forward from the end of the previous match for the next subsequence that matches.
    public boolean find() {
        int start = this.searchFrom;
        if (start < this.from) {
            start = this.from;
        }
        this.hitEnd = false;
        this.requireEnd = false;
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
        if (start < 0 || start > this.text.length()) {
            throw new IndexOutOfBoundsException("Illegal start index");
        }
        this.reset();
        this.searchFrom = start;
        return this.find();
    }

    // --- Result accessors (read the state the engine leaves behind) ---

    public int groupCount() {
        return this.groups.length / 2 - 1;
    }

    public boolean hasMatch() {
        return this.matched;
    }

    private void checkMatch() {
        if (!this.matched) {
            throw new IllegalStateException("No match found");
        }
    }

    private void checkGroup(int group) {
        if (group < 0 || group > this.groupCount()) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
    }

    public String group(int group) {
        this.checkMatch();
        this.checkGroup(group);
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
        this.checkMatch();
        this.checkGroup(group);
        return this.groups[group * 2];
    }

    public int start() {
        return this.start(0);
    }

    public int end(int group) {
        this.checkMatch();
        this.checkGroup(group);
        return this.groups[group * 2 + 1];
    }

    public int end() {
        return this.end(0);
    }

    // The name -> group-number map of the compiled pattern. The by-name overloads resolve
    // through this.
    public Map<String, Integer> namedGroups() {
        return this.parentPattern.namedGroups();
    }

    // The by-name overloads. MatchResult supplies them as `default` methods, but the JDK's
    // Matcher declares them concretely (they show up in `javap java.util.regex.Matcher`), so
    // we declare them too rather than inheriting.
    public String group(String name) {
        return this.group(this.groupIndex(name));
    }

    public int start(String name) {
        return this.start(this.groupIndex(name));
    }

    public int end(String name) {
        return this.end(this.groupIndex(name));
    }

    private int groupIndex(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        Integer number = this.namedGroups().get(name);
        if (number == null) {
            throw new IllegalArgumentException("No group with name <" + name + ">");
        }
        return number.intValue();
    }

    // Freezes the current match into a snapshot that later find()/reset() calls cannot
    // disturb. Callers that stash results while iterating need this.
    public MatchResult toMatchResult() {
        int[] copy = new int[this.groups.length];
        for (int i = 0; i < this.groups.length; i++) {
            copy[i] = this.groups[i];
        }
        int first = this.matched ? this.groups[0] : -1;
        int last = this.matched ? this.groups[1] : -1;
        return new ImmutableMatchResult(first, last, copy, this.text.toString(), this.parentPattern);
    }

    // The input hints. hitEnd() reports that the last match operation looked at (or past) the
    // end of the region; requireEnd() reports that a successful match leaned on the end of
    // input, so more input could have turned it into a failure. Both are best-effort in the
    // same sense as the JDK's: they are set by the nodes that actually consulted the bound.
    public boolean hitEnd() {
        return this.hitEnd;
    }

    public boolean requireEnd() {
        return this.requireEnd;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("java.util.regex.Matcher[pattern=");
        sb.append(this.pattern().pattern());
        sb.append(" region=");
        sb.append(this.from);
        sb.append(",");
        sb.append(this.to);
        sb.append(" lastmatch=");
        if (this.matched && this.groups[0] >= 0) {
            sb.append(this.group());
        }
        sb.append("]");
        return sb.toString();
    }

    // --- Append / replace ---------------------------------------------------------------

    // Copies the input from the end of the previous append up to the start of the current
    // match, then the expanded replacement. The pair appendReplacement/appendTail is the
    // building block replaceAll() is defined in terms of.
    public Matcher appendReplacement(StringBuilder sb, String replacement) {
        this.checkMatch();
        sb.append(this.text.subSequence(this.lastAppendPosition, this.start()).toString());
        this.appendExpanded(sb, replacement);
        this.lastAppendPosition = this.end();
        return this;
    }

    public Matcher appendReplacement(StringBuffer sb, String replacement) {
        StringBuilder tmp = new StringBuilder();
        this.appendReplacement(tmp, replacement);
        sb.append(tmp);
        return this;
    }

    // Appends whatever input remains after the last append position.
    public StringBuilder appendTail(StringBuilder sb) {
        sb.append(this.text.subSequence(this.lastAppendPosition, this.text.length()).toString());
        return sb;
    }

    public StringBuffer appendTail(StringBuffer sb) {
        sb.append(this.text.subSequence(this.lastAppendPosition, this.text.length()).toString());
        return sb;
    }

    // Replaces every match of the pattern in the input with `replacement`, expanding group
    // references, and returns the resulting string. The original input is left untouched.
    public String replaceAll(String replacement) {
        this.reset();
        StringBuilder result = new StringBuilder();
        boolean found = this.find();
        while (found) {
            this.appendReplacement(result, replacement);
            found = this.find();
        }
        return this.appendTail(result).toString();
    }

    // Same, with each match's replacement computed from the MatchResult. The function is
    // handed a *snapshot*, so it may safely keep it.
    public String replaceAll(Function<MatchResult, String> replacer) {
        this.reset();
        StringBuilder result = new StringBuilder();
        boolean found = this.find();
        while (found) {
            String r = replacer.apply(this.toMatchResult());
            result.append(this.text.subSequence(this.lastAppendPosition, this.start()).toString());
            result.append(r);
            this.lastAppendPosition = this.end();
            found = this.find();
        }
        return this.appendTail(result).toString();
    }

    // Like replaceAll, but only the first match is replaced.
    public String replaceFirst(String replacement) {
        this.reset();
        if (!this.find()) {
            return this.text.toString();
        }
        StringBuilder result = new StringBuilder();
        this.appendReplacement(result, replacement);
        return this.appendTail(result).toString();
    }

    public String replaceFirst(Function<MatchResult, String> replacer) {
        this.reset();
        if (!this.find()) {
            return this.text.toString();
        }
        String r = replacer.apply(this.toMatchResult());
        StringBuilder result = new StringBuilder();
        result.append(this.text.subSequence(0, this.start()).toString());
        result.append(r);
        this.lastAppendPosition = this.end();
        return this.appendTail(result).toString();
    }

    // Every remaining match, as immutable snapshots.
    //
    // DIFFERENCE FROM THE JDK: the JDK's results() is lazy (a Spliterator that advances the
    // Matcher as the stream is consumed). KajiLibrary's java.util.stream has no
    // Spliterator/StreamSupport, and its only general source is Stream.of(T[]), so ours runs
    // every match eagerly and then wraps the array. Observable differences: this Matcher is
    // left at the end of the input as soon as results() returns, and an unbounded pattern
    // over a huge input allocates all snapshots up front.
    public Stream<MatchResult> results() {
        MatchResult[] all = new MatchResult[8];
        int n = 0;
        while (this.find()) {
            if (n == all.length) {
                MatchResult[] bigger = new MatchResult[all.length * 2];
                for (int i = 0; i < n; i++) {
                    bigger[i] = all[i];
                }
                all = bigger;
            }
            all[n] = this.toMatchResult();
            n = n + 1;
        }
        MatchResult[] exact = new MatchResult[n];
        for (int i = 0; i < n; i++) {
            exact[i] = all[i];
        }
        return Stream.of(exact);
    }

    // Expands one replacement string against the current match into `result`: `$n` is
    // replaced by group n (digits are read greedily but capped so the group exists, the way
    // real javac does), `${name}` by the named group, and `\` escapes the next character.
    private void appendExpanded(StringBuilder result, String replacement) {
        int i = 0;
        int len = replacement.length();
        while (i < len) {
            char c = replacement.charAt(i);
            if (c == '\\') {
                i = i + 1;
                if (i >= len) {
                    throw new IllegalArgumentException("character to be escaped is missing");
                }
                result.append(replacement.charAt(i));
                i = i + 1;
            } else if (c == '$') {
                i = i + 1;
                if (i < len && replacement.charAt(i) == '{') {
                    i = i + 1;
                    StringBuilder name = new StringBuilder();
                    while (i < len && replacement.charAt(i) != '}') {
                        name.append(replacement.charAt(i));
                        i = i + 1;
                    }
                    if (i >= len) {
                        throw new IllegalArgumentException("named capturing group is missing trailing '}'");
                    }
                    i = i + 1;
                    String g = this.group(name.toString());
                    if (g != null) {
                        result.append(g);
                    }
                    continue;
                }
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
                    throw new IllegalArgumentException("Illegal group reference");
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
