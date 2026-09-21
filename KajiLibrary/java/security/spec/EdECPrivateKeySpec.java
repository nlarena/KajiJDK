package java.security.spec;

// An Edwards private key: the curve by name, plus the bytes of the seed.
//
// They are **bytes** and not a `BigInteger`, and the difference matters: in EdDSA the private key
// is not the scalar but a seed from which both the scalar and the value used to generate each
// signature's nonce are derived by hashing. That is what makes Ed25519 deterministic and what saves
// it from the disaster that sinks DSA and ECDSA when the nonce repeats. Treating these bytes as an
// integer would lose that distinction.
public final class EdECPrivateKeySpec implements KeySpec {

    private final NamedParameterSpec params;
    private final byte[] bytes;

    public EdECPrivateKeySpec(NamedParameterSpec params, byte[] bytes) {
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        if (bytes == null) {
            throw new NullPointerException("bytes must not be null");
        }
        this.params = params;
        this.bytes = copyOf(bytes);
    }

    private static byte[] copyOf(byte[] b) {
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    public NamedParameterSpec getParams() {
        return this.params;
    }

    // A copy of the seed. The copy is required in both directions: it is secret material, and
    // whoever hands it over cannot be exposed to the receiver changing the array underneath.
    public byte[] getBytes() {
        return copyOf(this.bytes);
    }
}
