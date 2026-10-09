package javax.crypto;

import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * What a provider has to write in order to offer a symmetric key factory.
 *
 * <h2>The two shapes of a key</h2>
 *
 * <p>The opaque one --{@link SecretKey}-- may live inside a device and refuse to be looked at. The
 * transparent one --{@link KeySpec}-- is material the program assembles or reads from a file. This
 * factory is the only road between the two, and that is why it is what a key is derived from a
 * password with: {@link javax.crypto.spec.PBEKeySpec} goes in, {@link SecretKey} comes out.
 *
 * <h2>{@link #engineTranslateKey}</h2>
 *
 * <p>It converts a key from another provider into one of this one's. It is for using a key that
 * arrived from outside with a provider that only knows how to work with its own, without exporting
 * the material.
 *
 * @since 1.4
 */
public abstract class SecretKeyFactorySpi {

    /** One. */
    public SecretKeyFactorySpi() {
    }

    /**
     * Assembles a key out of its description.
     *
     * @param keySpec the description
     * @return the key
     * @throws InvalidKeySpecException if the description is no good for this algorithm
     */
    protected abstract SecretKey engineGenerateSecret(KeySpec keySpec)
            throws InvalidKeySpecException;

    /**
     * Describes a key.
     *
     * @param key the key
     * @param keySpec which description is wanted
     * @return the description
     * @throws InvalidKeySpecException if the key cannot be described that way
     */
    protected abstract KeySpec engineGetKeySpec(SecretKey key, Class<?> keySpec)
            throws InvalidKeySpecException;

    /**
     * Converts a key from another provider into one of this one's.
     *
     * @param key the key
     * @return this provider's equivalent key
     * @throws InvalidKeyException if it cannot be converted
     */
    protected abstract SecretKey engineTranslateKey(SecretKey key) throws InvalidKeyException;
}
