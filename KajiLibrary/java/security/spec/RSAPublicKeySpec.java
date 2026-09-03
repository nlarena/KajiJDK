package java.security.spec;

import java.math.BigInteger;

// Una clave publica RSA en claro: el modulo n y el exponente publico e.
//
// **No valida nada**, y no es un olvido: es lo que hace el JDK, hasta el punto de aceptar los dos
// argumentos en null. La razon es que no hay validacion barata que sirva —comprobar que n sea
// producto de dos primos es el problema que RSA supone dificil— y una validacion parcial daria una
// falsa sensacion de que la clave se reviso. Quien construye la clave a partir de esta spec es el
// que decide si la acepta.
public class RSAPublicKeySpec implements KeySpec {

    private final BigInteger modulus;
    private final BigInteger publicExponent;
    private final AlgorithmParameterSpec params;

    public RSAPublicKeySpec(BigInteger modulus, BigInteger publicExponent) {
        this(modulus, publicExponent, null);
    }

    // La sobrecarga con parametros existe por RSASSA-PSS: ahi la clave no es solo (n, e) sino
    // tambien que hash y que largo de sal se usan, y esa informacion viaja dentro de la clave.
    public RSAPublicKeySpec(BigInteger modulus, BigInteger publicExponent,
                            AlgorithmParameterSpec params) {
        this.modulus = modulus;
        this.publicExponent = publicExponent;
        this.params = params;
    }

    public BigInteger getModulus() {
        return this.modulus;
    }

    public BigInteger getPublicExponent() {
        return this.publicExponent;
    }

    // Los parametros del algoritmo, o null si la clave no los lleva.
    public AlgorithmParameterSpec getParams() {
        return this.params;
    }
}
