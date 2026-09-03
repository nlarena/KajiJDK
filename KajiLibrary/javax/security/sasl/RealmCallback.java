package javax.security.sasl;

import javax.security.auth.callback.TextInputCallback;

/**
 * KajiLibrary's javax.security.sasl.RealmCallback -- preguntar en que dominio.
 *
 * <p>Un {@link TextInputCallback} que no agrega nada mas que su <b>tipo</b>. Eso es todo lo que
 * necesita: un manejador recibe una lista de callbacks y decide que hacer con cada uno mirando de
 * que clase es, asi que "pedir un dominio" y "pedir un texto cualquiera" tienen que ser tipos
 * distintos para poder responderse distinto.
 *
 * <p>El dominio --el <i>realm</i>-- es el espacio de nombres donde vale el usuario. El mismo nombre
 * puede ser dos personas distintas en dos dominios, y por eso el mecanismo lo pregunta aparte en vez
 * de esperar que venga pegado al usuario.
 */
public class RealmCallback extends TextInputCallback {

    private static final long serialVersionUID = -4342673378785456908L;

    /** Sin sugerencia. */
    public RealmCallback(String prompt) {
        super(prompt);
    }

    /**
     * Con una sugerencia.
     *
     * @param defaultRealmInfo el dominio que el servidor propone
     */
    public RealmCallback(String prompt, String defaultRealmInfo) {
        super(prompt, defaultRealmInfo);
    }
}
