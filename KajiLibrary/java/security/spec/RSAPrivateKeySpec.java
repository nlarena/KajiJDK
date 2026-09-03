package java.security.spec;

import java.math.BigInteger;

// Una clave privada RSA en su forma minima: el modulo n y el exponente privado d.
//
// Con (n, d) alcanza para descifrar y firmar, pero cuesta caro: una exponenciacion modular con un
// exponente del tamaño de n. `RSAPrivateCrtKeySpec` guarda ademas los factores para poder hacerlo
// por el teorema chino del resto, que es unas cuatro veces mas rapido. Que esta clase sea la base y
// la otra la subclase no es casualidad: lo que la subclase agrega es **redundante** —los factores se
// deducen de d, aunque no facilmente— y por eso es lo opcional.
public class RSAPrivateKeySpec implements KeySpec {

    private final BigInteger modulus;
    private final BigInteger privateExponent;
    private final AlgorithmParameterSpec params;

    public RSAPrivateKeySpec(BigInteger modulus, BigInteger privateExponent) {
        this(modulus, privateExponent, null);
    }

    public RSAPrivateKeySpec(BigInteger modulus, BigInteger privateExponent,
                             AlgorithmParameterSpec params) {
        this.modulus = modulus;
        this.privateExponent = privateExponent;
        this.params = params;
    }

    public BigInteger getModulus() {
        return this.modulus;
    }

    public BigInteger getPrivateExponent() {
        return this.privateExponent;
    }

    public AlgorithmParameterSpec getParams() {
        return this.params;
    }
}
