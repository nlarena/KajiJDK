package javax.net.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;

/**
 * What a provider implements for a {@link TrustManagerFactory} to exist.
 *
 * <p>The mirror of {@link KeyManagerFactorySpi}, on the trust side. Note that
 * {@link #engineInit(KeyStore)} takes no password: a trust store keeps public certificates, not
 * private keys, and there is nothing to unlock.
 */
public abstract class TrustManagerFactorySpi {

    public TrustManagerFactorySpi() {
    }

    /** Initializes from a store of trusted certificates. */
    protected abstract void engineInit(KeyStore ks) throws KeyStoreException;

    /** Initializes from parameters; see {@link CertPathTrustManagerParameters}. */
    protected abstract void engineInit(ManagerFactoryParameters spec)
            throws InvalidAlgorithmParameterException;

    /**
     * The trust managers.
     *
     * @throws IllegalStateException if it was not initialized first
     */
    protected abstract TrustManager[] engineGetTrustManagers();
}
