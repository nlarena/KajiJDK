package javax.security.auth.callback;

import java.util.Locale;

/**
 * KajiLibrary's javax.security.auth.callback.LanguageCallback -- en que idioma hablarle al usuario.
 *
 * <p>Es el unico callback <b>sin prompt</b>, y tiene sentido: preguntar en que idioma se pregunta
 * seria circular. La aplicacion lo contesta con lo que ya sabe -- la configuracion del sistema, la
 * cabecera de una peticion HTTP, la preferencia guardada del usuario.
 */
public class LanguageCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = 2019050433478903213L;

    private Locale locale;

    public LanguageCallback() {
    }

    /** Fija el idioma. Lo llama quien contesta. */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /** El idioma, o null si todavia nadie contesto. */
    public Locale getLocale() {
        return this.locale;
    }
}
