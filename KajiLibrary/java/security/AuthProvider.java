package java.security;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

/**
 * Un {@link Provider} cuyas claves hay que desbloquear antes de usarlas.
 *
 * <h2>Por que un proveedor necesitaria autenticacion</h2>
 *
 * <p>Porque no todos guardan sus claves en un archivo. Un proveedor respaldado por una tarjeta
 * inteligente, un token USB o un HSM tiene las claves adentro del dispositivo, y el dispositivo pide
 * un PIN antes de dejar firmar con ellas. Ese estado —conectado o no— no existe en un
 * {@link Provider} comun, que se supone disponible desde que se lo registra.
 *
 * <p>De ahi los tres metodos: {@link #login} abre la sesion, {@link #logout} la cierra, y
 * {@link #setCallbackHandler} dice <strong>como</strong> se le pide el PIN al usuario — porque el
 * proveedor no sabe si hay una terminal, una ventana o un servicio del otro lado. Es el mismo
 * mecanismo de {@code javax.security.auth.callback} que usa JAAS, y por eso lo reusa.
 *
 * <p>Toda la clase es declarativa: quien la extiende es el proveedor concreto, que es el unico que
 * sabe hablar con su dispositivo.
 */
public abstract class AuthProvider extends Provider {

    private static final long serialVersionUID = 4197859053084546461L;

    /**
     * @deprecated usar el constructor que toma la version como {@link String}: un {@code double} no
     *     puede representar una version de tres partes, y {@code 1.10} es menor que {@code 1.9}
     */
    @Deprecated(since = "9")
    protected AuthProvider(String name, double version, String info) {
        super(name, version, info);
    }

    /** Con el nombre, la version y una descripcion. */
    protected AuthProvider(String name, String versionStr, String info) {
        super(name, versionStr, info);
    }

    /**
     * Abre la sesion con el dispositivo.
     *
     * @param subject donde dejar los principales que resulten, o {@code null}
     * @param handler como pedirle las credenciales al usuario; {@code null} usa el que se haya
     *     puesto con {@link #setCallbackHandler}
     * @throws LoginException si no se pudo
     */
    public abstract void login(Subject subject, CallbackHandler handler) throws LoginException;

    /**
     * Cierra la sesion.
     *
     * <p>Despues de esto las claves del dispositivo vuelven a no estar disponibles, que es el punto:
     * una aplicacion que termino de firmar no deberia dejar el token abierto.
     */
    public abstract void logout() throws LoginException;

    /**
     * Fija como se le piden las credenciales al usuario.
     *
     * <p>Se puede llamar antes de {@link #login} para que este no tenga que recibir uno.
     */
    public abstract void setCallbackHandler(CallbackHandler handler);
}
