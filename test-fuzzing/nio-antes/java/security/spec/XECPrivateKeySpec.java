package java.security.spec;

// A Montgomery-curve private key: the scalar, as bytes.
//
// Bytes and not a `BigInteger` because the X25519 scalar is not used as is: before multiplying,
// fixed bits are "clamped" —the three low ones are cleared and the top ones fixed— so that it is
// always a multiple of the cofactor and has a constant length. That is what neutralizes
// small-subgroup attacks and what lets the ladder run in constant time. An integer has nowhere to
// keep that shape; a fixed-length byte array does.
public class XECPrivateKeySpec implements KeySpec {

    private final AlgorithmParameterSpec params;
    private final byte[] scalar;

    public XECPrivateKeySpec(AlgorithmParameterSpec params, byte[] scalar) {
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        if (scalar == null) {
            throw new NullPointerException("scalar must not be null");
        }
        this.params = params;
        this.scalar = copyOf(scalar);
    }

    private static byte[] copyOf(byte[] b) {
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    public AlgorithmParameterSpec getParams() {
        return this.params;
    }

    // A copy of the private scalar.
    public byte[] getScalar() {
        return copyOf(this.scalar);
    }
}
