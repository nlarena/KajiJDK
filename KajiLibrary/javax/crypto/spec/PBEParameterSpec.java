package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * La sal y la cantidad de iteraciones de un cifrado basado en contrasena.
 *
 * <p>Los dos valores son lo que hace lento el ataque por diccionario: la sal impide precalcular
 * tablas y las iteraciones encarecen cada intento. Esta clase no los valida --ni el largo de la sal
 * ni un minimo de iteraciones-- porque el valor razonable depende del algoritmo y del ano, y
 * ponerle un piso ahora seria un numero que envejece mal.
 */
public class PBEParameterSpec implements AlgorithmParameterSpec {

    private final byte[] salt;
    private final int iterationCount;
    private final AlgorithmParameterSpec paramSpec;

    /**
     * @throws NullPointerException si la sal es nula
     */
    public PBEParameterSpec(byte[] salt, int iterationCount) {
        this(salt, iterationCount, null);
    }

    /**
     * Con parametros para el cifrador de abajo --el IV de un AES, por ejemplo--.
     *
     * @throws NullPointerException si la sal es nula
     */
    public PBEParameterSpec(byte[] salt, int iterationCount, AlgorithmParameterSpec paramSpec) {
        if (salt == null) {
            throw new NullPointerException("la sal no puede ser nula");
        }
        this.salt = IvParameterSpec.copy(salt, 0, salt.length);
        this.iterationCount = iterationCount;
        this.paramSpec = paramSpec;
    }

    /** Una copia de la sal. */
    public byte[] getSalt() {
        return IvParameterSpec.copy(this.salt, 0, this.salt.length);
    }

    /** Cuantas iteraciones. */
    public int getIterationCount() {
        return this.iterationCount;
    }

    /** Los parametros del cifrador de abajo, o nulo si no hay. */
    public AlgorithmParameterSpec getParameterSpec() {
        return this.paramSpec;
    }
}
