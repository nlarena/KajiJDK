package javax.net.ssl;

/**
 * Marca los objetos que sirven para inicializar una {@link KeyManagerFactory} o una
 * {@link TrustManagerFactory} con algo que no es un {@code KeyStore}.
 *
 * <p>Sin metodos, y de nuevo a proposito: cada forma de inicializar necesita datos distintos —ver
 * {@link CertPathTrustManagerParameters} y {@link KeyStoreBuilderParameters}—, asi que lo unico que
 * comparten es ser aceptables donde se pide inicializacion.
 */
public interface ManagerFactoryParameters {
}
