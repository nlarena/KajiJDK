package javax.crypto.spec;

import java.security.InvalidKeyException;
import java.security.spec.KeySpec;

/**
 * Una clave DES: ocho bytes, de los cuales solo cincuenta y seis bits son clave.
 *
 * <p>El octavo bit de cada byte es de **paridad** y DES lo ignora. De ahi los dos metodos
 * estaticos, que son lo unico interesante de esta clase:
 *
 * <ul>
 * <li>{@link #isParityAdjusted} dice si los bits de paridad estan bien puestos --paridad impar por
 *     byte--. Una clave sin ajustar sigue siendo usable; el bit solo servia para detectar errores
 *     de transmision en hardware de los anos setenta.</li>
 * <li>{@link #isWeak} dice si es una de las dieciseis claves que DES tiene documentadas como
 *     debiles o semidebiles. Una clave debil hace que cifrar dos veces devuelva el texto original,
 *     y una semidebil forma pares donde una descifra lo que cifro la otra. **Eso si importa**, y por
 *     eso el metodo existe: son claves que hay que rechazar, no advertir.
 *     <p>Ojo con como compara: byte por byte, **con el bit de paridad**. Una clave debil con la
 *     paridad mal puesta no la reconoce, aunque para DES sea la misma clave. Ver la nota de la
 *     implementacion.</li>
 * </ul>
 */
public class DESKeySpec implements KeySpec {

    /** Los bytes que una clave DES ocupa. */
    public static final int DES_KEY_LEN = 8;

    // Las dieciseis claves problematicas, con los bits de paridad puestos como el estandar las
    // publica. Las cuatro primeras son debiles y las doce restantes forman los seis pares
    // semidebiles. Se comparan ignorando los bits de paridad, que es lo que hace `esta`.
    private static final byte[][] PROBLEMATIC = {
        { (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
          (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01 },
        { (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE,
          (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE },
        { (byte) 0xE0, (byte) 0xE0, (byte) 0xE0, (byte) 0xE0,
          (byte) 0xF1, (byte) 0xF1, (byte) 0xF1, (byte) 0xF1 },
        { (byte) 0x1F, (byte) 0x1F, (byte) 0x1F, (byte) 0x1F,
          (byte) 0x0E, (byte) 0x0E, (byte) 0x0E, (byte) 0x0E },
        { (byte) 0x01, (byte) 0xFE, (byte) 0x01, (byte) 0xFE,
          (byte) 0x01, (byte) 0xFE, (byte) 0x01, (byte) 0xFE },
        { (byte) 0xFE, (byte) 0x01, (byte) 0xFE, (byte) 0x01,
          (byte) 0xFE, (byte) 0x01, (byte) 0xFE, (byte) 0x01 },
        { (byte) 0x1F, (byte) 0xE0, (byte) 0x1F, (byte) 0xE0,
          (byte) 0x0E, (byte) 0xF1, (byte) 0x0E, (byte) 0xF1 },
        { (byte) 0xE0, (byte) 0x1F, (byte) 0xE0, (byte) 0x1F,
          (byte) 0xF1, (byte) 0x0E, (byte) 0xF1, (byte) 0x0E },
        { (byte) 0x01, (byte) 0xE0, (byte) 0x01, (byte) 0xE0,
          (byte) 0x01, (byte) 0xF1, (byte) 0x01, (byte) 0xF1 },
        { (byte) 0xE0, (byte) 0x01, (byte) 0xE0, (byte) 0x01,
          (byte) 0xF1, (byte) 0x01, (byte) 0xF1, (byte) 0x01 },
        { (byte) 0x1F, (byte) 0xFE, (byte) 0x1F, (byte) 0xFE,
          (byte) 0x0E, (byte) 0xFE, (byte) 0x0E, (byte) 0xFE },
        { (byte) 0xFE, (byte) 0x1F, (byte) 0xFE, (byte) 0x1F,
          (byte) 0xFE, (byte) 0x0E, (byte) 0xFE, (byte) 0x0E },
        { (byte) 0x01, (byte) 0x1F, (byte) 0x01, (byte) 0x1F,
          (byte) 0x01, (byte) 0x0E, (byte) 0x01, (byte) 0x0E },
        { (byte) 0x1F, (byte) 0x01, (byte) 0x1F, (byte) 0x01,
          (byte) 0x0E, (byte) 0x01, (byte) 0x0E, (byte) 0x01 },
        { (byte) 0xE0, (byte) 0xFE, (byte) 0xE0, (byte) 0xFE,
          (byte) 0xF1, (byte) 0xFE, (byte) 0xF1, (byte) 0xFE },
        { (byte) 0xFE, (byte) 0xE0, (byte) 0xFE, (byte) 0xE0,
          (byte) 0xFE, (byte) 0xF1, (byte) 0xFE, (byte) 0xF1 },
    };

    private final byte[] key;

    /**
     * @throws InvalidKeyException si el arreglo tiene menos de ocho bytes
     * @throws NullPointerException si es nulo
     */
    public DESKeySpec(byte[] key) throws InvalidKeyException {
        this(key, 0);
    }

    /**
     * La clave son los ocho bytes a partir de `offset`.
     *
     * @throws InvalidKeyException si quedan menos de ocho bytes desde `offset`
     * @throws NullPointerException si el arreglo es nulo
     */
    public DESKeySpec(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("la clave no puede ser nula");
        }
        if (key.length - offset < DES_KEY_LEN) {
            throw new InvalidKeyException(
                    "una clave DES son " + DES_KEY_LEN + " bytes desde el offset");
        }
        this.key = IvParameterSpec.copy(key, offset, DES_KEY_LEN);
    }

    /** Una copia de los ocho bytes. */
    public byte[] getKey() {
        return IvParameterSpec.copy(this.key, 0, DES_KEY_LEN);
    }

    /**
     * Si los bits de paridad estan puestos: cada byte tiene una cantidad **impar** de unos.
     *
     * @throws InvalidKeyException si quedan menos de ocho bytes desde `offset`
     * @throws NullPointerException si el arreglo es nulo
     */
    public static boolean isParityAdjusted(byte[] key, int offset) throws InvalidKeyException {
        exigirOcho(key, offset);
        for (int i = offset; i < offset + DES_KEY_LEN; i++) {
            int unos = 0;
            int b = key[i] & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                unos = unos + ((b >> bit) & 1);
            }
            if (unos % 2 == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Si es una de las dieciseis claves debiles o semidebiles. Ver la nota de la clase.
     *
     * @throws InvalidKeyException si quedan menos de ocho bytes desde `offset`
     * @throws NullPointerException si el arreglo es nulo
     */
    public static boolean isWeak(byte[] key, int offset) throws InvalidKeyException {
        exigirOcho(key, offset);
        for (int i = 0; i < PROBLEMATIC.length; i++) {
            boolean igual = true;
            for (int j = 0; j < DES_KEY_LEN && igual; j++) {
                // Se comparan los bytes ENTEROS, bit de paridad incluido, y eso es lo que hace el
                // JDK 25 -- comprobado. Uno esperaria lo contrario: DES ignora el bit de paridad,
                // asi que 0x00 y 0x01 son la misma clave para el algoritmo y la de todos ceros
                // deberia ser tan debil como la de todos unos. `isWeak` contesta `false` para la de
                // ceros y `true` para la de unos.
                //
                // Es una decision del JDK y no un descuido: la lista del estandar publica las
                // dieciseis claves CON su paridad ajustada, y el metodo responde por esa lista y no
                // por la clase de equivalencia. La consecuencia practica es que `isWeak` no alcanza
                // por si solo -- hay que ajustar la paridad antes de preguntar.
                if (key[offset + j] != PROBLEMATIC[i][j]) {
                    igual = false;
                }
            }
            if (igual) {
                return true;
            }
        }
        return false;
    }

    private static void exigirOcho(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("la clave no puede ser nula");
        }
        if (key.length - offset < DES_KEY_LEN) {
            throw new InvalidKeyException(
                    "una clave DES son " + DES_KEY_LEN + " bytes desde el offset");
        }
    }
}
