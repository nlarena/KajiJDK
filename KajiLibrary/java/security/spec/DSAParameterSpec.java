package java.security.spec;

import java.math.BigInteger;
import java.security.interfaces.DSAParams;

// Los parametros de dominio de DSA como spec: p, q y g.
//
// Implementa `AlgorithmParameterSpec` y `DSAParams` a la vez, y eso no es redundancia: la primera la
// hace pasable a `AlgorithmParameters` y a los generadores, la segunda la hace pasable donde se
// espera los parametros de una clave DSA concreta. Es el punto donde el mundo "esto es una
// descripcion" y el mundo "esto son los parametros de esa clave" se tocan.
//
// No valida que q divida a p-1 ni que g genere el subgrupo correcto. Igual que en el resto del
// paquete: es un contenedor, y las comprobaciones que importan cuestan exponenciaciones modulares
// que un constructor no deberia hacer.
public class DSAParameterSpec implements AlgorithmParameterSpec, DSAParams {

    private final BigInteger p;
    private final BigInteger q;
    private final BigInteger g;

    public DSAParameterSpec(BigInteger p, BigInteger q, BigInteger g) {
        this.p = p;
        this.q = q;
        this.g = g;
    }

    public BigInteger getP() {
        return this.p;
    }

    public BigInteger getQ() {
        return this.q;
    }

    public BigInteger getG() {
        return this.g;
    }
}
