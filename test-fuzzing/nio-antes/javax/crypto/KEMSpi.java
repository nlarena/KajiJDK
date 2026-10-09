package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * What a provider has to write in order to offer a key encapsulation mechanism.
 *
 * <h2>Why two objects and not two methods</h2>
 *
 * <p>Because each side uses a different key and does the checking once. Building the encapsulator
 * validates the public key; after that it can encapsulate many times without validating anything
 * again. The same on the other side with the private one.
 *
 * <p>The two objects also know the sizes --{@code secretSize} and {@code encapsulationSize}--
 * before doing anything, which is what allows memory to be reserved and fixed-size protocols to be
 * built.
 *
 * @since 21
 */
public interface KEMSpi {

    /**
     * Builds an encapsulator for that public key.
     *
     * @param publicKey the other one's public key
     * @param spec the parameters, or {@code null}
     * @param secureRandom where to take the randomness from, or {@code null}
     * @return the encapsulator
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws InvalidKeyException if the key is no good
     */
    EncapsulatorSpi engineNewEncapsulator(PublicKey publicKey, AlgorithmParameterSpec spec,
            SecureRandom secureRandom)
            throws InvalidAlgorithmParameterException, InvalidKeyException;

    /**
     * Builds a decapsulator for that private key.
     *
     * @param privateKey one's own private key
     * @param spec the parameters, or {@code null}
     * @return the decapsulator
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws InvalidKeyException if the key is no good
     */
    DecapsulatorSpi engineNewDecapsulator(PrivateKey privateKey, AlgorithmParameterSpec spec)
            throws InvalidAlgorithmParameterException, InvalidKeyException;

    /** The side that generates the secret. */
    interface EncapsulatorSpi {

        /**
         * Generates a secret and its encapsulation.
         *
         * @param from from which byte of the secret
         * @param to up to which byte of the secret
         * @param algorithm which algorithm the resulting key is for
         * @return the secret and the encapsulation
         */
        KEM.Encapsulated engineEncapsulate(int from, int to, String algorithm);

        /**
         * How large the secret is.
         *
         * @return the size in bytes
         */
        int engineSecretSize();

        /**
         * How large the encapsulation is.
         *
         * @return the size in bytes
         */
        int engineEncapsulationSize();
    }

    /** The side that recovers the secret. */
    interface DecapsulatorSpi {

        /**
         * Recovers the secret from an encapsulation.
         *
         * @param encapsulation the encapsulation
         * @param from from which byte of the secret
         * @param to up to which byte of the secret
         * @param algorithm which algorithm the resulting key is for
         * @return the key
         * @throws DecapsulateException if it could not be recovered
         */
        SecretKey engineDecapsulate(byte[] encapsulation, int from, int to, String algorithm)
                throws DecapsulateException;

        /**
         * How large the secret is.
         *
         * @return the size in bytes
         */
        int engineSecretSize();

        /**
         * How large the encapsulation is.
         *
         * @return the size in bytes
         */
        int engineEncapsulationSize();
    }
}
