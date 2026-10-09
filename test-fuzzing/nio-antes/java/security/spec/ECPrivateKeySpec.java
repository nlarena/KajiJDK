package java.security.spec;

import java.math.BigInteger;

// An EC private key in the clear: the scalar s, plus the domain parameters.
//
// It is the most dangerous type in the whole package, not because of what it does but because of
// what it holds: the `BigInteger` is the whole private key, in memory, unprotected. There is no way
// to erase it —`BigInteger` is immutable and does not expose its array— and that is why the real
// API prefers a private key to live behind an opaque `PrivateKey`. This spec exists for the moment
// a key has to be built from its numbers, not for keeping it around like this.
public class ECPrivateKeySpec implements KeySpec {

    private final BigInteger s;
    private final ECParameterSpec params;

    public ECPrivateKeySpec(BigInteger s, ECParameterSpec params) {
        if (s == null) {
            throw new NullPointerException("s is null");
        }
        if (params == null) {
            throw new NullPointerException("params is null");
        }
        this.s = s;
        this.params = params;
    }

    // The private scalar.
    public BigInteger getS() {
        return this.s;
    }

    public ECParameterSpec getParams() {
        return this.params;
    }
}
