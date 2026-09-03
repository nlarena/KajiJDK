package javax.crypto.spec;

import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Los parametros publicos de Diffie-Hellman: el primo `p`, el generador `g` y, opcionalmente, el
 * largo del exponente privado.
 *
 * <p>`l` en cero significa "sin restriccion", que es distinto de "cero bits": el constructor de dos
 * argumentos lo deja asi. No hay forma de pedir un exponente de cero bits, y no la hay porque no
 * tendria sentido.
 *
 * <p>No valida que `p` sea primo. Comprobarlo es caro --una prueba probabilistica sobre un numero
 * de dos mil bits-- y el JDK tampoco lo hace: quien genera los parametros es responsable de eso.
 */
public class DHParameterSpec implements AlgorithmParameterSpec {

    private final BigInteger p;
    private final BigInteger g;
    private final int l;

    /** Sin restriccion sobre el largo del exponente privado. */
    public DHParameterSpec(BigInteger p, BigInteger g) {
        this.p = p;
        this.g = g;
        this.l = 0;
    }

    /** Con el exponente privado limitado a `l` bits. */
    public DHParameterSpec(BigInteger p, BigInteger g, int l) {
        this.p = p;
        this.g = g;
        this.l = l;
    }

    /** El primo. */
    public BigInteger getP() {
        return this.p;
    }

    /** El generador. */
    public BigInteger getG() {
        return this.g;
    }

    /** El largo del exponente privado en bits, o cero si no hay restriccion. */
    public int getL() {
        return this.l;
    }
}
