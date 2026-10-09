package java.text;

/**
 * The {@link CollationKey} a {@link RuleBasedCollator} produces: the collation weights of a string,
 * flattened into bytes whose plain unsigned order is the collation order.
 *
 * <p>Package-private, like the JDK's, because the byte layout is an implementation detail of the
 * collator that made it -- a key is only ever meaningful to the rules it came from, and giving the
 * class a name would invite code to depend on the shape.
 *
 * <p>The layout is one level at a time, coarsest first, with a zero byte between levels: all the
 * primary weights, {@code 0}, all the secondary weights, {@code 0}, all the tertiary weights. Zero
 * works as the separator because a zero weight is never written -- a zero at a level means the
 * element says nothing there, and such elements are skipped. So a byte-by-byte comparison reaches
 * the separator exactly when one string's primaries run out, which is where a shorter prefix must
 * sort first, and only then does it start comparing accents.
 */
final class RuleBasedCollationKey extends CollationKey {

    private final byte[] key;

    RuleBasedCollationKey(String source, byte[] key) {
        super(source);
        this.key = key;
    }

    public int compareTo(CollationKey target) {
        byte[] other = target.toByteArray();
        int shared = this.key.length < other.length ? this.key.length : other.length;
        for (int i = 0; i < shared; ++i) {
            // Unsigned: a weight of 0x80 must sort after 0x7F, and a byte is signed in Java.
            int a = this.key[i] & 0xff;
            int b = other[i] & 0xff;
            if (a != b) {
                return a < b ? -1 : 1;
            }
        }
        if (this.key.length == other.length) {
            return 0;
        }
        return this.key.length < other.length ? -1 : 1;
    }

    public byte[] toByteArray() {
        byte[] copy = new byte[this.key.length];
        for (int i = 0; i < this.key.length; ++i) {
            copy[i] = this.key[i];
        }
        return copy;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CollationKey)) {
            return false;
        }
        return this.compareTo((CollationKey) obj) == 0;
    }

    public int hashCode() {
        int hash = 1;
        for (int i = 0; i < this.key.length; ++i) {
            hash = hash * 31 + (this.key[i] & 0xff);
        }
        return hash;
    }
}
