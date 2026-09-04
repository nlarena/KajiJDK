package javax.net.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

/**
 * Lo que un proveedor implementa para que exista una {@link KeyManagerFactory}.
 *
 * <p>Es el patron SPI del JDK: la clase publica hace la busqueda de proveedor, valida los
 * argumentos y fija el orden de las llamadas; esta hace el trabajo. Separarlos permite cambiar el
 * proveedor sin tocar el codigo que lo usa, que es la razon de que toda
 * {@code java.security} este partida en dos asi.
 */
public abstract class KeyManagerFactorySpi {

    public KeyManagerFactorySpi() {
    }

    /** Inicializa desde un almacen de claves y su contrasena. */
    protected abstract void engineInit(KeyStore ks, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException;

    /** Inicializa desde parametros que no son un almacen; ver {@link ManagerFactoryParameters}. */
    protected abstract void engineInit(ManagerFactoryParameters spec)
            throws InvalidAlgorithmParameterException;

    /**
     * Los manejadores, uno por tipo de clave.
     *
     * @throws IllegalStateException si no se inicializo antes
     */
    protected abstract KeyManager[] engineGetKeyManagers();
}
