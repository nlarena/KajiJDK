package java.security.spec;

import java.math.BigInteger;
import java.util.Arrays;

// El cuerpo binario GF(2^m): polinomios sobre GF(2) modulo un polinomio de reduccion irreducible.
//
// Un elemento es un polinomio de grado < m, y la reduccion se hace modulo otro polinomio de grado
// exactamente m. Ese polinomio se puede dar de tres formas y las tres describen lo mismo:
//
//   - sin polinomio: cuerpo "generico", sin base fijada. No se puede operar, pero sirve para decir
//     de que tamaño es el cuerpo.
//   - como `BigInteger`: el bit i prendido significa que el termino x^i esta.
//   - como los indices de los terminos del medio: para un trinomio x^m + x^k + 1 es {k}, para un
//     pentanomio x^m + x^k3 + x^k2 + x^k1 + 1 es {k3, k2, k1}.
//
// Las dos ultimas se convierten una en la otra en el constructor, asi que despues de construir
// ambos accesores responden, den por donde den. Por eso `equals` compara solo m y los indices: el
// `BigInteger` es redundante y compararlo tambien seria trabajo de mas por el mismo resultado.
//
// Solo se aceptan trinomios y pentanomios (bitCount 3 o 5). No es una limitacion de esta clase
// sino de lo que la practica usa: los estandares eligen siempre uno de los dos porque la reduccion
// es barata, y aceptar un polinomio arbitrario abriria la puerta a uno reducible, que no genera un
// cuerpo.
public class ECFieldF2m implements ECField {

    private final int m;

    // El polinomio de reduccion como bits, o null si el cuerpo se creo sin base.
    private final BigInteger rp;

    // Los indices de los terminos del medio, en orden **descendente**. Null si no hay polinomio.
    private final int[] ks;

    // Cuerpo sin base fijada: se conoce el tamaño y nada mas.
    public ECFieldF2m(int m) {
        if (m <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        this.m = m;
        this.rp = null;
        this.ks = null;
    }

    public ECFieldF2m(int m, BigInteger rp) {
        if (m <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        // Sin `rp` no hay nada que validar: que reviente aca con NPE es lo mismo que hace el JDK.
        int cuenta = rp.bitCount();
        // El termino independiente y el de grado m tienen que estar: el primero porque sin el el
        // polinomio es divisible por x —o sea reducible—, el segundo porque es lo que fija el grado.
        if (!rp.testBit(0) || !rp.testBit(m) || ((cuenta != 3) && (cuenta != 5))) {
            throw new IllegalArgumentException("rp does not represent a valid reduction polynomial");
        }
        this.m = m;
        this.rp = rp;
        // Se sacan los dos extremos y quedan justo los terminos del medio.
        BigInteger resto = rp.clearBit(0).clearBit(m);
        this.ks = new int[cuenta - 2];
        // Se llena de atras para adelante porque `getLowestSetBit` devuelve los indices de menor a
        // mayor y el contrato pide orden descendente.
        for (int i = this.ks.length - 1; i >= 0; i--) {
            int indice = resto.getLowestSetBit();
            this.ks[i] = indice;
            resto = resto.clearBit(indice);
        }
    }

    public ECFieldF2m(int m, int[] ks) {
        if (m <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        int[] copia = new int[ks.length];
        System.arraycopy(ks, 0, copia, 0, ks.length);
        if ((copia.length != 1) && (copia.length != 3)) {
            throw new IllegalArgumentException("length of ks is neither 1 nor 3");
        }
        for (int i = 0; i < copia.length; i++) {
            // Un termino del medio con indice 0 o m seria uno de los extremos, que van implicitos.
            if ((copia[i] < 1) || (copia[i] > m - 1)) {
                throw new IllegalArgumentException("ks[" + i + "] is out of range");
            }
            if ((i != 0) && (copia[i] >= copia[i - 1])) {
                throw new IllegalArgumentException("values in ks are not in descending order");
            }
        }
        this.m = m;
        this.ks = copia;
        BigInteger p = BigInteger.ONE.setBit(m);
        for (int i = 0; i < copia.length; i++) {
            p = p.setBit(copia[i]);
        }
        this.rp = p;
    }

    // En un cuerpo binario un elemento son exactamente m bits, sin importar el polinomio.
    @Override
    public int getFieldSize() {
        return this.m;
    }

    public int getM() {
        return this.m;
    }

    // El polinomio de reduccion, o null si el cuerpo se creo sin base.
    public BigInteger getReductionPolynomial() {
        return this.rp;
    }

    // Copia de los indices de los terminos del medio, o null si no hay polinomio.
    public int[] getMidTermsOfReductionPolynomial() {
        if (this.ks == null) {
            return null;
        }
        int[] c = new int[this.ks.length];
        System.arraycopy(this.ks, 0, c, 0, this.ks.length);
        return c;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ECFieldF2m) {
            ECFieldF2m otro = (ECFieldF2m) obj;
            // No hace falta mirar `rp`: es funcion de m y ks.
            return (this.m == otro.m) && Arrays.equals(this.ks, otro.ks);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (this.m * 31) + (this.rp == null ? 0 : this.rp.hashCode());
    }
}
