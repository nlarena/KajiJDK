package java.security.spec;

import java.math.BigInteger;

// An RSA private key with the Chinese remainder theorem values (PKCS#1).
//
// Besides (n, d) it keeps the two primes p and q, the reduced exponents dP = d mod (p-1) and
// dQ = d mod (q-1), and the coefficient qInv = q^-1 mod p. With those a signature takes two
// exponentiations on numbers of half the bits instead of one on the full size, which comes out
// about four times cheaper.
//
// The price of that optimization is historic and worth naming: if one of the two halves of the CRT
// is computed wrongly —a bit flipped by a hardware fault, or one induced on purpose— the resulting
// signature lets n be factored with a single `gcd`. It is the Bellcore attack, and it is why every
// serious CRT-RSA implementation verifies the signature before returning it.
//
// Keeping p and q is also why this spec is more sensitive than its base class: whoever holds it
// holds the factorization of the modulus, which is everything.
public class RSAPrivateCrtKeySpec extends RSAPrivateKeySpec {

    private final BigInteger publicExponent;
    private final BigInteger primeP;
    private final BigInteger primeQ;
    private final BigInteger primeExponentP;
    private final BigInteger primeExponentQ;
    private final BigInteger crtCoefficient;

    public RSAPrivateCrtKeySpec(BigInteger modulus,
                                BigInteger publicExponent,
                                BigInteger privateExponent,
                                BigInteger primeP,
                                BigInteger primeQ,
                                BigInteger primeExponentP,
                                BigInteger primeExponentQ,
                                BigInteger crtCoefficient) {
        this(modulus, publicExponent, privateExponent, primeP, primeQ,
             primeExponentP, primeExponentQ, crtCoefficient, null);
    }

    public RSAPrivateCrtKeySpec(BigInteger modulus,
                                BigInteger publicExponent,
                                BigInteger privateExponent,
                                BigInteger primeP,
                                BigInteger primeQ,
                                BigInteger primeExponentP,
                                BigInteger primeExponentQ,
                                BigInteger crtCoefficient,
                                AlgorithmParameterSpec keyParams) {
        super(modulus, privateExponent, keyParams);
        this.publicExponent = publicExponent;
        this.primeP = primeP;
        this.primeQ = primeQ;
        this.primeExponentP = primeExponentP;
        this.primeExponentQ = primeExponentQ;
        this.crtCoefficient = crtCoefficient;
    }

    // The public exponent: it is kept in the private key too because it is needed to verify one's
    // own signature before handing it over, which is the defence against the Bellcore attack.
    public BigInteger getPublicExponent() {
        return this.publicExponent;
    }

    public BigInteger getPrimeP() {
        return this.primeP;
    }

    public BigInteger getPrimeQ() {
        return this.primeQ;
    }

    // d mod (p-1).
    public BigInteger getPrimeExponentP() {
        return this.primeExponentP;
    }

    // d mod (q-1).
    public BigInteger getPrimeExponentQ() {
        return this.primeExponentQ;
    }

    // q^-1 mod p.
    public BigInteger getCrtCoefficient() {
        return this.crtCoefficient;
    }
}
