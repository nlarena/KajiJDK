package javax.print.attribute;

import java.io.Serializable;

// The syntax class of the attributes whose value is a **set of integers**, kept as a list of
// ranges.
//
// It is the package's class with real logic, and all the logic is in a single idea: the **canonical
// form**. Any list of ranges is accepted --unordered, overlapping, with empty ranges-- and inside
// the same representation is always kept: ordered from lower to higher, without empty ranges, and
// with the ones that touch or overlap merged into one. Two ranges are "adjacent" if the upper one
// starts right after the lower one (`ub + 1 == lb`), and in that case they are merged too.
//
// Canonicalizing early is what makes `equals` and `hashCode` cheap and correct: `"1-3,4-6"` and
// `"1-6"` describe the same set and have to come out equal, and after canonicalization they are,
// component by component, without comparing sets.
//
// Two details inherited from the JDK that are replicated as they are because they are observable:
//  - the text form does **not** validate an integer's range: `"2147483648"` wraps around to
//    -2147483648 instead of failing, because the digits are accumulated with `int` arithmetic;
//  - the `int[][]` form does reject negatives, but only in **non-empty** ranges: `{{5,3}}` is an
//    empty range and is discarded before its sign is looked at.
//
// And one divergence, only one, that shows up only when the first of those two oddities already
// produced a negative bound: it is explained where it lives, in `canonicalArrayForm`.
public abstract class SetOfIntegerSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = 3666874174847632203L;

    // Always in canonical form. Each row is {lb, ub}, both inclusive.
    private int[][] members;

    // The states of the text-form recognizer. The grammar is ranges = <empty> | range ("," range)*
    //     range = int | int ("-" | ":") int with spaces allowed between tokens but not inside an
    //     integer. Seven states are needed and not fewer: "after the lower bound" has to accept the
    //     dash and "after the upper" does not, and "just started" has to accept the end of the
    //     string while "just saw a comma" does not -- that is why `"1,"` is an error and `" "` is
    //     the empty set.
    private static final int ST_START = 0;
    private static final int ST_IN_LB = 1;
    private static final int ST_AFTER_LB = 2;
    private static final int ST_BEFORE_UB = 3;
    private static final int ST_IN_UB = 4;
    private static final int ST_AFTER_UB = 5;
    private static final int ST_AFTER_COMMA = 6;

    protected SetOfIntegerSyntax(String members) {
        this.members = parse(members);
    }

    protected SetOfIntegerSyntax(int[][] members) {
        this.members = parse(members);
    }

    // A single integer: the set {member}.
    protected SetOfIntegerSyntax(int member) {
        if (member < 0) {
            throw new IllegalArgumentException();
        }
        this.members = new int[][] {{member, member}};
    }

    // A range. If `lowerBound > upperBound` the range is empty and the set is left empty -- and in
    // that case the sign is not even looked at, which is why `new X(-1, -5)` does not fail and `new
    // X(-1, 5)` does.
    protected SetOfIntegerSyntax(int lowerBound, int upperBound) {
        if (lowerBound <= upperBound) {
            if (lowerBound < 0) {
                throw new IllegalArgumentException();
            }
            this.members = new int[][] {{lowerBound, upperBound}};
        } else {
            this.members = new int[0][];
        }
    }

    // The recognizer's two character classifications. They go through `Character` and not through a
    // hand-written ASCII range because the JDK uses `Character.isWhitespace` and
    // `Character.digit(c, 10)`, and the difference is observable: `digit` accepts the decimal
    // digits of any script --the Arabic-Indic U+0660..U+0669, for example-- and `isWhitespace`
    // accepts the Unicode separators and rejects the no-break space U+00A0.
    private static boolean isSpace(char c) {
        return Character.isWhitespace(c);
    }

    private static int digitOf(char c) {
        return Character.digit(c, 10);
    }

    // The recognizer. It returns the canonical form; `null` is the empty set, not an error.
    private static int[][] parse(String members) {
        int[][] raw = new int[8][];
        int count = 0;
        int n = (members == null) ? 0 : members.length();
        int state = ST_START;
        int lb = 0;
        int ub = 0;
        int i = 0;
        while (i < n) {
            char c = members.charAt(i);
            i++;
            int d = digitOf(c);
            if (state == ST_START || state == ST_AFTER_COMMA) {
                if (isSpace(c)) {
                    continue;
                }
                if (d < 0) {
                    throw new IllegalArgumentException();
                }
                lb = d;
                state = ST_IN_LB;
            } else if (state == ST_IN_LB) {
                if (d >= 0) {
                    // No overflow check, on purpose: it is what the JDK does.
                    lb = lb * 10 + d;
                } else if (isSpace(c)) {
                    state = ST_AFTER_LB;
                } else if (c == '-' || c == ':') {
                    state = ST_BEFORE_UB;
                } else if (c == ',') {
                    raw = push(raw, count, lb, lb);
                    count++;
                    state = ST_AFTER_COMMA;
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (state == ST_AFTER_LB) {
                if (isSpace(c)) {
                    continue;
                }
                if (c == '-' || c == ':') {
                    state = ST_BEFORE_UB;
                } else if (c == ',') {
                    raw = push(raw, count, lb, lb);
                    count++;
                    state = ST_AFTER_COMMA;
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (state == ST_BEFORE_UB) {
                if (isSpace(c)) {
                    continue;
                }
                if (d < 0) {
                    throw new IllegalArgumentException();
                }
                ub = d;
                state = ST_IN_UB;
            } else if (state == ST_IN_UB) {
                if (d >= 0) {
                    ub = ub * 10 + d;
                } else if (isSpace(c)) {
                    state = ST_AFTER_UB;
                } else if (c == ',') {
                    raw = push(raw, count, lb, ub);
                    count++;
                    state = ST_AFTER_COMMA;
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                // ST_TRAS_UB
                if (isSpace(c)) {
                    continue;
                }
                if (c == ',') {
                    raw = push(raw, count, lb, ub);
                    count++;
                    state = ST_AFTER_COMMA;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        // The end of the string is valid in five of the seven states. The two that do not accept it
        // are the ones left waiting for something: ST_BEFORE_UB (we saw the dash) and
        // ST_AFTER_COMMA (we saw the comma).
        if (state == ST_IN_LB || state == ST_AFTER_LB) {
            raw = push(raw, count, lb, lb);
            count++;
        } else if (state == ST_IN_UB || state == ST_AFTER_UB) {
            raw = push(raw, count, lb, ub);
            count++;
        } else if (state != ST_START) {
            throw new IllegalArgumentException();
        }
        return canonicalArrayForm(raw, count);
    }

    // The `int[][]` form. Each row is {n} or {lb, ub}; any other length is an error.
    private static int[][] parse(int[][] members) {
        int n = (members == null) ? 0 : members.length;
        int[][] raw = new int[n < 1 ? 1 : n][];
        int count = 0;
        for (int i = 0; i < n; i++) {
            int lb;
            int ub;
            if (members[i].length == 1) {
                lb = members[i][0];
                ub = members[i][0];
            } else if (members[i].length == 2) {
                lb = members[i][0];
                ub = members[i][1];
            } else {
                throw new IllegalArgumentException();
            }
            if (lb <= ub) {
                if (lb < 0) {
                    throw new IllegalArgumentException();
                }
                raw = push(raw, count, lb, ub);
                count++;
            }
        }
        return canonicalArrayForm(raw, count);
    }

    // Pushes {lb, ub} at position `count`, growing the array if needed.
    private static int[][] push(int[][] raw, int count, int lb, int ub) {
        int[][] target = raw;
        if (count >= target.length) {
            int[][] larger = new int[target.length * 2 + 1][];
            for (int i = 0; i < count; i++) {
                larger[i] = target[i];
            }
            target = larger;
        }
        target[count] = new int[] {lb, ub};
        return target;
    }

    // Sorts, discards the empty ones and merges the ones that overlap or touch.
    //
    // The merge is decided with `long` and not `int`. The reason, measured by ablation (the
    // comparison was changed to `int` and test 60 of PrnSetIntSyntaxTest started failing): with `ub
    // + 1` in `int`, a range ending at Integer.MAX_VALUE wraps around to Integer.MIN_VALUE and the
    // comparison `lb <= ub + 1` comes out false against **any** lower bound. That is, the
    // overflow's error is to stop merging what is adjacent, not to merge too much: `{{0, MAX},
    // {MAX, MAX}}` has to give "0-2147483647" and with `int` it gave two ranges.
    //
    // **Known and only divergence against the JDK, verified by running the same program against
    // both.** The JDK merges with `Math.max(lba, lbb) - Math.min(uba, ubb) <= 1`, in `int`, and
    // that subtraction overflows when one of the bounds is negative. Negative bounds cannot be put
    // in through any of the four valid forms --they all reject the sign-- but they do appear
    // through the text form's overflow the header documents, and there the two implementations part
    // ways:
    //
    //     "0,2147483648"   JDK: "-2147483648-0"    ours: "-2147483648,0"
    //
    // The JDK merges the two points into a range of four billion elements because the subtraction
    // wrapped around; we leave them separate, which is what they are. It was preferred not to
    // replicate the overflow: the input is already garbage in both cases, and copying the second
    // overflow to cover the first would make `contains(-5)` return true.
    private static int[][] canonicalArrayForm(int[][] raw, int count) {
        // Insertion: the list is short and this way no Comparator is needed.
        for (int i = 1; i < count; i++) {
            int[] current = raw[i];
            int j = i - 1;
            while (j >= 0 && (raw[j][0] > current[0]
                              || (raw[j][0] == current[0] && raw[j][1] > current[1]))) {
                raw[j + 1] = raw[j];
                j--;
            }
            raw[j + 1] = current;
        }
        int[][] merged = new int[count][];
        int used = 0;
        for (int i = 0; i < count; i++) {
            int lb = raw[i][0];
            int ub = raw[i][1];
            if (lb > ub) {
                continue;
            }
            if (used > 0 && ((long) lb) <= ((long) merged[used - 1][1]) + 1L) {
                if (ub > merged[used - 1][1]) {
                    merged[used - 1][1] = ub;
                }
            } else {
                merged[used] = new int[] {lb, ub};
                used++;
            }
        }
        int[][] result = new int[used][];
        for (int i = 0; i < used; i++) {
            result[i] = merged[i];
        }
        return result;
    }

    // A copy: the internal array never goes out.
    public int[][] getMembers() {
        int n = this.members.length;
        int[][] result = new int[n][2];
        for (int i = 0; i < n; i++) {
            result[i][0] = this.members[i][0];
            result[i][1] = this.members[i][1];
        }
        return result;
    }

    // The ranges are sorted, so one can stop as soon as one is past.
    public boolean contains(int x) {
        int n = this.members.length;
        for (int i = 0; i < n; i++) {
            if (x < this.members[i][0]) {
                return false;
            }
            if (x <= this.members[i][1]) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(IntegerSyntax attribute) {
        return contains(attribute.getValue());
    }

    // The smallest member **strictly greater** than x, or -1 if there is none.
    public int next(int x) {
        int n = this.members.length;
        for (int i = 0; i < n; i++) {
            if (x < this.members[i][0]) {
                return this.members[i][0];
            }
            if (x < this.members[i][1]) {
                return x + 1;
            }
        }
        return -1;
    }

    // Component by component: both sides are canonicalized, so that is enough.
    public boolean equals(Object object) {
        if (!(object instanceof SetOfIntegerSyntax)) {
            return false;
        }
        int[][] others = ((SetOfIntegerSyntax) object).members;
        int n = this.members.length;
        if (n != others.length) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (this.members[i][0] != others[i][0] || this.members[i][1] != others[i][1]) {
                return false;
            }
        }
        return true;
    }

    // The sum of the ends. Cheap and consistent with equals thanks to the canonicalization.
    public int hashCode() {
        int result = 0;
        int n = this.members.length;
        for (int i = 0; i < n; i++) {
            result += this.members[i][0] + this.members[i][1];
        }
        return result;
    }

    // "1-5,7,10-12". A one-element range is printed without a dash; the empty set is the empty
    // string.
    public String toString() {
        StringBuilder result = new StringBuilder();
        int n = this.members.length;
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                result.append(',');
            }
            int lb = this.members[i][0];
            int ub = this.members[i][1];
            if (lb == ub) {
                result.append(lb);
            } else {
                result.append(lb);
                result.append('-');
                result.append(ub);
            }
        }
        return result.toString();
    }
}
