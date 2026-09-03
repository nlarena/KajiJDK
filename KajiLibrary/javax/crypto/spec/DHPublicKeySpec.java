package javax.crypto.spec;

import java.math.BigInteger;
import java.security.spec.KeySpec;

/**
 * Una clave publica Diffie-Hellman: el valor `y` mas los parametros `p` y `g` con los que se
 * calculo.
 *
 * <p>Los parametros van adentro de la clave y no aparte porque una clave DH **no significa nada sin
 * ellos**: el mismo `y` con otro primo es otra clave. Es la diferencia con RSA, donde el modulo ya
 * viene en la clave.
 */
public class DHPublicKeySpec implements KeySpec {

    private final BigInteger y;
    private final BigInteger p;
    private final BigInteger g;

    /** El valor publico `y`, con su primo y su generador. */
    public DHPublicKeySpec(BigInteger y, BigInteger p, BigInteger g) {
        this.y = y;
        this.p = p;
        this.g = g;
    }

    /** El valor publico. */
    public BigInteger getY() {
        return this.y;
    }

    /** El primo. */
    public BigInteger getP() {
        return this.p;
    }

    /** El generador. */
    public BigInteger getG() {
        return this.g;
    }
}
