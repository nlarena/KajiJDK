package java.security.spec;

import java.math.BigInteger;

// Una clave publica DSA en claro: y, mas los parametros p, q, g.
//
// Los parametros van sueltos en vez de venir en un `DSAParameterSpec` por antiguedad del API, no por
// diseño: esta clase es del JDK 1.2 y `DSAParameterSpec` es hermana, no anterior.
public class DSAPublicKeySpec implements KeySpec {

    private final BigInteger y;
    private final BigInteger p;
    private final BigInteger q;
    private final BigInteger g;

    public DSAPublicKeySpec(BigInteger y, BigInteger p, BigInteger q, BigInteger g) {
        this.y = y;
        this.p = p;
        this.q = q;
        this.g = g;
    }

    // El valor publico y = g^x mod p.
    public BigInteger getY() {
        return this.y;
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
