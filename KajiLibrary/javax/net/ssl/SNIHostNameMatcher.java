package javax.net.ssl;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * El matcher que devuelve {@link SNIHostName#createSNIMatcher}.
 *
 * <p>De paquete y no publica: el JDK tampoco la expone. Nadie deberia poder construirla salvo por
 * esa fabrica, que es la que garantiza que la expresion ya fue validada.
 */
final class SNIHostNameMatcher extends SNIMatcher {

    private final Pattern pattern;

    SNIHostNameMatcher(String regex) {
        super(StandardConstants.SNI_HOST_NAME);
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Acepta un nombre de host que case con la expresion.
     *
     * <p>Un {@link SNIServerName} de otro tipo se rechaza sin mirarlo: este matcher solo entiende
     * nombres de host, y opinar sobre otra cosa seria inventar.
     */
    public boolean matches(SNIServerName serverName) {
        if (serverName == null) {
            throw new NullPointerException("serverName");
        }
        if (!(serverName instanceof SNIHostName)) {
            if (serverName.getType() != StandardConstants.SNI_HOST_NAME) {
                return false;
            }
            SNIHostName reconstruido = new SNIHostName(serverName.getEncoded());
            return this.pattern.matcher(
                    reconstruido.getAsciiName().toLowerCase(Locale.ENGLISH)).matches();
        }
        SNIHostName h = (SNIHostName) serverName;
        return this.pattern.matcher(h.getAsciiName().toLowerCase(Locale.ENGLISH)).matches();
    }
}
