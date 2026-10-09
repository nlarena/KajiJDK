package java.security.spec;

// The key as a stream of bytes in some standard format, plus the name of the algorithm.
//
// It is the least transparent `KeySpec` of all —it exposes not a single field of the key— and even
// so it is the most used, because it is the only one that works for an algorithm the library does
// not know: if the DER can be moved from one side to the other, there is no need to understand it.
//
// The array is **copied** on the way in and on the way out. It is not stylistic paranoia: whoever
// receives an encoded key cannot allow whoever gave it to change it behind its back after
// validation, and whoever hands it over cannot allow the receiver to mutate the internal copy.
public abstract class EncodedKeySpec implements KeySpec {

    private final byte[] encodedKey;

    // The name of the algorithm, or null if whoever built the spec did not know it. Its being
    // optional is part of the contract: an X.509 DER carries the algorithm inside, and the caller
    // may not have read it.
    private final String algorithmName;

    public EncodedKeySpec(byte[] encodedKey) {
        this.encodedKey = copyOf(encodedKey);
        this.algorithmName = null;
    }

    protected EncodedKeySpec(byte[] encodedKey, String algorithm) {
        if (algorithm == null) {
            throw new NullPointerException("algorithm name may not be null");
        }
        if (algorithm.isEmpty()) {
            throw new IllegalArgumentException("algorithm name may not be empty");
        }
        this.encodedKey = copyOf(encodedKey);
        this.algorithmName = algorithm;
    }

    private static byte[] copyOf(byte[] b) {
        if (b == null) {
            throw new NullPointerException("the encoded key must not be null");
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    // The name of the algorithm, or null if none was given.
    public String getAlgorithm() {
        return this.algorithmName;
    }

    // A copy of the encoded bytes.
    public byte[] getEncoded() {
        byte[] c = new byte[this.encodedKey.length];
        System.arraycopy(this.encodedKey, 0, c, 0, this.encodedKey.length);
        return c;
    }

    // The name of the encoding format: "X.509", "PKCS#8".
    public abstract String getFormat();
}
