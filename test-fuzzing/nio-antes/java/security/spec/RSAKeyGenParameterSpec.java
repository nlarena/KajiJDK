package java.security.spec;

import java.math.BigInteger;

// Which RSA key to generate: how many bits for the modulus and with which public exponent.
//
// The two predefined values are the Fermat primes F0 = 3 and F4 = 65537. Being prime and having few
// bits set is what makes them useful: public encryption is an exponentiation by e, and with
// e = 65537 = 2^16 + 1 it is seventeen steps. F0 = 3 is cheaper still but discouraged: with a small
// e and no proper padding, a short message encrypted to three different recipients is recovered
// without factoring anything (Hastad's attack). F4 is everyone's default.
public class RSAKeyGenParameterSpec implements AlgorithmParameterSpec {

    public static final BigInteger F0 = BigInteger.valueOf(3);
    public static final BigInteger F4 = BigInteger.valueOf(65537);

    private final int keysize;
    private final BigInteger publicExponent;
    private final AlgorithmParameterSpec keyParams;

    public RSAKeyGenParameterSpec(int keysize, BigInteger publicExponent) {
        this(keysize, publicExponent, null);
    }

    public RSAKeyGenParameterSpec(int keysize, BigInteger publicExponent,
                                  AlgorithmParameterSpec keyParams) {
        this.keysize = keysize;
        this.publicExponent = publicExponent;
        this.keyParams = keyParams;
    }

    // The size of the modulus in bits.
    public int getKeysize() {
        return this.keysize;
    }

    public BigInteger getPublicExponent() {
        return this.publicExponent;
    }

    // Parameters that stay attached to the generated key; for RSASSA-PSS, the `PSSParameterSpec`.
    public AlgorithmParameterSpec getKeyParams() {
        return this.keyParams;
    }
}
