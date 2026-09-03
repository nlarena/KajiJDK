package java.security.spec;

import java.math.BigInteger;

// Un punto de una curva eliptica en coordenadas afines.
//
// Deliberadamente **no** sabe a que curva pertenece: el par (x, y) solo tiene sentido junto a un
// `EllipticCurve`, y separarlos es lo que permite que la misma clase sirva para el generador de un
// `ECParameterSpec` y para la clave publica de un `ECPublicKeySpec` sin duplicar el tipo.
//
// Esta clase no hace aritmetica de curva —no suma puntos ni multiplica por escalares— y eso es a
// proposito: es un descriptor, no una implementacion de ECC. Sumar dos puntos requiere conocer la
// curva, que aca no esta.
public class ECPoint {

    // El punto en el infinito: el neutro del grupo. Se representa con las dos coordenadas en null
    // porque **no tiene** coordenadas afines; no es un (0, 0) ni ningun otro par concreto. Esa
    // ausencia es lo que obliga a que `equals` y `hashCode` lo traten aparte.
    public static final ECPoint POINT_INFINITY = new ECPoint();

    private final BigInteger x;
    private final BigInteger y;

    // Constructor privado, solo para POINT_INFINITY: es la unica forma legitima de tener un ECPoint
    // con coordenadas nulas.
    private ECPoint() {
        this.x = null;
        this.y = null;
    }

    public ECPoint(BigInteger x, BigInteger y) {
        if ((x == null) || (y == null)) {
            throw new NullPointerException("affine coordinate x or y is null");
        }
        this.x = x;
        this.y = y;
    }

    // La coordenada x, o null si este es el punto en el infinito.
    public BigInteger getAffineX() {
        return this.x;
    }

    // La coordenada y, o null si este es el punto en el infinito.
    public BigInteger getAffineY() {
        return this.y;
    }

    // El infinito solo es igual a si mismo. El chequeo va primero porque comparar sus coordenadas
    // nulas contra las de otro punto seria un NPE, y porque dos infinitos siempre son la misma
    // instancia: la constante.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this == POINT_INFINITY) {
            return false;
        }
        if (obj instanceof ECPoint) {
            ECPoint otro = (ECPoint) obj;
            return this.x.equals(otro.x) && this.y.equals(otro.y);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this == POINT_INFINITY) {
            return 0;
        }
        return this.x.hashCode() * 31 + this.y.hashCode();
    }
}
