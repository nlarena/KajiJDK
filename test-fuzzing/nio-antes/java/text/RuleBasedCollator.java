package java.text;

/**
 * A {@link Collator} whose ordering is given as <em>data</em> -- a rule string -- rather than as
 * code.
 *
 * <p>That is the design decision worth pointing at. Every language sorts differently, and the
 * differences are small and endless: Swedish files {@code v} and {@code w} together, Spanish once
 * treated {@code ch} as a letter of its own, Danish puts {@code aa} at the end of the alphabet. A
 * collator that hard-coded any of this would need a new class per language. So instead the rules
 * are a little text format, and the class is one interpreter for all of them:
 *
 * <pre>
 *     new RuleBasedCollator("&lt; a,A &lt; b,B &lt; c,C");
 * </pre>
 *
 * <h2>The rule syntax</h2>
 *
 * <p>A rule string is a sequence of <var>relation</var> <var>character</var> pairs. The relation
 * says how strongly the character differs from the one before it:
 *
 * <ul>
 * <li>{@code <} -- a <b>primary</b> difference: a different base letter.</li>
 * <li>{@code ;} -- a <b>secondary</b> difference: the same letter with a different accent.</li>
 * <li>{@code ,} -- a <b>tertiary</b> difference: the same letter and accent, different case.</li>
 * <li>{@code =} -- no difference at all: the two characters sort identically.</li>
 * <li>{@code &} -- a <b>reset</b>: continue from the weights of an already-listed character, which
 *     is how a locale adds one letter to an existing alphabet without restating it.</li>
 * </ul>
 *
 * <p>Whitespace between tokens is ignored, and a character can be quoted with {@code '} when it
 * would otherwise be read as a relation ({@code '<'} is the less-than sign, not a primary
 * difference). {@code ''} is a literal quote.
 *
 * @implNote A KajiLibrary subset of the rule syntax. Not implemented, and each rejected rather than
 *           silently mis-parsed:
 *           <ul>
 *           <li><b>contractions</b> -- a multi-character element such as {@code < ch}. It needs the
 *               element iterator to look ahead, which is the change described on
 *               {@link CollationElementIterator}.</li>
 *           <li><b>expansions</b> -- {@code < ae/e}, one character weighing as several.</li>
 *           <li>the modifiers {@code @} (French accent ordering, which reverses the secondary
 *               comparison) and {@code !} (Thai/Lao vowel rearrangement).</li>
 *           </ul>
 *
 * @implNote {@link Collator#getInstance} returns a collator whose table is built in code rather
 *           than parsed from a rule string, and its {@link #getRules} is generated back from the
 *           table on demand. The reason is mundane and worth writing down: an accented rule string
 *           cannot be written as a Java {@code String} literal today, because the frozen VM reads
 *           a string constant's modified-UTF-8 back one {@code char} per <em>byte</em>, so
 *           {@code "\u00e9"} arrives as two characters. Character literals are unaffected, so the
 *           table is built from those. When that VM defect is fixed the default becomes an
 *           ordinary rule string like any other.
 */
public class RuleBasedCollator extends Collator {

    // The ordering table: a character and the packed weights it carries, in parallel arrays.
    //
    // A linear scan is the lookup. The JDK indexes a compressed trie, which is the right structure
    // for a table with thousands of entries; with the low hundreds these rules produce, the scan is
    // simpler to read and to get right, and the ASCII fast path below removes it from the hot case
    // that actually matters.
    private char[] tableChars;
    private int[] tableOrders;
    private int tableSize;

    // Direct index for U+0000..U+007F. -1 means "not in the table"; a real order can never be -1,
    // since that would mean every level at its maximum.
    private int[] ascii;

    // The rule string this collator was built from, or null for the code-built default (whose rules
    // are regenerated on demand -- see the class note).
    private String rules;

    /**
     * Builds a collator from a rule string.
     *
     * @param rules the rules, in the syntax described above
     * @throws ParseException if the rules are malformed, or use a construct this subset does not
     *         implement; the error offset is the character that could not be read
     * @throws NullPointerException if {@code rules} is {@code null}
     */
    public RuleBasedCollator(String rules) throws ParseException {
        if (rules == null) {
            throw new NullPointerException();
        }
        this.startTable();
        int error = this.applyRules(rules);
        if (error >= 0) {
            throw new ParseException("Unreadable collation rule", error);
        }
        this.rules = rules;
    }

    // The code-built default. Package-private and non-throwing, because Collator.getInstance()
    // does not declare ParseException and must not be able to fail.
    private RuleBasedCollator() {
        this.startTable();
        this.buildDefaultTable();
        this.rules = null;
    }

    // A copy, for clone(). Object.clone does not exist in this library, so a copy is made by
    // construction; the arrays are duplicated so the two collators cannot alias each other.
    private RuleBasedCollator(RuleBasedCollator other) {
        this.tableSize = other.tableSize;
        this.tableChars = new char[other.tableChars.length];
        this.tableOrders = new int[other.tableOrders.length];
        for (int i = 0; i < other.tableSize; ++i) {
            this.tableChars[i] = other.tableChars[i];
            this.tableOrders[i] = other.tableOrders[i];
        }
        this.ascii = new int[128];
        for (int i = 0; i < 128; ++i) {
            this.ascii[i] = other.ascii[i];
        }
        this.rules = other.rules;
        this.setStrength(other.getStrength());
        this.setDecomposition(other.getDecomposition());
    }

    /**
     * Returns the default collator: a code-built ASCII-and-Latin-1 ordering.
     *
     * @return a new collator
     */
    static RuleBasedCollator defaultCollator() {
        return new RuleBasedCollator();
    }

    /**
     * Returns the rules this collator sorts by.
     *
     * @return the rule string
     */
    public String getRules() {
        if (this.rules == null) {
            this.rules = this.generateRules();
        }
        return this.rules;
    }

    /**
     * Returns an iterator over the collation elements of a string.
     *
     * @param source the text
     * @return a new iterator positioned at the start
     */
    public CollationElementIterator getCollationElementIterator(String source) {
        return new CollationElementIterator(source, this);
    }

    /**
     * Returns an iterator over the collation elements of some text.
     *
     * @param source the text
     * @return a new iterator positioned at the start
     */
    public CollationElementIterator getCollationElementIterator(CharacterIterator source) {
        return new CollationElementIterator(source, this);
    }

    /**
     * Compares two strings under these rules and this collator's strength.
     *
     * @param source the first string
     * @param target the second string
     * @return negative, zero or positive as {@code source} sorts before, with, or after
     *         {@code target}
     * @implSpec Compares one level at a time, coarsest first, and stops at the first level that
     *           differs or at the collator's strength -- the whole point of the layering being that
     *           a difference in accent must never outrank a difference in letter, no matter where
     *           in the string it falls.
     */
    public synchronized int compare(String source, String target) {
        int[] left = this.elements(source);
        int[] right = this.elements(target);
        int strength = this.getStrength();

        int result = RuleBasedCollator.compareLevel(left, right, 0);
        if (result != 0) {
            return result;
        }
        if (strength == 0) {           // PRIMARY
            return 0;
        }
        result = RuleBasedCollator.compareLevel(left, right, 1);
        if (result != 0) {
            return result;
        }
        if (strength == 1) {           // SECONDARY
            return 0;
        }
        result = RuleBasedCollator.compareLevel(left, right, 2);
        if (result != 0) {
            return result;
        }
        if (strength < 3) {            // TERTIARY
            return 0;
        }
        // IDENTICAL: nothing left but the code points themselves.
        int raw = source.compareTo(target);
        if (raw < 0) {
            return -1;
        }
        return raw > 0 ? 1 : 0;
    }

    /**
     * Reduces a string to a sort key under these rules and this collator's strength.
     *
     * @param source the string
     * @return its key
     */
    public synchronized CollationKey getCollationKey(String source) {
        if (source == null) {
            return null;
        }
        return new RuleBasedCollationKey(source, this.keyBytes(source));
    }

    /**
     * Returns an independent copy of this collator.
     *
     * @return the copy
     */
    public Object clone() {
        return new RuleBasedCollator(this);
    }

    /**
     * Two rule-based collators are equal when their tables and their settings agree.
     *
     * @param obj the object to compare with
     * @return {@code true} if they sort identically
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RuleBasedCollator)) {
            return false;
        }
        RuleBasedCollator other = (RuleBasedCollator) obj;
        if (this.getStrength() != other.getStrength()) {
            return false;
        }
        if (this.getDecomposition() != other.getDecomposition()) {
            return false;
        }
        if (this.tableSize != other.tableSize) {
            return false;
        }
        for (int i = 0; i < this.tableSize; ++i) {
            if (this.tableChars[i] != other.tableChars[i]) {
                return false;
            }
            if (this.tableOrders[i] != other.tableOrders[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int hash = this.tableSize;
        for (int i = 0; i < this.tableSize; ++i) {
            hash = hash * 31 + this.tableOrders[i];
        }
        return hash;
    }

    // ---- the table ------------------------------------------------------------------------

    // The packed weights of `c`, or the unmapped value if the rules never mentioned it. Called by
    // CollationElementIterator, which is why it is package-private rather than private.
    int orderOf(char c) {
        if (c < 128) {
            int direct = this.ascii[c];
            if (direct != -1) {
                return direct;
            }
        } else {
            for (int i = 0; i < this.tableSize; ++i) {
                if (this.tableChars[i] == c) {
                    return this.tableOrders[i];
                }
            }
        }
        // Unmapped: primary 0x7FFF sorts after everything the rules named, and carrying the
        // character in the low bits keeps two unmapped characters distinguishable. See the note on
        // CollationElementIterator.
        return 0x7fff0000 | c;
    }

    private void startTable() {
        this.tableChars = new char[64];
        this.tableOrders = new int[64];
        this.tableSize = 0;
        this.ascii = new int[128];
        for (int i = 0; i < 128; ++i) {
            this.ascii[i] = -1;
        }
    }

    private void put(char c, int primary, int secondary, int tertiary) {
        int packed = (primary << 16) | ((secondary & 0xff) << 8) | (tertiary & 0xff);
        if (c < 128) {
            this.ascii[c] = packed;
        }
        for (int i = 0; i < this.tableSize; ++i) {
            if (this.tableChars[i] == c) {
                this.tableOrders[i] = packed;
                return;
            }
        }
        if (this.tableSize == this.tableChars.length) {
            char[] biggerChars = new char[this.tableSize * 2];
            int[] biggerOrders = new int[this.tableSize * 2];
            for (int i = 0; i < this.tableSize; ++i) {
                biggerChars[i] = this.tableChars[i];
                biggerOrders[i] = this.tableOrders[i];
            }
            this.tableChars = biggerChars;
            this.tableOrders = biggerOrders;
        }
        this.tableChars[this.tableSize] = c;
        this.tableOrders[this.tableSize] = packed;
        this.tableSize = this.tableSize + 1;
    }

    // ---- rule parsing ---------------------------------------------------------------------

    // Applies `rules`, returning -1 on success or the offset of the first character that could not
    // be read. Returning an offset instead of throwing keeps the parser usable from the
    // non-throwing default constructor; the public constructor turns the offset into a
    // ParseException, which is the API.
    private int applyRules(String rules) {
        int primary = 0;
        int secondary = 0;
        int tertiary = 0;
        int i = 0;
        int length = rules.length();
        while (i < length) {
            char relation = rules.charAt(i);
            if (RuleBasedCollator.isRuleSpace(relation)) {
                i = i + 1;
                continue;
            }
            if (relation != '<' && relation != ';' && relation != ','
                    && relation != '=' && relation != '&') {
                return i;
            }
            i = i + 1;
            // Skip to the character the relation applies to.
            while (i < length && RuleBasedCollator.isRuleSpace(rules.charAt(i))) {
                i = i + 1;
            }
            if (i >= length) {
                return i - 1;
            }
            char c = rules.charAt(i);
            if (c == '\'') {
                // A quoted literal: '<' is the character, '' is a quote.
                if (i + 1 >= length) {
                    return i;
                }
                c = rules.charAt(i + 1);
                if (i + 2 >= length || rules.charAt(i + 2) != '\'') {
                    return i;
                }
                i = i + 2;
            }
            i = i + 1;
            // An expansion (`a/b`) or a contraction (two letters before the next relation) is
            // rejected rather than half-honoured: see the class note.
            if (i < length && rules.charAt(i) == '/') {
                return i;
            }
            if (i < length && !RuleBasedCollator.isRuleSpace(rules.charAt(i))
                    && !RuleBasedCollator.isRelation(rules.charAt(i))) {
                return i;
            }

            if (relation == '&') {
                int existing = this.orderOf(c);
                if ((existing & 0xffff0000) == 0x7fff0000) {
                    // Reset onto a character the rules have not introduced yet.
                    return i - 1;
                }
                primary = (existing & 0xffff0000) >>> 16;
                secondary = (existing & 0x0000ff00) >> 8;
                tertiary = existing & 0x000000ff;
                continue;
            }
            if (relation == '<') {
                primary = primary + 1;
                secondary = 0;
                tertiary = 0;
            } else if (relation == ';') {
                secondary = secondary + 1;
                tertiary = 0;
            } else if (relation == ',') {
                tertiary = tertiary + 1;
            }
            this.put(c, primary, secondary, tertiary);
        }
        return -1;
    }

    private static boolean isRelation(char c) {
        return c == '<' || c == ';' || c == ',' || c == '=' || c == '&';
    }

    private static boolean isRuleSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    // ---- the built-in ordering -------------------------------------------------------------

    // Built in code, from character literals, for the reason given in the class note. The shape is
    // the conventional one: whitespace, then punctuation, then digits, then letters, with case as a
    // tertiary difference and Latin-1 accents as secondary differences on their base letter.
    private void buildDefaultTable() {
        int primary = 0;

        // Whitespace: one primary, told apart at the tertiary level so that a tab and a space are
        // not the same string at full strength.
        primary = primary + 1;
        this.put(' ', primary, 0, 0);
        this.put('\t', primary, 0, 1);
        this.put('\n', primary, 0, 2);
        this.put('\r', primary, 0, 3);
        this.put('\f', primary, 0, 4);
        this.put('', primary, 0, 5);
        this.put('\u00a0', primary, 0, 6);

        // Punctuation and symbols, each its own primary, in the order below.
        char[] punctuation = new char[] {
            '_', '-', ',', ';', ':', '!', '?', '.', '\'', '"',
            '(', ')', '[', ']', '{', '}', '@', '$', '*', '\\',
            '&', '#', '%', '`', '^', '+', '<', '=', '>', '|',
            '~', '/'
        };
        for (int i = 0; i < punctuation.length; ++i) {
            primary = primary + 1;
            this.put(punctuation[i], primary, 0, 0);
        }

        // Digits.
        for (char d = '0'; d <= '9'; d = (char) (d + 1)) {
            primary = primary + 1;
            this.put(d, primary, 0, 0);
        }

        // Letters: lowercase is the base, uppercase differs only in the tertiary weight, which is
        // exactly what makes SECONDARY strength case-insensitive.
        int aPrimary = primary + 1;
        for (char c = 'a'; c <= 'z'; c = (char) (c + 1)) {
            primary = primary + 1;
            this.put(c, primary, 0, 0);
            this.put((char) (c - 'a' + 'A'), primary, 0, 1);
        }

        // Latin-1 accented letters, as secondary variants of their base. Written as ranges because
        // Latin-1 groups each base letter's accents contiguously, lowercase at +0x20 from upper.
        this.accents('a', aPrimary, '\u00c0', '\u00c5');   // A-grave .. A-ring
        this.accents('c', aPrimary + 2, '\u00c7', '\u00c7');
        this.accents('e', aPrimary + 4, '\u00c8', '\u00cb');
        this.accents('i', aPrimary + 8, '\u00cc', '\u00cf');
        this.accents('n', aPrimary + 13, '\u00d1', '\u00d1');
        this.accents('o', aPrimary + 14, '\u00d2', '\u00d6');
        this.accents('u', aPrimary + 20, '\u00d9', '\u00dc');
        this.accents('y', aPrimary + 24, '\u00dd', '\u00dd');
    }

    // Maps the uppercase range [from, to] and its lowercase counterparts (+0x20) onto `basePrimary`
    // as successive secondary weights.
    private void accents(char base, int basePrimary, char from, char to) {
        int secondary = 0;
        for (char upper = from; upper <= to; upper = (char) (upper + 1)) {
            secondary = secondary + 1;
            char lower = (char) (upper + 0x20);
            this.put(lower, basePrimary, secondary, 0);
            this.put(upper, basePrimary, secondary, 1);
        }
    }

    // Regenerates a rule string from the table, for the code-built default. One `<` per primary,
    // `;` per secondary, `,` per tertiary -- the same grammar the parser reads, so the result feeds
    // back into `new RuleBasedCollator(...)`.
    private String generateRules() {
        // Order the entries by weight with a simple selection pass: the table is small and this
        // runs at most once per collator.
        int[] order = new int[this.tableSize];
        for (int i = 0; i < this.tableSize; ++i) {
            order[i] = i;
        }
        for (int i = 0; i < this.tableSize; ++i) {
            int best = i;
            for (int j = i + 1; j < this.tableSize; ++j) {
                if (this.tableOrders[order[j]] < this.tableOrders[order[best]]) {
                    best = j;
                }
            }
            int swap = order[i];
            order[i] = order[best];
            order[best] = swap;
        }
        StringBuffer out = new StringBuffer();
        int lastPrimary = -1;
        int lastSecondary = -1;
        for (int i = 0; i < this.tableSize; ++i) {
            int packed = this.tableOrders[order[i]];
            char c = this.tableChars[order[i]];
            int primary = (packed & 0xffff0000) >>> 16;
            int secondary = (packed & 0x0000ff00) >> 8;
            if (primary != lastPrimary) {
                out.append('<');
                lastPrimary = primary;
                lastSecondary = secondary;
            } else if (secondary != lastSecondary) {
                out.append(';');
                lastSecondary = secondary;
            } else {
                out.append(',');
            }
            if (RuleBasedCollator.isRelation(c) || c == '\'' || RuleBasedCollator.isRuleSpace(c)) {
                out.append('\'');
                out.append(c);
                out.append('\'');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // ---- comparison -------------------------------------------------------------------------

    private int[] elements(String s) {
        int[] out = new int[s.length()];
        for (int i = 0; i < s.length(); ++i) {
            out[i] = this.orderOf(s.charAt(i));
        }
        return out;
    }

    // Compares one level of two element sequences. `level` is 0 primary, 1 secondary, 2 tertiary.
    // Zero weights are SKIPPED rather than compared: a zero at a level means "this element says
    // nothing here", which is what makes an ignorable character invisible to the coarse levels
    // while still counting at the fine ones.
    private static int compareLevel(int[] left, int[] right, int level) {
        int i = 0;
        int j = 0;
        while (true) {
            int a = 0;
            while (i < left.length && a == 0) {
                a = RuleBasedCollator.weight(left[i], level);
                i = i + 1;
            }
            int b = 0;
            while (j < right.length && b == 0) {
                b = RuleBasedCollator.weight(right[j], level);
                j = j + 1;
            }
            if (a == 0 && b == 0) {
                return 0;
            }
            if (a != b) {
                return a < b ? -1 : 1;
            }
        }
    }

    private static int weight(int packed, int level) {
        if (level == 0) {
            return (packed & 0xffff0000) >>> 16;
        }
        if (level == 1) {
            return (packed & 0x0000ff00) >> 8;
        }
        return packed & 0x000000ff;
    }

    // The key bytes: each level's non-zero weights in order, levels separated by a zero byte. Zero
    // can be the separator precisely because a zero weight never reaches the key.
    private byte[] keyBytes(String source) {
        int[] elements = this.elements(source);
        int strength = this.getStrength();
        int levels = strength >= 2 ? 3 : strength + 1;

        int size = 0;
        for (int level = 0; level < levels; ++level) {
            for (int i = 0; i < elements.length; ++i) {
                if (RuleBasedCollator.weight(elements[i], level) != 0) {
                    size = size + (level == 0 ? 2 : 1);
                }
            }
            size = size + 1;   // the separator
        }

        byte[] key = new byte[size];
        int at = 0;
        for (int level = 0; level < levels; ++level) {
            for (int i = 0; i < elements.length; ++i) {
                int w = RuleBasedCollator.weight(elements[i], level);
                if (w == 0) {
                    continue;
                }
                if (level == 0) {
                    key[at] = (byte) ((w >> 8) & 0xff);
                    key[at + 1] = (byte) (w & 0xff);
                    at = at + 2;
                } else {
                    key[at] = (byte) (w & 0xff);
                    at = at + 1;
                }
            }
            key[at] = 0;
            at = at + 1;
        }
        return key;
    }

}
