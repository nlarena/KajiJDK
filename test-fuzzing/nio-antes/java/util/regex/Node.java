package java.util.regex;

import java.util.HashMap;
import java.util.Map;

// KajiLibrary's regex node tree + parser (all package-private — real javac keeps these
// nested inside Pattern; we make them top-level in the package to sidestep the compiler's
// generic-enclosing-capture limitation, and the API gate skips them since the JDK has no
// java.util.regex.Node counterpart).
//
// The engine is an NFA walked by recursive backtracking in continuation-passing style: a
// Node matches at a position and, on success, delegates to its `next` (the continuation).
// Backtracking falls out of the recursion — a node that fails returns false, and the caller
// tries its next alternative. RegexParser turns a pattern string into the linked tree.
//
// Compile flags are resolved at *parse* time: the parser bakes CASE_INSENSITIVE, DOTALL,
// MULTILINE and UNIX_LINES into the nodes it builds, so the engine itself stays branch-free
// on flags. COMMENTS and LITERAL are handled entirely in the parser/pre-pass.

// --- The node hierarchy ---------------------------------------------------------------

abstract class Node {
    // The continuation: the node to run after this one matches.
    Node next;

    Node() {
        this.next = null;
    }

    // Attempts to match starting at index i; on success returns next.match(m, j, seq).
    // Every concrete node overrides this; the default is a safety net.
    boolean match(Matcher m, int i, CharSequence seq) {
        throw new RuntimeException("unmatchable node");
    }

    // --- shared character helpers -------------------------------------------------

    // ASCII case folding. CASE_INSENSITIVE on its own is defined by the JDK as US-ASCII
    // folding ("Unicode-aware case folding" is what UNICODE_CASE adds, and we don't offer
    // that flag), so folding to lower case over 'A'..'Z' is the *correct* behavior here and
    // not a shortcut.
    static char fold(char c) {
        if (c >= 'A' && c <= 'Z') {
            return (char) (c + 32);
        }
        return c;
    }

    static boolean sameChar(char a, char b, boolean ci) {
        if (a == b) {
            return true;
        }
        if (ci) {
            return fold(a) == fold(b);
        }
        return false;
    }

    // A line terminator, in the JDK's default (non-UNIX_LINES) sense: LF, CR, NEL, LS, PS.
    static boolean isLineTerm(char c, boolean unixLines) {
        if (c == '\n') {
            return true;
        }
        if (unixLines) {
            return false;
        }
        // NEL (U+0085), LINE SEPARATOR (U+2028) and PARAGRAPH SEPARATOR (U+2029),
        // spelled numerically so the source stays pure ASCII.
        return c == '\r' || c == (char) 0x85 || c == (char) 0x2028 || c == (char) 0x2029;
    }

    static boolean isWordChar(char c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        if (c >= '0' && c <= '9') {
            return true;
        }
        return c == '_';
    }
}

// Terminal accept node: the end of the pattern. Records the overall match end (group 0's
// end). During matches() the accept is only valid at the region end.
final class LastNode extends Node {
    boolean match(Matcher m, int i, CharSequence seq) {
        if (m.anchorEnd && i != m.to) {
            m.hitEnd = true;
            return false;
        }
        m.groups[1] = i;
        return true;
    }
}

// A single literal character.
final class CharNode extends Node {
    char ch;
    boolean ci;

    CharNode(char ch, boolean ci) {
        this.ch = ch;
        this.ci = ci;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        if (i >= m.to) {
            m.hitEnd = true;
            return false;
        }
        if (sameChar(seq.charAt(i), this.ch, this.ci)) {
            return this.next.match(m, i + 1, seq);
        }
        return false;
    }
}

// The '.' metacharacter. Without DOTALL it matches any character except a line terminator
// (the terminator set narrows to LF under UNIX_LINES); with DOTALL it matches everything.
final class AnyNode extends Node {
    boolean dotall;
    boolean unixLines;

    AnyNode(boolean dotall, boolean unixLines) {
        this.dotall = dotall;
        this.unixLines = unixLines;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        if (i >= m.to) {
            m.hitEnd = true;
            return false;
        }
        if (this.dotall || !isLineTerm(seq.charAt(i), this.unixLines)) {
            return this.next.match(m, i + 1, seq);
        }
        return false;
    }
}

// A character class [...]: a set of ranges (a single character is a range a-a), optionally
// negated. Ranges grow on demand the way the rest of KajiLibrary grows its backing arrays.
final class CharClassNode extends Node {
    boolean negate;
    boolean ci;
    char[] lo;
    char[] hi;
    int count;

    CharClassNode() {
        this.negate = false;
        this.ci = false;
        this.lo = new char[8];
        this.hi = new char[8];
        this.count = 0;
    }

    void addRange(char a, char b) {
        if (this.count == this.lo.length) {
            this.grow();
        }
        this.lo[this.count] = a;
        this.hi[this.count] = b;
        this.count = this.count + 1;
    }

    private void grow() {
        int n = this.lo.length * 2;
        char[] nl = new char[n];
        char[] nh = new char[n];
        for (int i = 0; i < this.count; i++) {
            nl[i] = this.lo[i];
            nh[i] = this.hi[i];
        }
        this.lo = nl;
        this.hi = nh;
    }

    private boolean inRanges(char c) {
        for (int i = 0; i < this.count; i++) {
            if (c >= this.lo[i] && c <= this.hi[i]) {
                return true;
            }
        }
        return false;
    }

    // Whether `c` falls in the class (before applying `negate`). Under CASE_INSENSITIVE both
    // ASCII cases of `c` are tried, so [a-z] also accepts 'A'.
    boolean inSet(char c) {
        if (this.inRanges(c)) {
            return true;
        }
        if (this.ci) {
            char l = fold(c);
            if (l != c && this.inRanges(l)) {
                return true;
            }
            if (c >= 'a' && c <= 'z' && this.inRanges((char) (c - 32))) {
                return true;
            }
        }
        return false;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        if (i >= m.to) {
            m.hitEnd = true;
            return false;
        }
        if (this.inSet(seq.charAt(i)) != this.negate) {
            return this.next.match(m, i + 1, seq);
        }
        return false;
    }
}

// A greedy quantifier wrapping an atom: min..max repetitions (max == -1 means unbounded).
// Covers *, +, ?, {n}, {n,}, {n,m}. The atom fragment's tail loops back to this node, and a
// Prolog placed in front resets the per-loop counter on fresh entry. The count and the
// iteration-start position live in Matcher scratch slots keyed by `localIndex`.
final class Quantifier extends Node {
    Node atom;
    Node atomTail;
    int min;
    int max;
    int localIndex;
    // Reluctant (lazy) quantifiers (`*?`, `+?`, ...) prefer the continuation over one more
    // repetition; greedy is the default.
    boolean lazy;

    Quantifier() {
        this.atom = null;
        this.atomTail = null;
        this.min = 0;
        this.max = -1;
        this.localIndex = 0;
        this.lazy = false;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        int idx = this.localIndex;
        int count = m.counts[idx];
        boolean canMore = (this.max == -1) || (count < this.max);
        // Guard against a zero-width iteration looping forever: if the previous iteration
        // began at this same position, it consumed nothing, so stop repeating.
        boolean progressed = (count == 0) || (i != m.loopStart[idx]);
        if (this.lazy) {
            // Reluctant: try to stop (hand off to the continuation) before matching more.
            if (count >= this.min && this.next.match(m, i, seq)) {
                return true;
            }
            if (canMore && progressed) {
                m.counts[idx] = count + 1;
                m.loopStart[idx] = i;
                if (this.atom.match(m, i, seq)) {
                    return true;
                }
                m.counts[idx] = count;
            }
            return false;
        }
        if (canMore && progressed) {
            m.counts[idx] = count + 1;
            m.loopStart[idx] = i;
            // Greedy: try to match one more occurrence first (the atom loops back here).
            if (this.atom.match(m, i, seq)) {
                return true;
            }
            m.counts[idx] = count;
        }
        // Otherwise stop, provided we've met the minimum, and hand off to the continuation.
        if (count >= this.min) {
            return this.next.match(m, i, seq);
        }
        return false;
    }
}

// Resets a quantifier's counter, then enters the loop. Placed in front of a Quantifier so
// that re-entering the loop from outside (e.g. an outer quantifier repeating a group that
// contains this one) starts a fresh count, while the atom's loop-back skips it.
final class Prolog extends Node {
    Quantifier loop;

    Prolog(Quantifier loop) {
        this.loop = loop;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        m.counts[this.loop.localIndex] = 0;
        m.loopStart[this.loop.localIndex] = -1;
        return this.loop.match(m, i, seq);
    }
}

// Entry of a capturing group: records the group's start, runs the body, and restores the
// old start if the rest of the pattern fails (so backtracking leaves clean state).
final class GroupHead extends Node {
    int groupIndex;

    GroupHead(int groupIndex) {
        this.groupIndex = groupIndex;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        int save = m.groups[2 * this.groupIndex];
        m.groups[2 * this.groupIndex] = i;
        boolean ok = this.next.match(m, i, seq);
        if (!ok) {
            m.groups[2 * this.groupIndex] = save;
        }
        return ok;
    }
}

// Exit of a capturing group: records the group's end, runs the continuation, and restores
// the old end on failure.
final class GroupTail extends Node {
    int groupIndex;

    GroupTail(int groupIndex) {
        this.groupIndex = groupIndex;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        int save = m.groups[2 * this.groupIndex + 1];
        m.groups[2 * this.groupIndex + 1] = i;
        boolean ok = this.next.match(m, i, seq);
        if (!ok) {
            m.groups[2 * this.groupIndex + 1] = save;
        }
        return ok;
    }
}

// Alternation a|b|c: tries each alternative head in turn. Every alternative's tail is wired
// to the shared BranchConn, whose `next` is what follows the whole alternation.
final class Branch extends Node {
    Node[] atoms;
    int size;
    BranchConn conn;

    Branch() {
        this.atoms = new Node[4];
        this.size = 0;
        this.conn = null;
    }

    void add(Node atomHead) {
        if (this.size == this.atoms.length) {
            int n = this.atoms.length * 2;
            Node[] na = new Node[n];
            for (int i = 0; i < this.size; i++) {
                na[i] = this.atoms[i];
            }
            this.atoms = na;
        }
        this.atoms[this.size] = atomHead;
        this.size = this.size + 1;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        for (int k = 0; k < this.size; k++) {
            if (this.atoms[k].match(m, i, seq)) {
                return true;
            }
        }
        return false;
    }
}

// The join point of an alternation: all alternatives converge here, then flow to `next`.
final class BranchConn extends Node {
    boolean match(Matcher m, int i, CharSequence seq) {
        return this.next.match(m, i, seq);
    }
}

// The '^' anchor. Plain '^' (and '\A', which is the same node with multiline off) matches at
// the start of the region; with MULTILINE it also matches right after a line terminator,
// with the JDK's CRLF rule (no match between the CR and the LF).
final class Begin extends Node {
    boolean multiline;
    boolean unixLines;

    Begin(boolean multiline, boolean unixLines) {
        this.multiline = multiline;
        this.unixLines = unixLines;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        int startIndex = m.anchorStart();
        if (!this.multiline) {
            // '^' without MULTILINE, and '\A': only the very start of the region.
            if (i != startIndex) {
                return false;
            }
            return this.next.match(m, i, seq);
        }
        int endIndex = m.anchorLimit(seq);
        // Perl (and therefore the JDK) never matches '^' at the end of input, not even
        // right after a line terminator.
        if (i >= endIndex) {
            m.hitEnd = true;
            return false;
        }
        if (i > startIndex) {
            char ch = seq.charAt(i - 1);
            if (!isLineTerm(ch, this.unixLines)) {
                return false;
            }
            // Never match between the CR and the LF of a CRLF pair.
            if (ch == '\r' && seq.charAt(i) == '\n') {
                return false;
            }
        }
        return this.next.match(m, i, seq);
    }
}

// The '$' anchor. Without MULTILINE it matches only at the end of the region, optionally
// before a single trailing line terminator (which is also exactly '\Z'); with MULTILINE it
// matches before any line terminator. Matching at the very end sets requireEnd, because more
// input could turn the match into a failure — that is what Matcher.requireEnd() reports.
final class End extends Node {
    boolean multiline;
    boolean unixLines;

    End(boolean multiline, boolean unixLines) {
        this.multiline = multiline;
        this.unixLines = unixLines;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        int endIndex = m.anchorLimit(seq);
        if (!this.multiline) {
            if (i < endIndex - 2) {
                return false;
            }
            if (i == endIndex - 2) {
                if (seq.charAt(i) != '\r' || seq.charAt(i + 1) != '\n') {
                    return false;
                }
            }
        }
        if (i < endIndex) {
            char ch = seq.charAt(i);
            if (ch == '\n') {
                if (i > 0 && seq.charAt(i - 1) == '\r') {
                    return false;
                }
                if (this.multiline) {
                    return this.next.match(m, i, seq);
                }
            } else if (isLineTerm(ch, this.unixLines)) {
                if (this.multiline) {
                    return this.next.match(m, i, seq);
                }
            } else {
                return false;
            }
        }
        m.hitEnd = true;
        m.requireEnd = true;
        return this.next.match(m, i, seq);
    }
}

// '\z' — the very end of the region, with no trailing-terminator allowance.
final class EndInput extends Node {
    boolean match(Matcher m, int i, CharSequence seq) {
        int endIndex = m.anchorLimit(seq);
        if (i != endIndex) {
            return false;
        }
        m.hitEnd = true;
        m.requireEnd = true;
        return this.next.match(m, i, seq);
    }
}

// '\b' (word boundary) and '\B' (its negation). A boundary sits between a word character and
// a non-word character; positions outside the region count as non-word (opaque bounds — we
// do not offer transparent bounds, see Matcher.useTransparentBounds).
final class WordBoundary extends Node {
    boolean negate;

    WordBoundary(boolean negate) {
        this.negate = negate;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        int startIndex = m.anchorStart();
        int endIndex = m.anchorLimit(seq);
        boolean before = i > startIndex && isWordChar(seq.charAt(i - 1));
        boolean after = i < endIndex && isWordChar(seq.charAt(i));
        if (i >= endIndex) {
            m.hitEnd = true;
        }
        if ((before != after) != this.negate) {
            return this.next.match(m, i, seq);
        }
        return false;
    }
}

// Terminal of a lookahead sub-expression: succeeds without consuming input or continuing the
// outer chain (a lookahead is a zero-width assertion).
final class LookAccept extends Node {
    boolean match(Matcher m, int i, CharSequence seq) {
        return true;
    }
}

// A zero-width lookahead assertion (?=X) (positive) or (?!X) (negative): runs the sub-
// expression X at the current position; on the expected outcome it continues the outer chain
// at the SAME position, consuming nothing. Lookbehind is not supported — the parser rejects
// it rather than mis-parsing it.
final class Lookahead extends Node {
    Node cond;
    boolean negate;

    boolean match(Matcher m, int i, CharSequence seq) {
        boolean ok = this.cond.match(m, i, seq);
        if (ok != this.negate) {
            return this.next.match(m, i, seq);
        }
        return false;
    }
}

// A backreference \n (or \k<name>): matches the exact run of text that a prior capturing
// group matched. A reference to a group that never participated matches the empty string (as
// real javac does).
final class BackRefNode extends Node {
    int groupIndex;
    boolean ci;

    BackRefNode(int groupIndex, boolean ci) {
        this.groupIndex = groupIndex;
        this.ci = ci;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        int gs = m.groups[2 * this.groupIndex];
        int ge = m.groups[2 * this.groupIndex + 1];
        if (gs < 0 || ge < 0) {
            return this.next.match(m, i, seq);
        }
        int gl = ge - gs;
        if (i + gl > m.to) {
            m.hitEnd = true;
            return false;
        }
        for (int k = 0; k < gl; k++) {
            if (!sameChar(seq.charAt(i + k), seq.charAt(gs + k), this.ci)) {
                return false;
            }
        }
        return this.next.match(m, i + gl, seq);
    }
}

// --- A parse fragment: the head and tail of a linked node chain -----------------------

final class Frag {
    Node head;
    Node tail;

    Frag(Node head, Node tail) {
        this.head = head;
        this.tail = tail;
    }
}

// --- The parser: pattern string -> node tree ------------------------------------------

// Recursive-descent parser for the grammar:
//   expr   := seq ('|' seq)*
//   seq    := factor*
//   factor := atom quantifier?
//   atom   := '(' expr ')' | '[' class ']' | '.' | '^' | '$' | '\' escape | literal
// Capturing groups are numbered as their '(' is seen (group 0 is the whole match, implicit).
// Each quantifier is assigned a scratch-slot index so Matcher can size its per-loop arrays.
//
// Flags reach the parser through the constructor and are baked into the nodes it emits.
// LITERAL and \Q..\E are handled by a pre-pass (expandQuotes) that rewrites the quoted runs
// into ordinary backslash escapes, so the grammar above never has to know about them.
final class RegexParser {
    private final String src;
    private final int len;
    private final int flags;
    private final boolean ci;
    private final boolean comments;
    private int pos;
    private int groups;
    private int loops;
    private Map<String, Integer> named;

    RegexParser(String pattern) {
        this(pattern, 0);
    }

    RegexParser(String pattern, int flags) {
        this.flags = flags;
        String p = pattern;
        if ((flags & Pattern.LITERAL) != 0) {
            p = literalize(pattern);
        } else {
            p = expandQuotes(pattern);
        }
        this.src = p;
        this.len = p.length();
        this.ci = (flags & Pattern.CASE_INSENSITIVE) != 0;
        this.comments = (flags & Pattern.COMMENTS) != 0 && (flags & Pattern.LITERAL) == 0;
        this.pos = 0;
        this.groups = 0;
        this.loops = 0;
        this.named = null;
    }

    int groupCount() {
        return this.groups;
    }

    int localCount() {
        return this.loops;
    }

    // The name -> group-number map, or null when the pattern declares no named groups.
    Map<String, Integer> namedGroups() {
        return this.named;
    }

    private boolean dotall() {
        return (this.flags & Pattern.DOTALL) != 0;
    }

    private boolean multiline() {
        return (this.flags & Pattern.MULTILINE) != 0;
    }

    private boolean unixLines() {
        return (this.flags & Pattern.UNIX_LINES) != 0;
    }

    // --- \Q..\E and LITERAL: rewrite quoted runs into ordinary escapes ----------------

    // Escapes every non-alphanumeric character, which is exactly "treat the whole string as
    // literal text".
    private static String literalize(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!isAlnum(c)) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isAlnum(char c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        return c >= '0' && c <= '9';
    }

    // Rewrites `\Q ... \E` runs into per-character escapes. Doing it as a pre-pass keeps the
    // grammar free of quoting state; the cost is that PatternSyntaxException offsets refer to
    // the rewritten string, which we accept (they are already approximate).
    //
    // Pattern.quote() emits `\Q`, breaks any embedded `\E` as `\E\\E\Q`, and closes with
    // `\E`; this pre-pass is what makes that round-trip actually work.
    private static String expandQuotes(String s) {
        int n = s.length();
        int i = 0;
        boolean quoting = false;
        StringBuilder sb = new StringBuilder();
        while (i < n) {
            char c = s.charAt(i);
            if (!quoting) {
                if (c == '\\' && i + 1 < n) {
                    char d = s.charAt(i + 1);
                    if (d == 'Q') {
                        quoting = true;
                        i = i + 2;
                        continue;
                    }
                    if (d == 'E') {
                        // A stray \E outside a quote is a no-op, as in the JDK.
                        i = i + 2;
                        continue;
                    }
                    sb.append(c);
                    sb.append(d);
                    i = i + 2;
                    continue;
                }
                sb.append(c);
                i = i + 1;
            } else {
                if (c == '\\' && i + 1 < n && s.charAt(i + 1) == 'E') {
                    quoting = false;
                    i = i + 2;
                    continue;
                }
                if (!isAlnum(c)) {
                    sb.append('\\');
                }
                sb.append(c);
                i = i + 1;
            }
        }
        return sb.toString();
    }

    // --- the grammar ------------------------------------------------------------------

    // Parses the whole pattern into a chain terminated by a LastNode; returns the head.
    Node parse() {
        Frag body = this.expr();
        LastNode last = new LastNode();
        if (this.pos < this.len) {
            // Only ')' can stop expr() early; a leftover one is unbalanced.
            throw this.error("Unmatched closing ')'");
        }
        if (body.head == null) {
            return last;
        }
        body.tail.next = last;
        return body.head;
    }

    // expr := seq ('|' seq)*
    private Frag expr() {
        Frag first = this.seq();
        if (this.pos >= this.len || this.peek() != '|') {
            return first;
        }
        Branch branch = new Branch();
        BranchConn conn = new BranchConn();
        branch.conn = conn;
        this.addAlt(branch, conn, first);
        while (this.pos < this.len && this.peek() == '|') {
            this.pos = this.pos + 1;
            Frag alt = this.seq();
            this.addAlt(branch, conn, alt);
        }
        return new Frag(branch, conn);
    }

    private void addAlt(Branch branch, BranchConn conn, Frag alt) {
        if (alt.head == null) {
            // An empty alternative (e.g. "a|") matches the empty string: flow straight to
            // the join point.
            branch.add(conn);
        } else {
            alt.tail.next = conn;
            branch.add(alt.head);
        }
    }

    // Under COMMENTS, whitespace in the pattern is ignored and '#' runs to end of line.
    private void skipComments() {
        if (!this.comments) {
            return;
        }
        boolean moved = true;
        while (moved && this.pos < this.len) {
            moved = false;
            char c = this.peek();
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == 0x0b) {
                this.pos = this.pos + 1;
                moved = true;
            } else if (c == '#') {
                while (this.pos < this.len && this.peek() != '\n') {
                    this.pos = this.pos + 1;
                }
                moved = true;
            }
        }
    }

    // seq := factor*  (stops at '|' or ')', or end of input)
    private Frag seq() {
        Node head = null;
        Node tail = null;
        this.skipComments();
        while (this.pos < this.len) {
            char c = this.peek();
            if (c == '|' || c == ')') {
                break;
            }
            Frag f = this.factor();
            if (head == null) {
                head = f.head;
                tail = f.tail;
            } else {
                tail.next = f.head;
                tail = f.tail;
            }
            this.skipComments();
        }
        return new Frag(head, tail);
    }

    // factor := atom quantifier?
    private Frag factor() {
        Frag atom = this.atom();
        this.skipComments();
        if (this.pos < this.len) {
            char c = this.peek();
            if (c == '*' || c == '+' || c == '?' || c == '{') {
                return this.quantifier(atom);
            }
        }
        return atom;
    }

    // Wraps an already-parsed atom fragment in a greedy quantifier: a Prolog (fresh-count
    // reset) in front, the Quantifier as the loop target, and the atom's tail looping back.
    private Frag quantifier(Frag atom) {
        if (atom.tail == null) {
            throw this.error("Dangling meta character");
        }
        char c = this.next();
        Quantifier q = new Quantifier();
        q.atom = atom.head;
        q.atomTail = atom.tail;
        q.localIndex = this.loops;
        this.loops = this.loops + 1;
        if (c == '*') {
            q.min = 0;
            q.max = -1;
        } else if (c == '+') {
            q.min = 1;
            q.max = -1;
        } else if (c == '?') {
            q.min = 0;
            q.max = 1;
        } else {
            // '{' n [',' [m]] '}'
            int n = this.readInt();
            int mx;
            if (this.pos < this.len && this.peek() == ',') {
                this.next();
                if (this.pos < this.len && this.peek() == '}') {
                    mx = -1;
                } else {
                    mx = this.readInt();
                }
            } else {
                mx = n;
            }
            this.expect('}');
            if (mx != -1 && mx < n) {
                throw this.error("Illegal repetition range");
            }
            q.min = n;
            q.max = mx;
        }
        // A trailing '?' makes the quantifier reluctant. A trailing '+' would make it
        // possessive; we have no cut/atomic node, so say so instead of silently treating
        // '+' as another quantifier over the loop (which is what a naive parse would do).
        if (this.pos < this.len && this.peek() == '?') {
            q.lazy = true;
            this.next();
        } else if (this.pos < this.len && this.peek() == '+') {
            throw this.error("Possessive quantifiers are not supported");
        }
        q.atomTail.next = q;
        Prolog p = new Prolog(q);
        return new Frag(p, q);
    }

    private Frag atom() {
        char c = this.peek();
        if (c == '(') {
            return this.group();
        }
        if (c == '[') {
            return this.charClass();
        }
        if (c == '.') {
            this.next();
            return single(new AnyNode(this.dotall(), this.unixLines()));
        }
        if (c == '^') {
            this.next();
            return single(new Begin(this.multiline(), this.unixLines()));
        }
        if (c == '$') {
            this.next();
            return single(new End(this.multiline(), this.unixLines()));
        }
        if (c == '\\') {
            return this.escape();
        }
        if (c == '*' || c == '+' || c == '?' || c == '{') {
            throw this.error("Dangling meta character");
        }
        this.next();
        return single(new CharNode(c, this.ci));
    }

    // '(' expr ')'  — a capturing group, or a '(?...)' special group: '(?:...)' non-capturing,
    // '(?=...)' / '(?!...)' lookahead, and '(?<name>...)' named capturing. Lookbehind,
    // atomic groups and inline flag groups are rejected explicitly.
    private Frag group() {
        this.next();
        if (this.pos < this.len && this.peek() == '?') {
            this.next();
            char kind = this.next();
            if (kind == ':') {
                Frag inner = this.expr();
                this.expect(')');
                if (inner.head == null) {
                    BranchConn pass = new BranchConn();
                    return new Frag(pass, pass);
                }
                return inner;
            }
            if (kind == '=' || kind == '!') {
                return this.lookahead(kind == '!');
            }
            if (kind == '<') {
                if (this.pos < this.len && (this.peek() == '=' || this.peek() == '!')) {
                    throw this.error("Lookbehind is not supported");
                }
                String name = this.readGroupName();
                this.expect('>');
                return this.capturingGroup(name);
            }
            if (kind == '>') {
                throw this.error("Atomic groups are not supported");
            }
            throw this.error("Unsupported group construct");
        }
        return this.capturingGroup(null);
    }

    private Frag lookahead(boolean negate) {
        Frag inner = this.expr();
        this.expect(')');
        LookAccept acc = new LookAccept();
        Lookahead la = new Lookahead();
        la.negate = negate;
        if (inner.head == null) {
            la.cond = acc;
        } else {
            la.cond = inner.head;
            inner.tail.next = acc;
        }
        return single(la);
    }

    // A capturing group, optionally carrying a name. The number is assigned when '(' is seen,
    // so nested groups number outside-in exactly as in the JDK.
    private Frag capturingGroup(String name) {
        this.groups = this.groups + 1;
        int idx = this.groups;
        if (name != null) {
            if (this.named == null) {
                this.named = new HashMap<String, Integer>();
            }
            if (this.named.containsKey(name)) {
                throw this.error("Named capturing group is already defined");
            }
            this.named.put(name, Integer.valueOf(idx));
        }
        GroupHead gh = new GroupHead(idx);
        GroupTail gt = new GroupTail(idx);
        Frag inner = this.expr();
        this.expect(')');
        if (inner.head == null) {
            gh.next = gt;
        } else {
            gh.next = inner.head;
            inner.tail.next = gt;
        }
        return new Frag(gh, gt);
    }

    // A group name: a letter followed by letters and digits (the JDK's rule).
    private String readGroupName() {
        StringBuilder sb = new StringBuilder();
        if (this.pos >= this.len) {
            throw this.error("Unclosed group name");
        }
        char c = this.peek();
        boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        if (!letter) {
            throw this.error("capturing group name does not start with a Latin letter");
        }
        while (this.pos < this.len && isAlnum(this.peek())) {
            sb.append(this.next());
        }
        return sb.toString();
    }

    // '[' ['^'] members ']'  — a character class.
    private Frag charClass() {
        this.next();
        CharClassNode cc = new CharClassNode();
        cc.ci = this.ci;
        if (this.pos < this.len && this.peek() == '^') {
            cc.negate = true;
            this.next();
        }
        boolean first = true;
        while (this.pos < this.len && (this.peek() != ']' || first)) {
            first = false;
            // Union/intersection of nested classes ([a-z&&[^bc]], [a[bc]]) needs a class
            // node that can hold sub-classes; ours holds a flat range list. Reject rather
            // than treat '&' and '[' as ordinary members, which is what a flat parse does.
            if (this.peek() == '&' && this.pos + 1 < this.len && this.charAt(this.pos + 1) == '&') {
                throw this.error("Character class intersection is not supported");
            }
            if (this.peek() == '[') {
                throw this.error("Nested character classes are not supported");
            }
            char c = this.next();
            if (c == '\\') {
                char e = this.next();
                this.addClassEscape(cc, e);
                continue;
            }
            // A range a-b, unless the '-' is the last member before ']'.
            if (this.pos + 1 < this.len && this.peek() == '-' && this.charAt(this.pos + 1) != ']') {
                this.next();
                char hi = this.next();
                if (hi == '\\') {
                    hi = this.classEscapeChar(this.next());
                }
                if (hi < c) {
                    throw this.error("Illegal character range");
                }
                cc.addRange(c, hi);
            } else {
                cc.addRange(c, c);
            }
        }
        this.expect(']');
        return single(cc);
    }

    // A predefined class inside [...]: \d \w \s add their ranges. The negated forms
    // (\D \W \S inside a class) would need a nested-class node, which we do not have; they
    // are rejected rather than silently added as their positive counterpart.
    private void addClassEscape(CharClassNode cc, char e) {
        if (e == 'd') {
            cc.addRange('0', '9');
        } else if (e == 'w') {
            cc.addRange('0', '9');
            cc.addRange('A', 'Z');
            cc.addRange('a', 'z');
            cc.addRange('_', '_');
        } else if (e == 's') {
            this.addWhitespace(cc);
        } else if (e == 'D' || e == 'W' || e == 'S') {
            throw this.error("Negated predefined classes inside a character class are not supported");
        } else {
            char c = this.classEscapeChar(e);
            cc.addRange(c, c);
        }
    }

    // The single character an escape denotes, for the endpoints of a class range and for the
    // plain-literal case inside a class.
    private char classEscapeChar(char e) {
        if (e == 'n') {
            return '\n';
        }
        if (e == 't') {
            return '\t';
        }
        if (e == 'r') {
            return '\r';
        }
        if (e == 'f') {
            return '\f';
        }
        if (e == 'a') {
            return (char) 7;
        }
        if (e == 'e') {
            return (char) 27;
        }
        if (e == '0') {
            return this.readOctal();
        }
        if (e == 'x') {
            return this.readHex(2);
        }
        if (e == 'u') {
            return this.readHex(4);
        }
        if (e == 'c') {
            return this.readControl();
        }
        if (isAlnum(e)) {
            throw this.error("Illegal/unsupported escape sequence");
        }
        return e;
    }

    private void addWhitespace(CharClassNode cc) {
        cc.addRange(' ', ' ');
        cc.addRange('\t', '\t');
        cc.addRange('\n', '\n');
        cc.addRange((char) 0x0b, (char) 0x0b);
        cc.addRange('\r', '\r');
        cc.addRange('\f', '\f');
    }

    // '\' escape outside a class.
    private Frag escape() {
        this.next();
        if (this.pos >= this.len) {
            throw this.error("Trailing backslash");
        }
        char e = this.next();
        if (e >= '1' && e <= '9') {
            // \1..\9 — a backreference to an already-declared capturing group. Digits are read
            // greedily but capped at the groups seen so far (as real javac does).
            int num = e - '0';
            if (num > this.groups) {
                throw this.error("No group to reference");
            }
            while (this.pos < this.len && this.peek() >= '0' && this.peek() <= '9') {
                int nn = num * 10 + (this.peek() - '0');
                if (nn > this.groups) {
                    break;
                }
                num = nn;
                this.next();
            }
            return single(new BackRefNode(num, this.ci));
        }
        if (e == 'd' || e == 'D' || e == 'w' || e == 'W' || e == 's' || e == 'S') {
            CharClassNode cc = new CharClassNode();
            cc.ci = this.ci;
            this.fillPredef(cc, e);
            return single(cc);
        }
        if (e == 'b') {
            return single(new WordBoundary(false));
        }
        if (e == 'B') {
            return single(new WordBoundary(true));
        }
        if (e == 'A') {
            return single(new Begin(false, this.unixLines()));
        }
        if (e == 'z') {
            return single(new EndInput());
        }
        if (e == 'Z') {
            // '\Z' is '$' with MULTILINE forced off: end of region, or just before a single
            // trailing line terminator.
            return single(new End(false, this.unixLines()));
        }
        if (e == 'k') {
            this.expect('<');
            String name = this.readGroupName();
            this.expect('>');
            if (this.named == null || !this.named.containsKey(name)) {
                throw this.error("No group with that name to reference");
            }
            return single(new BackRefNode(this.named.get(name).intValue(), this.ci));
        }
        // Constructs the JDK has but this engine does not. Naming them beats the old behavior
        // of turning '\p' into the literal 'p'.
        if (e == 'p' || e == 'P' || e == 'G' || e == 'R' || e == 'h' || e == 'H'
                || e == 'v' || e == 'V' || e == 'N' || e == 'X') {
            throw this.error("Unsupported escape sequence");
        }
        return single(new CharNode(this.classEscapeChar(e), this.ci));
    }

    // Builds a predefined class node; the uppercase forms negate the positive set.
    private void fillPredef(CharClassNode cc, char e) {
        if (e == 'd' || e == 'D') {
            cc.addRange('0', '9');
        } else if (e == 'w' || e == 'W') {
            cc.addRange('0', '9');
            cc.addRange('A', 'Z');
            cc.addRange('a', 'z');
            cc.addRange('_', '_');
        } else {
            this.addWhitespace(cc);
        }
        if (e == 'D' || e == 'W' || e == 'S') {
            cc.negate = true;
        }
    }

    private static Frag single(Node n) {
        return new Frag(n, n);
    }

    // --- character-stream helpers ---

    private char peek() {
        return this.src.charAt(this.pos);
    }

    private char charAt(int i) {
        return this.src.charAt(i);
    }

    private char next() {
        if (this.pos >= this.len) {
            throw this.error("Unexpected end of pattern");
        }
        char c = this.src.charAt(this.pos);
        this.pos = this.pos + 1;
        return c;
    }

    private void expect(char c) {
        if (this.pos >= this.len || this.src.charAt(this.pos) != c) {
            throw this.error("Expected a different character");
        }
        this.pos = this.pos + 1;
    }

    private int readInt() {
        int v = 0;
        boolean any = false;
        while (this.pos < this.len && this.peek() >= '0' && this.peek() <= '9') {
            v = v * 10 + (this.next() - '0');
            any = true;
        }
        if (!any) {
            throw this.error("Expected a number");
        }
        return v;
    }

    // '\0' followed by one to three octal digits.
    private char readOctal() {
        int v = 0;
        int n = 0;
        while (n < 3 && this.pos < this.len && this.peek() >= '0' && this.peek() <= '7') {
            v = v * 8 + (this.next() - '0');
            n = n + 1;
        }
        if (n == 0) {
            throw this.error("Illegal octal escape sequence");
        }
        return (char) v;
    }

    private char readHex(int digits) {
        int v = 0;
        for (int i = 0; i < digits; i++) {
            if (this.pos >= this.len) {
                throw this.error("Illegal hexadecimal escape sequence");
            }
            int d = hexDigit(this.next());
            if (d < 0) {
                throw this.error("Illegal hexadecimal escape sequence");
            }
            v = v * 16 + d;
        }
        return (char) v;
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    // '\cX' — the control character corresponding to X.
    private char readControl() {
        if (this.pos >= this.len) {
            throw this.error("Illegal control escape sequence");
        }
        char c = this.next();
        return (char) (c ^ 64);
    }

    private PatternSyntaxException error(String msg) {
        return new PatternSyntaxException(msg, this.src, this.pos);
    }
}
