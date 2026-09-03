package java.security.spec;

import java.math.BigInteger;

// Una clave privada DSA en claro: x, mas los parametros p, q, g.
public class DSAPrivateKeySpec implements KeySpec {

    private final BigInteger x;
    private final BigInteger p;
    private final BigInteger q;
    private final BigInteger g;

    public DSAPrivateKeySpec(BigInteger x, BigInteger p, BigInteger q, BigInteger g) {
        this.x = x;
        this.p = p;
        this.q = q;
        this.g = g;
    }

    // El exponente privado x.
    public BigInteger getX() {
        return this.x;
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
