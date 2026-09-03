package java.security.spec;

import java.math.BigInteger;

// Un punto de una curva de Edwards, en la forma comprimida de RFC 8032: la coordenada y entera, mas
// **un solo bit** de x.
//
// Que alcance con un bit es la propiedad que hace compacto a Ed25519. La ecuacion de la curva
// determina x^2 a partir de y, asi que quedan dos candidatos, x y -x; saber si x es par o impar
// elige uno. Una clave publica Ed25519 son entonces 32 bytes en vez de 64.
//
// Esta clase no descomprime: no calcula x a partir de y, porque para eso hace falta una raiz
// cuadrada modular sobre la curva concreta, que aca no esta. Guarda los dos datos y los devuelve.
public final class EdECPoint {

    private final boolean xOdd;
    private final BigInteger y;

    public EdECPoint(boolean xOdd, BigInteger y) {
        if (y == null) {
            throw new NullPointerException("y must not be null");
        }
        this.xOdd = xOdd;
        this.y = y;
    }

    // Si la coordenada x es impar: el bit que desempata entre x y -x.
    public boolean isXOdd() {
        return this.xOdd;
    }

    public BigInteger getY() {
        return this.y;
    }
}
