package java.security.interfaces;

import java.math.BigInteger;

// Una clave privada RSA que ademas expone los valores del teorema chino del resto.
//
// Es la contraparte de `RSAPrivateCrtKeySpec` del lado de las claves: lo mismo que ahi se explica
// sobre por que el CRT vale la pena y por que hay que verificar la firma antes de entregarla vale
// igual aca.
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
