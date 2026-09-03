package javax.security.sasl;

import java.io.Serializable;
import javax.security.auth.callback.Callback;

/**
 * KajiLibrary's javax.security.sasl.AuthorizeCallback -- puede este actuar como aquel.
 *
 * <p>La pregunta que hace un servidor SASL despues de autenticar, y separa dos cosas que se
 * confunden todo el tiempo:
 *
 * <ul>
 *   <li>el <b>identificador de autenticacion</b> es quien probo ser: la persona o el proceso que
 *       presento la contrasena;
 *   <li>el <b>identificador de autorizacion</b> es en nombre de quien quiere actuar.
 * </ul>
 *
 * <p>Casi siempre son el mismo y no pasa nada. Cuando no lo son --un proceso de administracion que
 * quiere hacer algo como otro usuario, un proxy que reenvia-- es exactamente el punto donde hay que
 * decidir si eso se permite, y esta clase es donde el manejador lo decide.
 *
 * <h2>La respuesta tiene dos partes</h2>
 *
 * <p>{@link #setAuthorized} dice si se permite, y {@link #setAuthorizedID} deja ademas
 * <b>reescribir</b> el identificador. Reescribirlo es util porque el nombre que llega por el
 * protocolo casi nunca es el que el sistema usa por dentro: llega
 * {@code "juan@ejemplo.com"} y adentro se trabaja con {@code "uid=juan,ou=gente"}.
 *
 * <p>{@link #getAuthorizedID} devuelve null mientras no se haya autorizado, y el de autorizacion si
 * se autorizo sin reescribir. Que vuelva a null al desautorizar es lo correcto: un identificador
 * autorizado que sobrevive a la negativa es justo el que alguien va a leer sin mirar la bandera.
 */
public class AuthorizeCallback implements Callback, Serializable {

    private static final long serialVersionUID = -2353344186490470805L;

    /** Quien probo ser. */
    private final String authenticationID;

    /** En nombre de quien quiere actuar. */
    private final String authorizationID;

    /** Lo que el manejador contesto. */
    private boolean authorized = false;

    /** El identificador reescrito, o null para usar el de autorizacion. */
    private String authorizedID = null;

    /**
     * @param authnID quien probo ser
     * @param authzID en nombre de quien quiere actuar
     */
    public AuthorizeCallback(String authnID, String authzID) {
        this.authenticationID = authnID;
        this.authorizationID = authzID;
    }

    /** Quien probo ser. */
    public String getAuthenticationID() {
        return this.authenticationID;
    }

    /** En nombre de quien quiere actuar. */
    public String getAuthorizationID() {
        return this.authorizationID;
    }

    /** Lo que contesto el manejador. */
    public boolean isAuthorized() {
        return this.authorized;
    }

    /** Ver {@link #isAuthorized}. */
    public void setAuthorized(boolean ok) {
        this.authorized = ok;
    }

    /**
     * El identificador que hay que usar.
     *
     * @return null si no se autorizo; ver la nota de la clase
     */
    public String getAuthorizedID() {
        if (!this.authorized) {
            return null;
        }
        return (this.authorizedID == null) ? this.authorizationID : this.authorizedID;
    }

    /** Reescribe el identificador. Ver la nota de la clase sobre por que hace falta. */
    public void setAuthorizedID(String id) {
        this.authorizedID = id;
    }
}
