package java.nio.charset;

/**
 * The base every charset in this library extends, so that {@link Charset#contains} has one
 * answer instead of one per pair.
 *
 * <p>Package-private, and shaped for us rather than for the JDK, which spells the same
 * relationship out charset by charset. The observation it rests on is that the charsets here
 * form a chain and not a lattice: US-ASCII sits inside ISO-8859-1, which sits inside every
 * Unicode encoding, and the Unicode encodings all cover exactly the same characters as each
 * other. One integer per charset therefore decides containment for every pair, and the
 * comparison cannot disagree with itself the way nine hand-written methods could.
 *
 * <p>Adding a charset that does <em>not</em> fit the chain -- a national eight-bit set that
 * overlaps ISO-8859-1 without containing it -- would mean this class stops being enough. That is
 * the moment to replace it, and the ranks are named rather than numeric so the moment is easy to
 * recognise.
 */
abstract class RankedCharset extends Charset {

    /** The 128 characters of US-ASCII. */
    static final int RANK_ASCII = 1;

    /** The 256 characters of ISO-8859-1, which begin with all of ASCII. */
    static final int RANK_LATIN1 = 2;

    /** Every Unicode code point; what all the UTF encodings cover. */
    static final int RANK_UNICODE = 3;

    private final int rank;

    RankedCharset(String canonicalName, String[] aliases, int rank) {
        super(canonicalName, aliases);
        this.rank = rank;
    }

    int rank() {
        return this.rank;
    }

    /**
     * Whether every character this charset can represent is also representable in {@code cs}.
     *
     * @param cs the charset to test for containment
     */
    public boolean contains(Charset cs) {
        if (!(cs instanceof RankedCharset)) {
            return false;
        }
        RankedCharset that = (RankedCharset) cs;
        return this.rank >= that.rank();
    }
}
