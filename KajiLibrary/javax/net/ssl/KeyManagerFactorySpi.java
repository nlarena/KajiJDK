package javax.net.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

/**
 * What a provider implements for a {@link KeyManagerFactory} to exist.
 *
 * <p>It is the JDK's SPI pattern: the public class does the provider lookup, validates the
 * arguments and fixes the order of the calls; this one does the work. Separating them allows
 * changing the provider without touching the code that uses it, which is why the whole of {@code
 * java.security} is split in two like this.
 */
public abstract class KeyManagerFactorySpi {

    public KeyManagerFactorySpi() {
    }

    /** Initializes from a key store and its password. */
    protected abstract void engineInit(KeyStore ks, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException;

    /** Initializes from parameters that are not a store; see {@link ManagerFactoryParameters}. */
    protected abstract void engineInit(ManagerFactoryParameters spec)
            throws InvalidAlgorithmParameterException;

    /**
     * The managers, one per key type.
     *
     * @throws IllegalStateException if it was not initialized first
     */
    protected abstract KeyManager[] engineGetKeyManagers();
}
