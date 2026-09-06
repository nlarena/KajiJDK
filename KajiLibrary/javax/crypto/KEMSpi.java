package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Lo que un proveedor tiene que escribir para ofrecer un mecanismo de encapsulacion de claves.
 *
 * <h2>Por que dos objetos y no dos metodos</h2>
 *
 * <p>Porque cada lado usa una clave distinta y hace la comprobacion una sola vez. Armar el
 * encapsulador valida la clave publica; despues se puede encapsular muchas veces sin volver a
 * validar nada. Lo mismo del otro lado con la privada.
 *
 * <p>Ademas los dos objetos saben los tamanos --{@code secretSize} y
 * {@code encapsulationSize}-- antes de hacer nada, que es lo que permite reservar memoria y armar
 * protocolos de tamano fijo.
 *
 * @since 21
 */
public interface KEMSpi {

    /**
     * Arma un encapsulador para esa clave publica.
     *
     * @param publicKey la clave publica del otro
     * @param spec los parametros, o {@code null}
     * @param secureRandom de donde sacar el azar, o {@code null}
     * @return el encapsulador
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws InvalidKeyException si la clave no sirve
     */
    EncapsulatorSpi engineNewEncapsulator(PublicKey publicKey, AlgorithmParameterSpec spec,
            SecureRandom secureRandom)
            throws InvalidAlgorithmParameterException, InvalidKeyException;

    /**
     * Arma un desencapsulador para esa clave privada.
     *
     * @param privateKey la clave privada propia
     * @param spec los parametros, o {@code null}
     * @return el desencapsulador
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws InvalidKeyException si la clave no sirve
     */
    DecapsulatorSpi engineNewDecapsulator(PrivateKey privateKey, AlgorithmParameterSpec spec)
            throws InvalidAlgorithmParameterException, InvalidKeyException;

    /** El lado que genera el secreto. */
    interface EncapsulatorSpi {

        /**
         * Genera un secreto y su encapsulacion.
         *
         * @param from desde que byte del secreto
         * @param to hasta que byte del secreto
         * @param algorithm para que algoritmo es la clave que sale
         * @return el secreto y la encapsulacion
         */
        KEM.Encapsulated engineEncapsulate(int from, int to, String algorithm);

        /**
         * Cuanto mide el secreto.
         *
         * @return el tamano en bytes
         */
        int engineSecretSize();

        /**
         * Cuanto mide la encapsulacion.
         *
         * @return el tamano en bytes
         */
        int engineEncapsulationSize();
    }

    /** El lado que recupera el secreto. */
    interface DecapsulatorSpi {

        /**
         * Recupera el secreto de una encapsulacion.
         *
         * @param encapsulation la encapsulacion
         * @param from desde que byte del secreto
         * @param to hasta que byte del secreto
         * @param algorithm para que algoritmo es la clave que sale
         * @return la clave
         * @throws DecapsulateException si no se pudo recuperar
         */
        SecretKey engineDecapsulate(byte[] encapsulation, int from, int to, String algorithm)
                throws DecapsulateException;

        /**
         * Cuanto mide el secreto.
         *
         * @return el tamano en bytes
         */
        int engineSecretSize();

        /**
         * Cuanto mide la encapsulacion.
         *
         * @return el tamano en bytes
         */
        int engineEncapsulationSize();
    }
}
