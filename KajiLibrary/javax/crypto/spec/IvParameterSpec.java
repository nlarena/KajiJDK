package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * Un vector de inicializacion.
 *
 * <p>El IV se **copia al entrar y al salir**, y es de las pocas veces que la copia defensiva no es
 * discutible: un IV que alguien pudiera cambiar despues de configurar el cifrador dejaria a dos
 * partes de un mismo programa creyendo que usan el mismo, que es una forma silenciosa de romper el
 * cifrado.
 */
public class IvParameterSpec implements AlgorithmParameterSpec {

    private final byte[] iv;

    /**
     * @throws NullPointerException si `iv` es nulo
     */
    public IvParameterSpec(byte[] iv) {
        if (iv == null) {
            throw new NullPointerException("el IV no puede ser nulo");
        }
        this.iv = copy(iv, 0, iv.length);
    }

    /**
     * El IV son `len` bytes a partir de `offset`.
     *
     * @throws IllegalArgumentException si el arreglo es mas corto que `offset + len`
     * @throws ArrayIndexOutOfBoundsException si `offset` o `len` son negativos
     */
    public IvParameterSpec(byte[] iv, int offset, int len) {
        if (iv == null) {
            throw new IllegalArgumentException("el IV no puede ser nulo");
        }
        if (offset < 0 || len < 0) {
            throw new ArrayIndexOutOfBoundsException("offset o largo negativos");
        }
        if (iv.length - offset < len) {
            throw new IllegalArgumentException("el IV es mas corto que offset + len");
        }
        this.iv = copy(iv, offset, len);
    }

    static byte[] copy(byte[] src, int offset, int len) {
        byte[] out = new byte[len];
        System.arraycopy(src, offset, out, 0, len);
        return out;
    }

    /** Una copia del IV. */
    public byte[] getIV() {
        return copy(this.iv, 0, this.iv.length);
    }
}
