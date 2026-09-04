package java.rmi;

/**
 * KajiLibrary's java.rmi.RMISecurityManager -- un {@link SecurityManager} sin nada propio.
 *
 * <p>Historia: hasta 1.1 esta clase tenia una politica propia para el codigo que llegaba por RMI.
 * Desde 1.2 no agrega nada sobre {@code SecurityManager}, y desde entonces usar una o la otra es
 * exactamente lo mismo.
 *
 * <p>Marcada para eliminacion junto con todo el mecanismo de {@code SecurityManager}, que ya no hace
 * nada. Se mantiene para que el codigo viejo compile.
 */
@Deprecated(since = "1.8", forRemoval = true)
public class RMISecurityManager extends SecurityManager {

    /** Igual que el de la clase base. */
    public RMISecurityManager() {
    }
}
