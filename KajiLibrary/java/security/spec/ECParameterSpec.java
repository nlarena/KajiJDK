package java.security.spec;

import java.math.BigInteger;

// Los parametros de dominio de ECC: la curva, el punto generador, el orden de ese generador y el
// cofactor.
//
// Los cuatro juntos son lo que dos partes tienen que compartir para que una clave publica signifique
// lo mismo de los dos lados. Una clave EC sin estos parametros no es interpretable: el mismo par
// (x, y) es un punto valido en infinitas curvas distintas.
//
// El cofactor h = |E| / n no es un detalle contable. Es la razon por la que existen los ataques de
// subgrupo chico: si h > 1, un punto que el atacante manda puede vivir en un subgrupo de orden
// pequeño y filtrar la clave privada modulo ese orden. Por eso las curvas serias lo tienen en 1 y
// por eso el valor viaja con los parametros en lugar de deducirse.
public class ECParameterSpec implements AlgorithmParameterSpec {

    private final EllipticCurve curve;
    private final ECPoint g;
    private final BigInteger n;
    private final int h;

    public ECParameterSpec(EllipticCurve curve, ECPoint g, BigInteger n, int h) {
        if (curve == null) {
            throw new NullPointerException("curve is null");
        }
        if (g == null) {
            throw new NullPointerException("generator is null");
        }
        if (n == null) {
            throw new NullPointerException("order is null");
        }
        if (n.signum() != 1) {
            throw new IllegalArgumentException("n is not positive");
        }
        if (h <= 0) {
            throw new IllegalArgumentException("h is not positive");
        }
        this.curve = curve;
        this.g = g;
        this.n = n;
        this.h = h;
    }

    public EllipticCurve getCurve() {
        return this.curve;
    }

    public ECPoint getGenerator() {
        return this.g;
    }

    // El orden del generador: el n mas chico tal que n*G es el infinito.
    public BigInteger getOrder() {
        return this.n;
    }

    public int getCofactor() {
        return this.h;
    }
}
