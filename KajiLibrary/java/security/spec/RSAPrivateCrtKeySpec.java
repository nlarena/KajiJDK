package java.security.spec;

import java.math.BigInteger;

// Una clave privada RSA con los valores del teorema chino del resto (PKCS#1).
//
// Ademas de (n, d) guarda los dos primos p y q, los exponentes reducidos dP = d mod (p-1) y
// dQ = d mod (q-1), y el coeficiente qInv = q^-1 mod p. Con eso se firma haciendo dos
// exponenciaciones sobre numeros de la mitad de bits en lugar de una sobre el doble, que sale
// aproximadamente cuatro veces mas barato.
//
// El precio de esa optimizacion es historico y vale nombrarlo: si una de las dos mitades del CRT se
// calcula mal —un bit que se da vuelta por un fallo de hardware o inducido a proposito— la firma
// resultante permite factorizar n con un solo `gcd`. Es el ataque de Bellcore, y es la razon por la
// que toda implementacion seria de CRT-RSA verifica la firma antes de devolverla.
//
// Guardar p y q es tambien la razon por la que esta spec es mas sensible que su clase base: quien la
// tenga tiene la factorizacion del modulo, que es todo.
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

    // El exponente publico: se guarda tambien en la privada porque hace falta para verificar la
    // propia firma antes de entregarla, que es la defensa contra el ataque de Bellcore.
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
