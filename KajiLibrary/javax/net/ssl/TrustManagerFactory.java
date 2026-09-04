package javax.net.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

/**
 * Produce los {@link TrustManager} que deciden en quien confiar.
 *
 * <p>Sirve porque escribir un {@link X509TrustManager} a mano es a la vez innecesario y peligroso:
 * validar una cadena de certificados tiene mas casos de los que uno recuerda —vencimiento, cadena
 * incompleta, revocacion, restricciones de uso— y cada uno omitido es un agujero. Esta fabrica
 * entrega la implementacion del proveedor, que ya los cubre.
 *
 * <p>Inicializarla con {@code null} usa el almacen de confianza por omision del sistema, que es lo
 * que se quiere casi siempre.
 */
public class TrustManagerFactory {

    private final TrustManagerFactorySpi factorySpi;
    private final Provider provider;
    private final String algorithm;

    /** El algoritmo por omision: la propiedad {@code ssl.TrustManagerFactory.algorithm}. */
    public static final String getDefaultAlgorithm() {
        String a = Security.getProperty("ssl.TrustManagerFactory.algorithm");
        return a == null ? "PKIX" : a;
    }

    /** Para los proveedores. */
    protected TrustManagerFactory(TrustManagerFactorySpi factorySpi, Provider provider,
            String algorithm) {
        this.factorySpi = factorySpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /** El algoritmo de esta fabrica. */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Del primer proveedor que ofrezca ese algoritmo.
     *
     * @throws NoSuchAlgorithmException si ninguno lo ofrece
     */
    public static final TrustManagerFactory getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm");
        }
        Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            Provider.Service s = provs[i].getService("TrustManagerFactory", algorithm);
            if (s != null) {
                return armar(s, provs[i], algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " TrustManagerFactory not available");
    }

    /**
     * De un proveedor nombrado.
     *
     * @throws NoSuchProviderException si no hay proveedor con ese nombre
     */
    public static final TrustManagerFactory getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    /**
     * De un proveedor concreto.
     *
     * @throws NoSuchAlgorithmException si ese proveedor no lo ofrece
     */
    public static final TrustManagerFactory getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm");
        }
        Provider.Service s = provider.getService("TrustManagerFactory", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(algorithm + " TrustManagerFactory not available");
        }
        return armar(s, provider, algorithm);
    }

    private static TrustManagerFactory armar(Provider.Service s, Provider p, String algorithm)
            throws NoSuchAlgorithmException {
        try {
            Object spi = s.newInstance(null);
            if (!(spi instanceof TrustManagerFactorySpi)) {
                throw new NoSuchAlgorithmException(
                        "el proveedor no devolvio un TrustManagerFactorySpi para " + algorithm);
            }
            return new TrustManagerFactory((TrustManagerFactorySpi) spi, p, algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (Exception e) {
            throw new NoSuchAlgorithmException(
                    algorithm + " TrustManagerFactory not available", e);
        }
    }

    /** El proveedor que la produjo. */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Desde un almacen de certificados de confianza; {@code null} usa el del sistema.
     *
     * <p>Sin contrasena, y no es una omision: un almacen de confianza guarda certificados publicos.
     * No hay nada secreto que desbloquear.
     */
    public final void init(KeyStore ks) throws KeyStoreException {
        this.factorySpi.engineInit(ks);
    }

    /** Desde parametros; ver {@link CertPathTrustManagerParameters}. */
    public final void init(ManagerFactoryParameters spec)
            throws InvalidAlgorithmParameterException {
        this.factorySpi.engineInit(spec);
    }

    /**
     * Los manejadores de confianza.
     *
     * @throws IllegalStateException si no se llamo antes a {@code init}
     */
    public final TrustManager[] getTrustManagers() {
        return this.factorySpi.engineGetTrustManagers();
    }
}
