package java.security.interfaces;

import java.math.BigInteger;
import java.security.spec.RSAOtherPrimeInfo;

// An RSA private key with CRT and more than two primes.
//
// It does not extend `RSAPrivateCrtKey` even though it declares the same six methods, and the
// repetition matters: a key of k primes is not a two-prime key, and letting it pass as one would
// make code that only looks at p and q believe it has the complete factorization when primes are
// missing.
public interface RSAMultiPrimePrivateCrtKey extends RSAPrivateKey {

    long serialVersionUID = 618058533534628008L;

    BigInteger getPublicExponent();

    BigInteger getPrimeP();

    BigInteger getPrimeQ();

    BigInteger getPrimeExponentP();

    BigInteger getPrimeExponentQ();

    BigInteger getCrtCoefficient();

    // The primes from the third on, with their CRT values, or null if there are only two.
    RSAOtherPrimeInfo[] getOtherPrimeInfo();
}
