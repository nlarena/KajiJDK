package javax.crypto.spec;

import java.security.InvalidKeyException;
import java.security.spec.KeySpec;

/**
 * Una clave Triple DES: veinticuatro bytes, o sea tres claves DES puestas una detras de la otra.
 *
 * <p>No tiene `isWeak`, y la ausencia es deliberada: la debilidad de Triple DES no esta en las
 * claves individuales sino en que dos de las tres sean iguales --con eso degenera a DES simple-- y
 * eso no es lo que `isWeak` de {@link DESKeySpec} mide. Comprobar solo las tres por separado daria
 * una falsa tranquilidad.
 */
public class DESedeKeySpec implements KeySpec {

    /** Los bytes que una clave Triple DES ocupa. */
    public static final int DES_EDE_KEY_LEN = 24;

    private final byte[] key;

    /**
     * @throws InvalidKeyException si el arreglo tiene menos de veinticuatro bytes
     * @throws NullPointerException si es nulo
     */
    public DESedeKeySpec(byte[] key) throws InvalidKeyException {
        this(key, 0);
    }

    /**
     * La clave son los veinticuatro bytes a partir de `offset`.
     *
     * @throws InvalidKeyException si quedan menos de veinticuatro bytes desde `offset`
     * @throws NullPointerException si el arreglo es nulo
     */
    public DESedeKeySpec(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("la clave no puede ser nula");
        }
        if (key.length - offset < DES_EDE_KEY_LEN) {
            throw new InvalidKeyException(
                    "una clave Triple DES son " + DES_EDE_KEY_LEN + " bytes desde el offset");
        }
        this.key = IvParameterSpec.copy(key, offset, DES_EDE_KEY_LEN);
    }

    /** Una copia de los veinticuatro bytes. */
    public byte[] getKey() {
        return IvParameterSpec.copy(this.key, 0, DES_EDE_KEY_LEN);
    }

    /**
     * Si las tres claves DES de adentro tienen sus bits de paridad puestos.
     *
     * @throws InvalidKeyException si quedan menos de veinticuatro bytes desde `offset`
     * @throws NullPointerException si el arreglo es nulo
     */
    public static boolean isParityAdjusted(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("la clave no puede ser nula");
        }
        if (key.length - offset < DES_EDE_KEY_LEN) {
            throw new InvalidKeyException(
                    "una clave Triple DES son " + DES_EDE_KEY_LEN + " bytes desde el offset");
        }
        return DESKeySpec.isParityAdjusted(key, offset)
                && DESKeySpec.isParityAdjusted(key, offset + 8)
                && DESKeySpec.isParityAdjusted(key, offset + 16);
    }
}
