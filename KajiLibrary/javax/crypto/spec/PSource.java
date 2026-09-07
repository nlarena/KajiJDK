package javax.crypto.spec;

/**
 * Where OAEP's input label (`P`) comes from.
 *
 * <p>It is a class and not an enum because the standard leaves the door open to other sources, but in
 * practice there is only one: {@link PSpecified}, "the bytes I give you". That the hierarchy exists
 * with a single member comes from PKCS#1's design, not from this library.
 *
 * <p>The constructor is `protected`: the base class represents no source on its own, only the
 * concept.
 */
public class PSource {

    private final String pSrcName;

    /**
     * @throws NullPointerException if the name is null
     */
    protected PSource(String pSrcName) {
        if (pSrcName == null) {
            throw new NullPointerException("the source's name cannot be null");
        }
        this.pSrcName = pSrcName;
    }

    /** The source's algorithm name. */
    public String getAlgorithm() {
        return this.pSrcName;
    }

    /**
     * The source that is a given set of bytes.
     *
     * <p>{@link #DEFAULT} is the **empty** label, which is what almost everyone uses: OAEP with an
     * empty label is what TLS and most protocols do. Having it as a constant avoids building a
     * zero-byte array on every call.
     */
    public static final class PSpecified extends PSource {

        /** The empty label. */
        public static final PSpecified DEFAULT = new PSpecified(new byte[0]);

        private final byte[] p;

        /**
         * @throws NullPointerException if the array is null
         */
        public PSpecified(byte[] p) {
            super("PSpecified");
            if (p == null) {
                throw new NullPointerException("the label cannot be null");
            }
            this.p = IvParameterSpec.copy(p, 0, p.length);
        }

        /** A copy of the label. */
        public byte[] getValue() {
            return IvParameterSpec.copy(this.p, 0, this.p.length);
        }
    }
}
