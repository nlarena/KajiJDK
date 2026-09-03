package java.security.interfaces;

import java.math.BigInteger;
import java.security.spec.RSAOtherPrimeInfo;

// Una clave privada RSA con CRT y mas de dos primos.
//
// No extiende `RSAPrivateCrtKey` aunque declare los mismos seis metodos, y la repeticion es
// deliberada: una clave de k primos no es una clave de dos, y dejarla pasar por una haria que codigo
// que solo mira p y q creyera tener la factorizacion completa cuando le falta la mitad.
public interface RSAMultiPrimePrivateCrtKey extends RSAPrivateKey {

    long serialVersionUID = 618058533534628008L;

    BigInteger getPublicExponent();

    BigInteger getPrimeP();

    BigInteger getPrimeQ();

    BigInteger getPrimeExponentP();

    BigInteger getPrimeExponentQ();

    BigInteger getCrtCoefficient();

    // Los primos del tercero en adelante, o null si no hay.
    RSAOtherPrimeInfo[] getOtherPrimeInfo();
}
