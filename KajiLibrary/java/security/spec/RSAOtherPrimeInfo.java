package java.security.spec;

import java.math.BigInteger;

// El tercer primo en adelante de una clave RSA multi-primo (el `OtherPrimeInfo` de PKCS#1).
//
// RSA no exige que el modulo sea producto de exactamente dos primos: con k primos el CRT se hace en
// k ramas de n/k bits cada una, y el trabajo baja mas todavia. Casi nadie lo usa porque con mas
// factores cada uno es mas chico, y un factor chico es mas facil de encontrar: la ganancia de
// velocidad se paga en margen de seguridad.
//
// Los tres valores se validan contra null y ahi termina: como en el resto de las specs de RSA, no
// hay verificacion aritmetica posible que sea barata.
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

    // `final` en los tres: una subclase que devolviera otra cosa haria que el CRT calcule mal, y un
    // CRT que calcula mal en RSA no da un resultado incorrecto sino una firma que revela la clave.
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
