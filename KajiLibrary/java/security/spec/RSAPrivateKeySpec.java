package java.security.spec;

import java.math.BigInteger;

// An RSA private key in its minimal form: the modulus n and the private exponent d.
//
// (n, d) is enough to decrypt and sign, but it is expensive: a modular exponentiation with an
// exponent the size of n. `RSAPrivateCrtKeySpec` also keeps the factors so it can do it by the
// Chinese remainder theorem, which is about four times faster. That this class is the base and the
// other the subclass is no accident: what the subclass adds is **redundant**, and so it is the
// optional part. This note said the factors can be deduced from d "though not easily"; together
// with the public exponent, a standard probabilistic algorithm recovers them quickly.
public class RSAPrivateKeySpec implements KeySpec {

    private final BigInteger modulus;
    private final BigInteger privateExponent;
    private final AlgorithmParameterSpec params;

    public RSAPrivateKeySpec(BigInteger modulus, BigInteger privateExponent) {
        this(modulus, privateExponent, null);
    }

    public RSAPrivateKeySpec(BigInteger modulus, BigInteger privateExponent,
                             AlgorithmParameterSpec params) {
        this.modulus = modulus;
        this.privateExponent = privateExponent;
        this.params = params;
    }

    public BigInteger getModulus() {
        return this.modulus;
    }

    public BigInteger getPrivateExponent() {
        return this.privateExponent;
    }

    public AlgorithmParameterSpec getParams() {
        return this.params;
    }
}
