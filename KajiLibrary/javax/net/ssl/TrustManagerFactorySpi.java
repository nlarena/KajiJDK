package javax.net.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;

/**
 * Lo que un proveedor implementa para que exista una {@link TrustManagerFactory}.
 *
 * <p>El espejo de {@link KeyManagerFactorySpi}, del lado de la confianza. Notar que
 * {@link #engineInit(KeyStore)} no lleva contrasena: un almacen de confianza guarda certificados
 * publicos, no claves privadas, y no hay nada que desbloquear.
 */
public abstract class TrustManagerFactorySpi {

    public TrustManagerFactorySpi() {
    }

    /** Inicializa desde un almacen de certificados de confianza. */
    protected abstract void engineInit(KeyStore ks) throws KeyStoreException;

    /** Inicializa desde parametros; ver {@link CertPathTrustManagerParameters}. */
    protected abstract void engineInit(ManagerFactoryParameters spec)
            throws InvalidAlgorithmParameterException;

    /**
     * Los manejadores de confianza.
     *
     * @throws IllegalStateException si no se inicializo antes
     */
    protected abstract TrustManager[] engineGetTrustManagers();
}
