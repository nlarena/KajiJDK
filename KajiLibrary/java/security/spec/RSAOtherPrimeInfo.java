package java.security.spec;

import java.math.BigInteger;

// The third prime onwards of a multi-prime RSA key (PKCS#1's `OtherPrimeInfo`).
//
// RSA does not require the modulus to be the product of exactly two primes: with k primes the CRT
// is done in k branches of n/k bits each, and the work drops further still. Almost nobody uses it
// because with more factors each one is smaller, and a small factor is easier to find: the speed
// gain is paid for in security margin.
//
// The three values are validated against null and that is where it ends: as in the rest of the RSA
// specs, there is no arithmetic check that is cheap.
public class RSAOtherPrimeInfo {

    private final BigInteger prime;
    private final BigInteger primeExponent;
    private final BigInteger crtCoefficient;

    public RSAOtherPrimeInfo(BigInteger prime, BigInteger primeExponent,
                             BigInteger crtCoefficient) {
        if (prime == null) {
            throw new NullPointerException("the prime parameter must be non-null");
        }
        if (primeExponent == null) {
            throw new NullPointerException("the primeExponent parameter must be non-null");
        }
        if (crtCoefficient == null) {
            throw new NullPointerException("the crtCoefficient parameter must be non-null");
        }
        this.prime = prime;
        this.primeExponent = primeExponent;
        this.crtCoefficient = crtCoefficient;
    }

    // `final` on all three: a subclass that returned something else would make the CRT compute
    // wrongly, and a CRT that computes wrongly in RSA does not give an incorrect result but a
    // signature that reveals the key.
    public final BigInteger getPrime() {
        return this.prime;
    }

    // d mod (prime-1).
    public final BigInteger getExponent() {
        return this.primeExponent;
    }

    public final BigInteger getCrtCoefficient() {
        return this.crtCoefficient;
    }
}
