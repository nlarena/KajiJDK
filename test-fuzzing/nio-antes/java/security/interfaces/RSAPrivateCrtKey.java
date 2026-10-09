package java.security.interfaces;

import java.math.BigInteger;

// An RSA private key that also exposes the Chinese remainder theorem values.
//
// It is the key-side counterpart of `RSAPrivateCrtKeySpec`: what is explained there about why CRT
// is worth it, and why the signature has to be verified before handing it out, applies here as
// well.
public interface RSAPrivateCrtKey extends RSAPrivateKey {

    long serialVersionUID = -5682214253527700368L;

    BigInteger getPublicExponent();

    BigInteger getPrimeP();

    BigInteger getPrimeQ();

    // d mod (p-1).
    BigInteger getPrimeExponentP();

    // d mod (q-1).
    BigInteger getPrimeExponentQ();

    // q^-1 mod p.
    BigInteger getCrtCoefficient();
}
