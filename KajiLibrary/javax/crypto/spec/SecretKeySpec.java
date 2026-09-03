package javax.crypto.spec;

import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.SecretKey;

/**
 * Una clave simetrica que es, literalmente, un arreglo de bytes y un nombre de algoritmo.
 *
 * <p>Es a la vez un {@link KeySpec} y un {@link SecretKey}, y esa doble naturaleza es su razon de
 * ser: se puede pasar a un `SecretKeyFactory` para que la traduzca, o usar directamente en un
 * cifrador. Sirve para los algoritmos cuyo material de clave **no tiene estructura** --AES,
 * HmacSHA256--; para los que si la tienen, como DES con su bit de paridad, hay una clase propia.
 *
 * <p><strong>No valida nada.</strong> Ni el largo ni el contenido: un `SecretKeySpec` de tres bytes
 * para AES se construye sin protestar y falla recien cuando un cifrador lo use. Es a proposito y es
 * lo que el JDK hace -- esta clase no sabe que algoritmos existen ni que largos aceptan, y fingir
 * que si llevaria a rechazar claves validas de un algoritmo que no conoce.
 */
public class SecretKeySpec implements KeySpec, SecretKey {

    private static final long serialVersionUID = 6577238317307289933L;

    private final byte[] key;
    private final String algorithm;

    /**
     * @throws IllegalArgumentException si la clave es nula, esta vacia, o el algoritmo es nulo
     */
    public SecretKeySpec(byte[] key, String algorithm) {
        if (key == null) {
            throw new IllegalArgumentException("la clave no puede ser nula");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("la clave no puede estar vacia");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("el algoritmo no puede ser nulo");
        }
        this.key = IvParameterSpec.copy(key, 0, key.length);
        this.algorithm = algorithm;
    }

    /**
     * La clave son `len` bytes a partir de `offset`.
     *
     * @throws IllegalArgumentException si la clave es nula o vacia, si el algoritmo es nulo, o si
     *     el arreglo es mas corto que `offset + len`
     * @throws ArrayIndexOutOfBoundsException si `offset` o `len` son negativos
     */
    public SecretKeySpec(byte[] key, int offset, int len, String algorithm) {
        if (key == null) {
            throw new IllegalArgumentException("la clave no puede ser nula");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("la clave no puede estar vacia");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("el algoritmo no puede ser nulo");
        }
        if (offset < 0 || len < 0) {
            throw new ArrayIndexOutOfBoundsException("offset o largo negativos");
        }
        if (key.length - offset < len) {
            throw new IllegalArgumentException("la clave es mas corta que offset + len");
        }
        if (len == 0) {
            throw new IllegalArgumentException("la clave no puede estar vacia");
        }
        this.key = IvParameterSpec.copy(key, offset, len);
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    /** Siempre `"RAW"`: los bytes son la clave, sin codificacion de por medio. */
    public String getFormat() {
        return "RAW";
    }

    /** Una copia del material de la clave. */
    public byte[] getEncoded() {
        return IvParameterSpec.copy(this.key, 0, this.key.length);
    }

    /**
     * El nombre del algoritmo **en minusculas** mas los bytes.
     *
     * <p>Las minusculas no son un detalle: `equals` compara el algoritmo sin distinguir mayusculas
     * --`"AES"` y `"aes"` nombran el mismo algoritmo-- y un `hashCode` que si las distinguiera
     * pondria dos claves iguales en cubetas distintas.
     */
    public int hashCode() {
        int h = 0;
        for (int i = 1; i < this.key.length; i++) {
            h = h + (this.key[i] * i);
        }
        return h ^ this.algorithm.toLowerCase().hashCode();
    }

    /**
     * Igualdad por algoritmo --sin distinguir mayusculas-- y por los bytes.
     *
     * <p>La comparacion de los bytes es de **tiempo constante**: recorre siempre los dos arreglos
     * enteros en vez de cortar en la primera diferencia. Comparar claves con un cortocircuito filtra
     * cuantos bytes iniciales coinciden a quien pueda medir el tiempo, que es como se rompe una
     * clave a fuerza de comparaciones.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SecretKey)) {
            return false;
        }
        SecretKey other = (SecretKey) obj;
        if (!this.algorithm.equalsIgnoreCase(other.getAlgorithm())) {
            return false;
        }
        byte[] theirs = other.getEncoded();
        if (theirs == null) {
            return false;
        }
        boolean same = this.key.length == theirs.length;
        int n = this.key.length < theirs.length ? this.key.length : theirs.length;
        int diff = 0;
        for (int i = 0; i < n; i++) {
            diff = diff | (this.key[i] ^ theirs[i]);
        }
        Arrays.fill(theirs, (byte) 0);
        return same && diff == 0;
    }
}
