package java.text;

/**
 * Walks text and yields one <em>collation element</em> per step: the sort weights a
 * {@link RuleBasedCollator} assigns to what it just read.
 *
 * <p>It exists because "the weight of a character" is not a well-formed question. Collation rules
 * work on units that need not line up with {@code char}s at all -- Spanish once sorted {@code ch}
 * as a single letter, German {@code \u00df} sorts as if it were {@code ss}, and an accent that follows
 * its base letter contributes to the same element rather than a new one. So a collator does not
 * index a table by character; it runs an iterator that decides, at each position, how much text
 * makes up the next element and what that element weighs.
 *
 * <p>Each element is one {@code int} packing three levels:
 *
 * <pre>
 *     bits 31..16   primary    -- which base letter
 *     bits 15.. 8   secondary  -- which accent
 *     bits  7.. 0   tertiary   -- which case
 * </pre>
 *
 * <p>and {@link #primaryOrder}, {@link #secondaryOrder} and {@link #tertiaryOrder} unpack them. An
 * element whose primary order is zero is <em>ignorable</em>: it takes part in the finer levels but
 * never in the coarse one, which is how a soft hyphen can sit inside a word without changing where
 * the word sorts.
 *
 * @implNote A KajiLibrary subset: one element per {@code char}. Contractions (several characters
 *           forming one element) and expansions (one character forming several) are not
 *           implemented -- they are what {@link #getMaxExpansion} exists to report, and it therefore
 *           always answers 1. Both are additions to this class and to the rule parser, not
 *           redesigns: the element stream is already the seam they plug into.
 *
 * @implNote A character the rules do not mention yields {@code 0x7FFF0000 | c}. The primary order
 *           {@code 0x7FFF} sorts every unmapped character after every mapped one, and putting the
 *           character itself in the low sixteen bits makes two different unmapped characters
 *           differ at the secondary and tertiary levels rather than compare equal -- which keeps
 *           sorting deterministic. It is the JDK's constant and the JDK's reasoning.
 */
public final class CollationElementIterator {

    /**
     * Returned by {@link #next} at the end of the text and by {@link #previous} at the start.
     */
    public static final int NULLORDER = 0xffffffff;

    private final RuleBasedCollator owner;
    private CharacterIterator text;
    private int offset;

    // Package-private: an iterator only ever comes from the collator whose rules it applies.
    CollationElementIterator(String sourceText, RuleBasedCollator owner) {
        this.owner = owner;
        this.text = new StringCharacterIterator(sourceText);
        this.offset = this.text.getBeginIndex();
    }

    CollationElementIterator(CharacterIterator sourceText, RuleBasedCollator owner) {
        this.owner = owner;
        this.text = sourceText;
        this.offset = sourceText.getBeginIndex();
    }

    /**
     * Puts the cursor back at the beginning of the text.
     */
    public void reset() {
        this.offset = this.text.getBeginIndex();
    }

    /**
     * Returns the next collation element and advances.
     *
     * @return the element, or {@link #NULLORDER} at the end of the text
     */
    public int next() {
        if (this.offset >= this.text.getEndIndex()) {
            return -1;   // NULLORDER == 0xffffffff; spelled out because a same-class `static final`
                         // read is emitted as a `getstatic` that evaluates to 0 today (#112).
        }
        char c = this.text.setIndex(this.offset);
        this.offset = this.offset + 1;
        return this.owner.orderOf(c);
    }

    /**
     * Steps back and returns the collation element before the cursor.
     *
     * @return the element, or {@link #NULLORDER} at the start of the text
     */
    public int previous() {
        if (this.offset <= this.text.getBeginIndex()) {
            return -1;   // NULLORDER, spelled out: see next()
        }
        this.offset = this.offset - 1;
        char c = this.text.setIndex(this.offset);
        return this.owner.orderOf(c);
    }

    /**
     * Extracts the primary weight of an element.
     *
     * @param order a value returned by {@link #next} or {@link #previous}
     * @return the primary order, in {@code 0..0xFFFF}
     */
    public static final int primaryOrder(int order) {
        return (order & 0xffff0000) >>> 16;
    }

    /**
     * Extracts the secondary weight of an element.
     *
     * @param order a value returned by {@link #next} or {@link #previous}
     * @return the secondary order, in {@code 0..0xFF}
     */
    public static final short secondaryOrder(int order) {
        return (short) ((order & 0x0000ff00) >> 8);
    }

    /**
     * Extracts the tertiary weight of an element.
     *
     * @param order a value returned by {@link #next} or {@link #previous}
     * @return the tertiary order, in {@code 0..0xFF}
     */
    public static final short tertiaryOrder(int order) {
        return (short) (order & 0x000000ff);
    }

    /**
     * Moves the cursor to a character offset in the text.
     *
     * @param newOffset the offset
     * @throws IllegalArgumentException if it is outside the text
     */
    public void setOffset(int newOffset) {
        if (newOffset < this.text.getBeginIndex() || newOffset > this.text.getEndIndex()) {
            throw new IllegalArgumentException("Invalid offset");
        }
        this.offset = newOffset;
    }

    /**
     * Returns the character offset the cursor is at.
     *
     * @return the offset
     */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Returns how many collation elements the character that produced {@code order} expands into.
     *
     * @param order an element previously returned by this iterator
     * @return always 1 in this subset; see the class note on expansions
     */
    public int getMaxExpansion(int order) {
        return 1;
    }

    /**
     * Replaces the text and resets the cursor.
     *
     * @param source the new text
     */
    public void setText(String source) {
        this.text = new StringCharacterIterator(source);
        this.offset = this.text.getBeginIndex();
    }

    /**
     * Replaces the text and resets the cursor.
     *
     * @param source the new text
     */
    public void setText(CharacterIterator source) {
        this.text = source;
        this.offset = source.getBeginIndex();
    }
}
