package javax.crypto.spec;

import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Una contrasena --y opcionalmente su sal, iteraciones y largo de clave-- para derivar una clave.
 *
 * <p><strong>La contrasena es un `char[]` y no un `String`, y esa es la idea entera de esta
 * clase.</strong> Un `String` es inmutable y vive en el pool hasta que el recolector lo levante:
 * una contrasena ahi queda en memoria un tiempo que nadie controla, y aparece en un volcado. Un
 * arreglo se puede **borrar**, y {@link #clearPassword} es lo que lo hace.
 *
 * <p>Por eso el constructor copia el arreglo y `getPassword` devuelve otra copia: el llamador puede
 * borrar el suyo enseguida sin romper este objeto. Y por eso `getPassword` **tira** despues de
 * `clearPassword` en vez de devolver ceros -- devolver una contrasena en blanco como si fuera valida
 * es la clase de error que no se nota hasta que algo se cifra con la clave equivocada.
 */
public class PBEKeySpec implements KeySpec {

    private char[] password;
    private final byte[] salt;
    private final int iterationCount;
    private final int keyLength;

    /**
     * Solo la contrasena. Un nulo se toma como contrasena vacia, que es lo que hace el JDK.
     */
    public PBEKeySpec(char[] password) {
        this.password = password == null ? new char[0] : copy(password);
        this.salt = null;
        this.iterationCount = 0;
        this.keyLength = 0;
    }

    /**
     * Con sal e iteraciones.
     *
     * @throws NullPointerException si la sal es nula
     * @throws IllegalArgumentException si la sal esta vacia o las iteraciones no son positivas
     */
    public PBEKeySpec(char[] password, byte[] salt, int iterationCount) {
        this(password, salt, iterationCount, 0, false);
    }

    /**
     * Con sal, iteraciones y largo de clave en bits.
     *
     * @throws NullPointerException si la sal es nula
     * @throws IllegalArgumentException si la sal esta vacia, o si las iteraciones o el largo no son
     *     positivos
     */
    public PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength) {
        this(password, salt, iterationCount, keyLength, true);
    }

    private PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength,
            boolean conLargo) {
        if (salt == null) {
            throw new NullPointerException("la sal no puede ser nula");
        }
        if (salt.length == 0) {
            throw new IllegalArgumentException("la sal no puede estar vacia");
        }
        if (iterationCount <= 0) {
            throw new IllegalArgumentException("las iteraciones tienen que ser positivas");
        }
        if (conLargo && keyLength <= 0) {
            throw new IllegalArgumentException("el largo de clave tiene que ser positivo");
        }
        this.password = password == null ? new char[0] : copy(password);
        this.salt = IvParameterSpec.copy(salt, 0, salt.length);
        this.iterationCount = iterationCount;
        this.keyLength = keyLength;
    }

    private static char[] copy(char[] src) {
        char[] out = new char[src.length];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    /**
     * Borra la contrasena de la memoria.
     *
     * <p>Se sobrescribe con ceros **antes** de soltar la referencia: soltarla sola dejaria los
     * caracteres en el monton hasta que el recolector pase, que es justo lo que esta clase existe
     * para evitar.
     */
    public final synchronized void clearPassword() {
        if (this.password != null) {
            Arrays.fill(this.password, (char) 0);
            this.password = null;
        }
    }

    /**
     * Una copia de la contrasena.
     *
     * @throws IllegalStateException si ya se llamo a {@link #clearPassword}
     */
    public final synchronized char[] getPassword() {
        if (this.password == null) {
            throw new IllegalStateException("la contrasena ya se borro");
        }
        return copy(this.password);
    }

    /** Una copia de la sal, o nulo si no tiene. */
    public final byte[] getSalt() {
        return this.salt == null ? null : IvParameterSpec.copy(this.salt, 0, this.salt.length);
    }

    /** Cuantas iteraciones, o cero si no se dieron. */
    public final int getIterationCount() {
        return this.iterationCount;
    }

    /** El largo de clave en bits, o cero si no se dio. */
    public final int getKeyLength() {
        return this.keyLength;
    }
}
