package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * El IV y el largo de la etiqueta de autenticacion de GCM.
 *
 * <p>`tLen` esta **en bits**, no en bytes, y es el error clasico con esta clase: pasar 16 en vez de
 * 128 configura una etiqueta de dos bytes, que se puede adivinar. Por eso el constructor rechaza
 * los negativos, aunque no puede rechazar un 16 -- es un valor legal para otros usos y la clase no
 * sabe cual es el suyo.
 */
public class GCMParameterSpec implements AlgorithmParameterSpec {

    private final int tLen;
    private final byte[] iv;

    /**
     * @throws IllegalArgumentException si `tLen` es negativo o el IV es nulo
     */
    public GCMParameterSpec(int tLen, byte[] iv) {
        if (iv == null) {
            throw new IllegalArgumentException("el IV no puede ser nulo");
        }
        if (tLen < 0) {
            throw new IllegalArgumentException("el largo de la etiqueta no puede ser negativo");
        }
        this.tLen = tLen;
        this.iv = IvParameterSpec.copy(iv, 0, iv.length);
    }

    /**
     * El IV son `len` bytes a partir de `offset`.
     *
     * @throws IllegalArgumentException si `tLen` es negativo, si el IV es nulo, o si el arreglo es
     *     mas corto que `offset + len`
     */
    public GCMParameterSpec(int tLen, byte[] iv, int offset, int len) {
        if (iv == null) {
            throw new IllegalArgumentException("el IV no puede ser nulo");
        }
        if (tLen < 0) {
            throw new IllegalArgumentException("el largo de la etiqueta no puede ser negativo");
        }
        if (offset < 0 || len < 0) {
            throw new IllegalArgumentException("offset o largo negativos");
        }
        if (iv.length - offset < len) {
            throw new IllegalArgumentException("el IV es mas corto que offset + len");
        }
        this.tLen = tLen;
        this.iv = IvParameterSpec.copy(iv, offset, len);
    }

    /** El largo de la etiqueta, **en bits**. */
    public int getTLen() {
        return this.tLen;
    }

    /** Una copia del IV. */
    public byte[] getIV() {
        return IvParameterSpec.copy(this.iv, 0, this.iv.length);
    }
}
