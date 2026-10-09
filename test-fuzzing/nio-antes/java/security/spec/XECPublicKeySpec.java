package java.security.spec;

import java.math.BigInteger;

// A Montgomery-curve public key (X25519, X448): the u coordinate.
//
// Only u, without v. It is not a compression like Edwards': the Montgomery ladder X25519 uses never
// needs the other coordinate, so it is simply not transmitted. An X25519 public key is 32 bytes and
// there is no sign bit to add.
//
// The parameters are an `AlgorithmParameterSpec` and not a `NamedParameterSpec` as in Edwards. The
// difference is the real API's and has to be respected: here a `NamedParameterSpec` fits as well as
// something else.
public class XECPublicKeySpec implements KeySpec {

    private final AlgorithmParameterSpec params;
    private final BigInteger u;

    public XECPublicKeySpec(AlgorithmParameterSpec params, BigInteger u) {
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        if (u == null) {
            throw new NullPointerException("u must not be null");
        }
        this.params = params;
        this.u = u;
    }

    public AlgorithmParameterSpec getParams() {
        return this.params;
    }

    // The u coordinate of the public point.
    public BigInteger getU() {
        return this.u;
    }
}
