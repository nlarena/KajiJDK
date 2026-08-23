package java.text;

/**
 * A string reduced to the bits that decide its sort position, so that comparing it again is cheap.
 *
 * <p>{@link Collator#compare} has to walk both strings and weigh every character every time it is
 * called. Sorting {@code n} strings calls it {@code n log n} times, so each string is re-analysed
 * {@code log n} times over. A collation key does that work once and hands back an opaque value
 * whose plain byte order <em>is</em> the collation order -- after which sorting is a byte compare.
 *
 * <pre>
 *     CollationKey a = collator.getCollationKey("resume");
 *     CollationKey b = collator.getCollationKey("r\u00e9sum\u00e9");
 *     a.compareTo(b);   // same answer as collator.compare(...), much cheaper
 * </pre>
 *
 * <p>Keys from <em>different</em> collators, or from one collator whose strength or decomposition
 * was changed in between, are not comparable: the key encodes the rules that made it, and nothing
 * in the key says which rules those were. That is why the class has no public constructor -- a key
 * only ever comes from the collator that will be asked about it.
 *
 * @implNote Complete: the JDK class is exactly these members. It is abstract because the byte
 *           layout belongs to the collator implementation, not to the abstraction.
 */
public abstract class CollationKey implements Comparable<CollationKey> {

    private final String source;

    /**
     * For subclasses.
     *
     * @param source the string this key was built from
     * @throws NullPointerException if {@code source} is {@code null}
     */
    protected CollationKey(String source) {
        if (source == null) {
            throw new NullPointerException();
        }
        this.source = source;
    }

    /**
     * Compares this key with another from the same collator.
     *
     * @param target the key to compare against
     * @return negative, zero or positive as this key sorts before, with, or after {@code target}
     */
    public abstract int compareTo(CollationKey target);

    /**
     * Returns the string this key was built from.
     *
     * @return the source string
     */
    public String getSourceString() {
        return this.source;
    }

    /**
     * Returns the key as a byte array whose unsigned byte order matches the collation order.
     *
     * @return a fresh byte array
     */
    public abstract byte[] toByteArray();
}
