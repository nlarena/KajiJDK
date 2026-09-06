package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Lo que un proveedor tiene que escribir para ofrecer un generador de claves simetricas.
 *
 * <h2>Por que no alcanza con bytes al azar</h2>
 *
 * <p>Para muchos algoritmos alcanzaria, y ahi el generador es poco mas que un envoltorio del
 * generador aleatorio. Para otros no: DES y DESede tienen bits de paridad y claves debiles que hay
 * que descartar, y una clave para un algoritmo con estructura --las de curvas, por ejemplo-- tiene
 * que caer en un rango. Sortear bytes y llamarlos clave produciria claves invalidas cada tanto.
 *
 * <h2>Las tres formas de configurarlo</h2>
 *
 * <p>Por tamano, por parametros, o por nada. La ultima no es un descuido: casi todos los algoritmos
 * tienen un tamano recomendado, y elegirlo por omision es mejor que obligar a cada programa a saber
 * cual es --que es como se terminan escribiendo claves de 512 bits en 2026--.
 *
 * @since 1.4
 */
public abstract class KeyGeneratorSpi {

    /** Uno. */
    public KeyGeneratorSpi() {
    }

    /**
     * Lo configura con el tamano de omision.
     *
     * @param random de donde sacar el azar
     */
    protected abstract void engineInit(SecureRandom random);

    /**
     * Lo configura con parametros.
     *
     * @param params los parametros
     * @param random de donde sacar el azar
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    protected abstract void engineInit(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException;

    /**
     * Lo configura con un tamano.
     *
     * @param keysize el tamano en bits
     * @param random de donde sacar el azar
     * @throws InvalidParameterException si ese tamano no sirve
     */
    protected abstract void engineInit(int keysize, SecureRandom random);

    /**
     * Genera una clave.
     *
     * @return la clave
     */
    protected abstract SecretKey engineGenerateKey();
}
