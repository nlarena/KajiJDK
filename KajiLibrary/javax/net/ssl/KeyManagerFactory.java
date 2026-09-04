package javax.net.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;

/**
 * Produce los {@link KeyManager} que presentan las credenciales propias.
 *
 * <p>El espejo de {@link TrustManagerFactory}: aquella decide en quien confiar, esta decide que
 * presentar. La diferencia visible es que {@link #init(KeyStore, char[])} <strong>si</strong> lleva
 * contrasena — un almacen de claves guarda claves privadas, y desbloquearlas es todo el punto.
 */
public class KeyManagerFactory {

    private final KeyManagerFactorySpi factorySpi;
    private final Provider provider;
    private final String algorithm;

    /** El algoritmo por omision: la propiedad {@code ssl.KeyManagerFactory.algorithm}. */
    public static final String getDefaultAlgorithm() {
        String a = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        return a == null ? "SunX509" : a;
    }

    /** Para los proveedores. */
    protected KeyManagerFactory(KeyManagerFactorySpi factorySpi, Provider provider,
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
    public static final KeyManagerFactory getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm");
        }
        Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            Provider.Service s = provs[i].getService("KeyManagerFactory", algorithm);
            if (s != null) {
                return armar(s, provs[i], algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " KeyManagerFactory not available");
    }

    /**
     * De un proveedor nombrado.
     *
     * @throws NoSuchProviderException si no hay proveedor con ese nombre
     */
    public static final KeyManagerFactory getInstance(String algorithm, String provider)
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
    public static final KeyManagerFactory getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm");
        }
        Provider.Service s = provider.getService("KeyManagerFactory", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(algorithm + " KeyManagerFactory not available");
        }
        return armar(s, provider, algorithm);
    }

    private static KeyManagerFactory armar(Provider.Service s, Provider p, String algorithm)
            throws NoSuchAlgorithmException {
        try {
            Object spi = s.newInstance(null);
            if (!(spi instanceof KeyManagerFactorySpi)) {
                throw new NoSuchAlgorithmException(
                        "el proveedor no devolvio un KeyManagerFactorySpi para " + algorithm);
            }
            return new KeyManagerFactory((KeyManagerFactorySpi) spi, p, algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (Exception e) {
            throw new NoSuchAlgorithmException(algorithm + " KeyManagerFactory not available", e);
        }
    }

    /** El proveedor que la produjo. */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Desde un almacen de claves y su contrasena.
     *
     * <p>La contrasena puede ser {@code null} si las claves no estan protegidas por separado; el
     * arreglo conviene limpiarlo despues, porque un {@code char[]} en memoria vive hasta que alguien
     * lo pise.
     */
    public final void init(KeyStore ks, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        this.factorySpi.engineInit(ks, password);
    }

    /** Desde parametros; ver {@link KeyStoreBuilderParameters}. */
    public final void init(ManagerFactoryParameters spec)
            throws InvalidAlgorithmParameterException {
        this.factorySpi.engineInit(spec);
    }

    /**
     * Los manejadores de claves, uno por tipo.
     *
     * @throws IllegalStateException si no se llamo antes a {@code init}
     */
    public final KeyManager[] getKeyManagers() {
        return this.factorySpi.engineGetKeyManagers();
    }
}
