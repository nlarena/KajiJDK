package javax.net.ssl;

import javax.net.ServerSocketFactory;

/**
 * Fabrica de {@link SSLServerSocket}.
 *
 * <p>El espejo de {@link SSLSocketFactory} del lado servidor, y con el mismo proposito: que el
 * codigo que abre el puerto no tenga que saber si lo que acepta va cifrado.
 *
 * <p>Sin proveedor de TLS instalado, {@link #getDefault} devuelve una fabrica que falla al usarse en
 * vez de {@code null} — ver la nota de {@link SSLSocketFactory}.
 */
public abstract class SSLServerSocketFactory extends ServerSocketFactory {

    private static SSLServerSocketFactory laDefault;

    protected SSLServerSocketFactory() {
    }

    /** La fabrica por omision, sacada del {@link SSLContext} por omision. */
    public static synchronized ServerSocketFactory getDefault() {
        if (laDefault == null) {
            try {
                laDefault =
                        (SSLServerSocketFactory) SSLContext.getDefault().getServerSocketFactory();
            } catch (Exception e) {
                laDefault = new DefaultSSLServerSocketFactory(e);
            }
        }
        return laDefault;
    }

    /** Las suites habilitadas por omision en lo que fabrique. */
    public abstract String[] getDefaultCipherSuites();

    /** Todas las suites que se podrian habilitar. */
    public abstract String[] getSupportedCipherSuites();
}
