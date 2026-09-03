package java.security.spec;

import java.math.BigInteger;

// Una clave privada RSA con CRT y **mas de dos** primos.
//
// Es `RSAPrivateCrtKeySpec` extendido con la lista de primos del tercero en adelante. A diferencia
// de las otras specs de RSA, esta si valida contra null los ocho `BigInteger`, y la razon es que
// aca la lista de primos hace que la estructura tenga una forma que respetar: un arreglo vacio
// significaria "multi-primo con cero primos extra", que es una contradiccion, y por eso se rechaza.
// Un arreglo **null** en cambio se acepta —quiere decir que no hay primos extra— y eso queda
// documentado porque no es lo que uno esperaria de un constructor que rechaza el arreglo vacio.
//
// No hereda de `RSAPrivateCrtKeySpec` sino de `RSAPrivateKeySpec`, y esta bien que asi sea: una
// clave de k primos no **es** una clave de dos primos, y dejar que se pasara por una haria que
// codigo que solo mira p y q operara con una factorizacion incompleta.
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
            this.otherPrimeInfo = copiar(otherPrimeInfo);
        }
    }

    private static RSAOtherPrimeInfo[] copiar(RSAOtherPrimeInfo[] a) {
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

    // Copia del arreglo, o null si no hay primos extra. La copia es superficial y alcanza porque
    // `RSAOtherPrimeInfo` es inmutable.
    public RSAOtherPrimeInfo[] getOtherPrimeInfo() {
        if (this.otherPrimeInfo == null) {
            return null;
        }
        return copiar(this.otherPrimeInfo);
    }
}
