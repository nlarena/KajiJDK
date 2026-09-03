package java.security.spec;

import java.math.BigInteger;

// Un cuerpo primo GF(p): los enteros modulo un primo.
//
// La clase **no verifica que `p` sea primo**, y eso es del contrato del JDK, no una omision de
// aca: probar primalidad de un numero de 256 bits en un constructor que se llama por cada clave
// seria un costo que nadie pidio. Quien construye el cuerpo es responsable de que lo sea.
public class ECFieldFp implements ECField {

    private final BigInteger p;

    public ECFieldFp(BigInteger p) {
        if (p == null) {
            throw new NullPointerException("p is null");
        }
        if (p.signum() != 1) {
            throw new IllegalArgumentException("p is not positive");
        }
        this.p = p;
    }

    // El tamano en bits de `p`, que es lo que ocupa un elemento del cuerpo.
    public int getFieldSize() {
        return this.p.bitLength();
    }

    public BigInteger getP() {
        return this.p;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ECFieldFp)) {
            return false;
        }
        return this.p.equals(((ECFieldFp) obj).p);
    }

    @Override
    public int hashCode() {
        return this.p.hashCode();
    }
}
