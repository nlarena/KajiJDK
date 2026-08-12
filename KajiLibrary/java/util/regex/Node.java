package java.util.regex;

// KajiLibrary's regex node tree + parser (all package-private — real javac keeps these
// nested inside Pattern; we make them top-level in the package to sidestep the compiler's
// generic-enclosing-capture limitation, and the API gate skips them since the JDK has no
// java.util.regex.Node counterpart).
//
// The engine is an NFA walked by recursive backtracking in continuation-passing style: a
// Node matches at a position and, on success, delegates to its `next` (the continuation).
// Backtracking falls out of the recursion — a node that fails returns false, and the caller
// tries its next alternative. RegexParser turns a pattern string into the linked tree.

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
}

// Terminal accept node: the end of the pattern. Records the overall match end (group 0's
// end). During matches() the accept is only valid at the region end.
final class LastNode extends Node {
    boolean match(Matcher m, int i, CharSequence seq) {
        if (m.requireEnd && i != m.to) {
            return false;
        }
        m.groups[1] = i;
        return true;
    }
}

// A single literal character.
final class CharNode extends Node {
    char ch;

    CharNode(char ch) {
        this.ch = ch;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        if (i < m.to && seq.charAt(i) == this.ch) {
            return this.next.match(m, i + 1, seq);
        }
        return false;
    }
}

// The '.' metacharacter: any character except a line terminator. Default JDK behavior; the
// DOTALL flag (making '.' match everything) and the full Unicode line-terminator set
// (/ / ) are H5-T5.
final class AnyNode extends Node {
    boolean match(Matcher m, int i, CharSequence seq) {
        if (i < m.to) {
            char c = seq.charAt(i);
            if (c != '\n' && c != '\r') {
                return this.next.match(m, i + 1, seq);
            }
        }
        return false;
    }
}

// A character class [...]: a set of ranges (a single character is a range a-a), optionally
// negated. Ranges grow on demand the way the rest of KajiLibrary grows its backing arrays.
final class CharClassNode extends Node {
    boolean negate;
    char[] lo;
    char[] hi;
    int count;

    CharClassNode() {
        this.negate = false;
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

    // Whether `c` falls in the class (before applying `negate`).
    boolean inSet(char c) {
        for (int i = 0; i < this.count; i++) {
            if (c >= this.lo[i] && c <= this.hi[i]) {
                return true;
            }
        }
        return false;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        if (i < m.to) {
            boolean in = this.inSet(seq.charAt(i));
            if (in != this.negate) {
                return this.next.match(m, i + 1, seq);
            }
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

// The '^' anchor: matches at the start of input (multiline handling is H5-T5).
final class Begin extends Node {
    boolean match(Matcher m, int i, CharSequence seq) {
        if (i == 0) {
            return this.next.match(m, i, seq);
        }
        return false;
    }
}

// The '$' anchor: matches at the end of input (multiline handling is H5-T5).
final class End extends Node {
    boolean match(Matcher m, int i, CharSequence seq) {
        if (i == m.to) {
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
// at the SAME position, consuming nothing. Lookbehind is H5-T5.
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

// A backreference \n: matches the exact run of text that a prior capturing group matched. A
// reference to a group that never participated matches the empty string (as real javac does).
final class BackRefNode extends Node {
    int groupIndex;

    BackRefNode(int groupIndex) {
        this.groupIndex = groupIndex;
    }

    boolean match(Matcher m, int i, CharSequence seq) {
        int gs = m.groups[2 * this.groupIndex];
        int ge = m.groups[2 * this.groupIndex + 1];
        if (gs < 0 || ge < 0) {
            return this.next.match(m, i, seq);
        }
        int gl = ge - gs;
        if (i + gl > m.to) {
            return false;
        }
        for (int k = 0; k < gl; k++) {
            if (seq.charAt(i + k) != seq.charAt(gs + k)) {
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

// Recursive-descent parser for the H5-T2 grammar:
//   expr   := seq ('|' seq)*
//   seq    := factor*
//   factor := atom quantifier?
//   atom   := '(' expr ')' | '[' class ']' | '.' | '^' | '$' | '\' escape | literal
// Capturing groups are numbered as their '(' is seen (group 0 is the whole match, implicit).
// Each quantifier is assigned a scratch-slot index so Matcher can size its per-loop arrays.
final class RegexParser {
    private final String src;
    private final int len;
    private int pos;
    private int groups;
    private int loops;

    RegexParser(String pattern) {
        this.src = pattern;
        this.len = pattern.length();
        this.pos = 0;
        this.groups = 0;
        this.loops = 0;
    }

    int groupCount() {
        return this.groups;
    }

    int localCount() {
        return this.loops;
    }

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

    // seq := factor*  (stops at '|' or ')', or end of input)
    private Frag seq() {
        Node head = null;
        Node tail = null;
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
        }
        return new Frag(head, tail);
    }

    // factor := atom quantifier?
    private Frag factor() {
        Frag atom = this.atom();
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
            q.min = n;
            q.max = mx;
        }
        // A trailing '?' makes the quantifier reluctant (possessive '+' is H5-T5).
        if (this.pos < this.len && this.peek() == '?') {
            q.lazy = true;
            this.next();
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
            return single(new AnyNode());
        }
        if (c == '^') {
            this.next();
            return single(new Begin());
        }
        if (c == '$') {
            this.next();
            return single(new End());
        }
        if (c == '\\') {
            return this.escape();
        }
        if (c == '*' || c == '+' || c == '?' || c == '{') {
            throw this.error("Dangling meta character");
        }
        this.next();
        return single(new CharNode(c));
    }

    // '(' expr ')'  — a capturing group, or a '(?...)' special group: '(?:...)' non-capturing
    // and '(?=...)' / '(?!...)' lookahead. Named groups '(?<name>...)' and lookbehind are H5-T5.
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
                Frag inner = this.expr();
                this.expect(')');
                LookAccept acc = new LookAccept();
                Lookahead la = new Lookahead();
                la.negate = (kind == '!');
                if (inner.head == null) {
                    la.cond = acc;
                } else {
                    la.cond = inner.head;
                    inner.tail.next = acc;
                }
                return single(la);
            }
            throw this.error("Unsupported group construct");
        }
        this.groups = this.groups + 1;
        int idx = this.groups;
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

    // '[' ['^'] members ']'  — a character class.
    private Frag charClass() {
        this.next();
        CharClassNode cc = new CharClassNode();
        if (this.pos < this.len && this.peek() == '^') {
            cc.negate = true;
            this.next();
        }
        boolean first = true;
        while (this.pos < this.len && (this.peek() != ']' || first)) {
            first = false;
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
                    hi = this.next();
                }
                cc.addRange(c, hi);
            } else {
                cc.addRange(c, c);
            }
        }
        this.expect(']');
        return single(cc);
    }

    // A predefined class inside [...]: \d \w \s add their ranges (the negated \D \W \S
    // inside a class are an H5-T5 refinement).
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
        } else if (e == 'n') {
            cc.addRange('\n', '\n');
        } else if (e == 't') {
            cc.addRange('\t', '\t');
        } else if (e == 'r') {
            cc.addRange('\r', '\r');
        } else {
            cc.addRange(e, e);
        }
    }

    private void addWhitespace(CharClassNode cc) {
        cc.addRange(' ', ' ');
        cc.addRange('\t', '\t');
        cc.addRange('\n', '\n');
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
            return single(new BackRefNode(num));
        }
        if (e == 'd' || e == 'D' || e == 'w' || e == 'W' || e == 's' || e == 'S') {
            CharClassNode cc = new CharClassNode();
            this.fillPredef(cc, e);
            return single(cc);
        }
        if (e == 'n') {
            return single(new CharNode('\n'));
        }
        if (e == 't') {
            return single(new CharNode('\t'));
        }
        if (e == 'r') {
            return single(new CharNode('\r'));
        }
        // An escaped metacharacter (\. \* \\ ...) or any other escaped literal.
        return single(new CharNode(e));
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

    private PatternSyntaxException error(String msg) {
        return new PatternSyntaxException(msg, this.src, this.pos);
    }
}
