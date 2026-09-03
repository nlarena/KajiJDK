package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * Los parametros de RC5: version, rondas, tamano de palabra y, si el modo lo pide, un IV.
 *
 * <p>RC5 es una **familia** de cifrados y no uno solo: cambiando el tamano de palabra y las rondas
 * se obtienen algoritmos distintos e incompatibles entre si. Por eso estos tres numeros van en los
 * parametros y no estan fijos en el algoritmo.
 *
 * <p>El IV mide **dos palabras**, no un largo fijo: con palabras de 32 bits son ocho bytes y con
 * palabras de 64 son dieciseis. De ahi que la validacion dependa de `wordSize`, que es lo que la
 * distingue de la de {@link RC2ParameterSpec}.
 */
public class RC5ParameterSpec implements AlgorithmParameterSpec {

    private final int version;
    private final int rounds;
    private final int wordSize;
    private final byte[] iv;

    /** Sin IV: para los modos que no lo usan. `wordSize` esta en bits. */
    public RC5ParameterSpec(int version, int rounds, int wordSize) {
        this.version = version;
        this.rounds = rounds;
        this.wordSize = wordSize;
        this.iv = null;
    }

    /**
     * @throws IllegalArgumentException si el IV es nulo o no mide dos palabras
     */
    public RC5ParameterSpec(int version, int rounds, int wordSize, byte[] iv) {
        this(version, rounds, wordSize, iv, 0);
    }

    /**
     * El IV son las dos palabras a partir de `offset`.
     *
     * @throws IllegalArgumentException si el IV es nulo o quedan menos de dos palabras desde
     *     `offset`
     */
    public RC5ParameterSpec(int version, int rounds, int wordSize, byte[] iv, int offset) {
        if (iv == null) {
            throw new IllegalArgumentException("el IV no puede ser nulo");
        }
        int len = (wordSize / 8) * 2;
        if (iv.length - offset < len) {
            throw new IllegalArgumentException(
                    "el IV de RC5 son dos palabras (" + len + " bytes) desde el offset");
        }
        this.version = version;
        this.rounds = rounds;
        this.wordSize = wordSize;
        this.iv = IvParameterSpec.copy(iv, offset, len);
    }

    /** La version del algoritmo. */
    public int getVersion() {
        return this.version;
    }

    /** Cuantas rondas. */
    public int getRounds() {
        return this.rounds;
    }

    /** El tamano de palabra, en bits. */
    public int getWordSize() {
        return this.wordSize;
    }

    /** Una copia del IV, o nulo si no tiene. */
    public byte[] getIV() {
        return this.iv == null ? null : IvParameterSpec.copy(this.iv, 0, this.iv.length);
    }

    /** Igualdad por los tres numeros y el IV. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RC5ParameterSpec)) {
            return false;
        }
        RC5ParameterSpec other = (RC5ParameterSpec) obj;
        return this.version == other.version
                && this.rounds == other.rounds
                && this.wordSize == other.wordSize
                && Arrays.equals(this.iv, other.iv);
    }

    public int hashCode() {
        int h = 0;
        if (this.iv != null) {
            for (int i = 0; i < this.iv.length; i++) {
                h = h + this.iv[i] * i;
            }
        }
        return h + this.version + this.rounds + this.wordSize;
    }
}
