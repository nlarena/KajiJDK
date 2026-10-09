package java.security.spec;

import java.math.BigInteger;

// An RSA private key with CRT and **more than two** primes.
//
// It is `RSAPrivateCrtKeySpec` extended with the list of primes from the third on. Unlike the other
// RSA key specs, this one does validate the eight `BigInteger`s against null, and here the list of
// primes gives the structure a shape to respect: an empty array would mean "multi-prime with zero
// extra primes", which is a contradiction, and so it is rejected. A **null** array, on the other
// hand, is accepted —it means there are no extra primes— and that is documented because it is not
// what one would expect from a constructor that rejects the empty array.
//
// It does not inherit from `RSAPrivateCrtKeySpec` but from `RSAPrivateKeySpec`, and rightly so: a
// key of k primes **is not** a two-prime key, and letting it pass as one would make code that only
// looks at p and q operate with an incomplete factorization.
public class RSAMultiPrimePrivateCrtKeySpec extends RSAPrivateKeySpec {

    private final BigInteger publicExponent;
    private final BigInteger primeP;
    private final BigInteger primeQ;
    private final BigInteger primeExponentP;
    private final BigInteger primeExponentQ;
    private final BigInteger crtCoefficient;
    private final RSAOtherPrimeInfo[] otherPrimeInfo;

    public RSAMultiPrimePrivateCrtKeySpec(BigInteger modulus,
                                          BigInteger publicExponent,
                                          BigInteger privateExponent,
                                          BigInteger primeP,
                                          BigInteger primeQ,
                                          BigInteger primeExponentP,
                                          BigInteger primeExponentQ,
                                          BigInteger crtCoefficient,
                                          RSAOtherPrimeInfo[] otherPrimeInfo) {
        this(modulus, publicExponent, privateExponent, primeP, primeQ,
             primeExponentP, primeExponentQ, crtCoefficient, otherPrimeInfo, null);
    }

    public RSAMultiPrimePrivateCrtKeySpec(BigInteger modulus,
                                          BigInteger publicExponent,
                                          BigInteger privateExponent,
                                          BigInteger primeP,
                                          BigInteger primeQ,
                                          BigInteger primeExponentP,
                                          BigInteger primeExponentQ,
                                          BigInteger crtCoefficient,
                                          RSAOtherPrimeInfo[] otherPrimeInfo,
                                          AlgorithmParameterSpec keyParams) {
        super(modulus, privateExponent, keyParams);
        if (modulus == null) {
            throw new NullPointerException("the modulus parameter must be non-null");
        }
        if (publicExponent == null) {
            throw new NullPointerException("the publicExponent parameter must be non-null");
        }
        if (privateExponent == null) {
            throw new NullPointerException("the privateExponent parameter must be non-null");
        }
        if (primeP == null) {
            throw new NullPointerException("the primeP parameter must be non-null");
        }
        if (primeQ == null) {
            throw new NullPointerException("the primeQ parameter must be non-null");
        }
        if (primeExponentP == null) {
            throw new NullPointerException("the primeExponentP parameter must be non-null");
        }
        if (primeExponentQ == null) {
            throw new NullPointerException("the primeExponentQ parameter must be non-null");
        }
        if (crtCoefficient == null) {
            throw new NullPointerException("the crtCoefficient parameter must be non-null");
        }
        this.publicExponent = publicExponent;
        this.primeP = primeP;
        this.primeQ = primeQ;
        this.primeExponentP = primeExponentP;
        this.primeExponentQ = primeExponentQ;
        this.crtCoefficient = crtCoefficient;
        if (otherPrimeInfo == null) {
            this.otherPrimeInfo = null;
        } else if (otherPrimeInfo.length == 0) {
            throw new IllegalArgumentException("the otherPrimeInfo parameter must not be empty");
        } else {
            this.otherPrimeInfo = copyOf(otherPrimeInfo);
        }
    }

    private static RSAOtherPrimeInfo[] copyOf(RSAOtherPrimeInfo[] a) {
        RSAOtherPrimeInfo[] c = new RSAOtherPrimeInfo[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    public BigInteger getPublicExponent() {
        return this.publicExponent;
    }

    public BigInteger getPrimeP() {
        return this.primeP;
    }

    public BigInteger getPrimeQ() {
        return this.primeQ;
    }

    public BigInteger getPrimeExponentP() {
        return this.primeExponentP;
    }

    public BigInteger getPrimeExponentQ() {
        return this.primeExponentQ;
    }

    public BigInteger getCrtCoefficient() {
        return this.crtCoefficient;
    }

    // A copy of the array, or null if there are no extra primes. The copy is shallow, and that is
    // enough because `RSAOtherPrimeInfo` is immutable.
    public RSAOtherPrimeInfo[] getOtherPrimeInfo() {
        if (this.otherPrimeInfo == null) {
            return null;
        }
        return copyOf(this.otherPrimeInfo);
    }
}
