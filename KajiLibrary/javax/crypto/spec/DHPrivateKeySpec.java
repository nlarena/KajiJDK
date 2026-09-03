package javax.crypto.spec;

import java.math.BigInteger;
import java.security.spec.KeySpec;

/**
 * Una clave privada Diffie-Hellman: el exponente `x` mas los parametros con los que se usa.
 *
 * <p>Vale la misma nota que {@link DHPublicKeySpec} sobre por que los parametros viajan adentro.
 */
public class DHPrivateKeySpec implements KeySpec {

    private final BigInteger x;
    private final BigInteger p;
    private final BigInteger g;

    /** El exponente privado `x`, con su primo y su generador. */
    public DHPrivateKeySpec(BigInteger x, BigInteger p, BigInteger g) {
        this.x = x;
        this.p = p;
        this.g = g;
    }

    /** El exponente privado. */
    public BigInteger getX() {
        return this.x;
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
