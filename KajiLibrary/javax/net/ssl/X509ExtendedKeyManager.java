package javax.net.ssl;

import java.security.Principal;

/**
 * Un {@link X509KeyManager} que tambien sabe elegir para un {@link SSLEngine}.
 *
 * <h2>Por que hizo falta una clase mas</h2>
 *
 * <p>Porque {@link X509KeyManager} recibe un {@link java.net.Socket} para decidir, y un
 * {@code SSLEngine} no tiene socket: es justamente la abstraccion que separo el protocolo del
 * transporte. Agregarle metodos a la interfaz habria roto a todo el que ya la implementaba, asi que
 * los nuevos llegaron en una clase abstracta con cuerpo.
 *
 * <p>Esos cuerpos devuelven {@code null}, que significa "no tengo credencial para esto". Es la
 * respuesta segura: no elegir nada es peor que fallar, pero mucho mejor que presentar una credencial
 * que no corresponde.
 */
public abstract class X509ExtendedKeyManager implements X509KeyManager {

    /** Para las subclases. */
    protected X509ExtendedKeyManager() {
    }

    /** Elige el alias de cliente para un motor; {@code null} si ninguno sirve. */
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return null;
    }

    /** Elige el alias de servidor para un motor; {@code null} si ninguno sirve. */
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return null;
    }
}
