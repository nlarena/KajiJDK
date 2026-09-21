package java.security.spec;

import java.math.BigInteger;

// An RSA public key in the clear: the modulus n and the public exponent e.
//
// It **validates nothing**, and it is not an oversight: it is what the JDK does, to the point of
// accepting both arguments as null. The reason is that there is no cheap validation that helps
// —checking that n is a product of two primes is the problem RSA assumes hard— and a partial
// validation would give a false sense that the key was checked. Whoever builds the key from this
// spec is the one who decides whether to accept it.
public class RSAPublicKeySpec implements KeySpec {

    private final BigInteger modulus;
    private final BigInteger publicExponent;
    private final AlgorithmParameterSpec params;

    public RSAPublicKeySpec(BigInteger modulus, BigInteger publicExponent) {
        this(modulus, publicExponent, null);
    }

    // The overload with parameters exists because of RSASSA-PSS: there the key is not only (n, e)
    // but also which hash and which salt length are used, and that information travels inside the
    // key.
    public RSAPublicKeySpec(BigInteger modulus, BigInteger publicExponent,
                            AlgorithmParameterSpec params) {
        this.modulus = modulus;
        this.publicExponent = publicExponent;
        this.params = params;
    }

    public BigInteger getModulus() {
        return this.modulus;
    }

    public BigInteger getPublicExponent() {
        return this.publicExponent;
    }

    // The algorithm parameters, or null if the key carries none.
    public AlgorithmParameterSpec getParams() {
        return this.params;
    }
}
