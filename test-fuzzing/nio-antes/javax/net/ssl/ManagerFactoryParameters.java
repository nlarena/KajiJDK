package javax.net.ssl;

/**
 * Marks the objects that serve to initialize a {@link KeyManagerFactory} or a
 * {@link TrustManagerFactory} with something that is not a {@code KeyStore}.
 *
 * <p>No methods, and again on purpose: each way of initializing needs different data --see
 * {@link CertPathTrustManagerParameters} and {@link KeyStoreBuilderParameters}--, so the only thing
 * they share is being acceptable where initialization is asked for.
 */
public interface ManagerFactoryParameters {
}
