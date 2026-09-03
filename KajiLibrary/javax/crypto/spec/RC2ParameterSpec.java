package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * Los parametros de RC2: los bits efectivos de la clave y, si el modo lo pide, un IV de ocho bytes.
 *
 * <p>Los "bits efectivos" son una particularidad de RC2 que conviene entender: la clave puede tener
 * el largo que sea, pero el algoritmo la expande a una fuerza declarada aparte. Una clave de 128
 * bits con `effectiveKeyBits` en 40 es una clave de 40 bits -- el numero de arriba es el que manda.
 *
 * <p>Tiene `equals` y `hashCode` propios, que la mayoria de las clases de este paquete no tienen.
 * No es un descuido de las otras: es que RC2 se usa en formatos donde hay que comparar dos juegos
 * de parametros para decidir si describen el mismo cifrado.
 */
public class RC2ParameterSpec implements AlgorithmParameterSpec {

    private static final int IV_LEN = 8;

    private final int effectiveKeyBits;
    private final byte[] iv;

    /** Sin IV: para los modos que no lo usan. */
    public RC2ParameterSpec(int effectiveKeyBits) {
        this.effectiveKeyBits = effectiveKeyBits;
        this.iv = null;
    }

    /**
     * @throws IllegalArgumentException si el IV es nulo o tiene menos de ocho bytes
     */
    public RC2ParameterSpec(int effectiveKeyBits, byte[] iv) {
        this(effectiveKeyBits, iv, 0);
    }

    /**
     * El IV son los ocho bytes a partir de `offset`.
     *
     * @throws IllegalArgumentException si el IV es nulo o quedan menos de ocho bytes desde `offset`
     */
    public RC2ParameterSpec(int effectiveKeyBits, byte[] iv, int offset) {
        if (iv == null) {
            throw new IllegalArgumentException("el IV no puede ser nulo");
        }
        if (iv.length - offset < IV_LEN) {
            throw new IllegalArgumentException(
                    "el IV de RC2 son " + IV_LEN + " bytes desde el offset");
        }
        this.effectiveKeyBits = effectiveKeyBits;
        this.iv = IvParameterSpec.copy(iv, offset, IV_LEN);
    }

    /** Los bits efectivos. Ver la nota de la clase. */
    public int getEffectiveKeyBits() {
        return this.effectiveKeyBits;
    }

    /** Una copia del IV, o nulo si no tiene. */
    public byte[] getIV() {
        return this.iv == null ? null : IvParameterSpec.copy(this.iv, 0, IV_LEN);
    }

    /** Igualdad por bits efectivos e IV. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RC2ParameterSpec)) {
            return false;
        }
        RC2ParameterSpec other = (RC2ParameterSpec) obj;
        return this.effectiveKeyBits == other.effectiveKeyBits
                && Arrays.equals(this.iv, other.iv);
    }

    public int hashCode() {
        int h = 0;
        if (this.iv != null) {
            for (int i = 0; i < this.iv.length; i++) {
                h = h + this.iv[i] * i;
            }
        }
        return h + this.effectiveKeyBits;
    }
}
