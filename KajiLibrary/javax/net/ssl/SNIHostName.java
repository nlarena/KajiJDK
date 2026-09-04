package javax.net.ssl;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Un nombre de host en la extension SNI: el unico tipo que hoy existe.
 *
 * <h2>Por que el nombre se guarda en ASCII y no como se escribio</h2>
 *
 * <p>Porque el protocolo transporta bytes y un nombre de dominio puede tener acentos. La conversion
 * a la forma ASCII (el {@code xn--...} de IDN) tiene que pasar <strong>una sola vez y en un solo
 * lugar</strong>: dos puntas que la hagan distinto no se reconocen, y el servidor manda el
 * certificado equivocado o corta.
 *
 * <p>Esta clase no implementa esa conversion — vive en {@code java.net.IDN} — pero si fija la otra
 * mitad de la regla: el nombre se compara <strong>sin distinguir mayusculas</strong>, porque los
 * nombres de dominio no las distinguen y {@code Ejemplo.com} tiene que matchear a {@code ejemplo.com}.
 */
public final class SNIHostName extends SNIServerName {

    private final String hostname;

    /**
     * Desde un nombre de host.
     *
     * @throws NullPointerException si es {@code null}
     * @throws IllegalArgumentException si esta vacio o termina en punto — un nombre absoluto con el
     *     punto final es valido en DNS pero no en SNI, y aceptarlo produciria una comparacion que
     *     nunca matchea
     */
    public SNIHostName(String hostname) {
        super(StandardConstants.SNI_HOST_NAME, bytes(hostname));
        this.hostname = hostname;
    }

    /**
     * Desde los bytes tal como vinieron del protocolo.
     *
     * @throws IllegalArgumentException si no son ASCII de siete bits, o si no forman un nombre
     *     valido
     */
    public SNIHostName(byte[] encoded) {
        super(StandardConstants.SNI_HOST_NAME, encoded);
        this.hostname = revisar(new String(encoded, StandardCharsets.US_ASCII));
    }

    private static byte[] bytes(String hostname) {
        if (hostname == null) {
            throw new NullPointerException("hostname");
        }
        return revisar(hostname).getBytes(StandardCharsets.US_ASCII);
    }

    private static String revisar(String hostname) {
        if (hostname.isEmpty()) {
            throw new IllegalArgumentException("el nombre de host esta vacio");
        }
        if (hostname.endsWith(".")) {
            throw new IllegalArgumentException("el nombre de host termina en punto: " + hostname);
        }
        for (int i = 0; i < hostname.length(); i++) {
            if (hostname.charAt(i) > 127) {
                throw new IllegalArgumentException(
                        "el nombre de host no es ASCII; convertirlo antes con java.net.IDN");
            }
        }
        return hostname;
    }

    /** El nombre en su forma ASCII. */
    public String getAsciiName() {
        return this.hostname;
    }

    /**
     * Sin distinguir mayusculas — ver la nota de la clase.
     *
     * <p>El {@link Locale#ENGLISH} en el {@code toLowerCase} no es decoracion: con el turco,
     * {@code "I"} baja a una i sin punto y {@code "INDEX"} dejaria de matchear a {@code "index"}.
     * Un nombre de dominio no depende del idioma de quien corre el programa.
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof SNIHostName) {
            SNIHostName o = (SNIHostName) other;
            return this.hostname.equalsIgnoreCase(o.hostname);
        }
        return false;
    }

    public int hashCode() {
        return 31 * 17 + this.hostname.toLowerCase(Locale.ENGLISH).hashCode();
    }

    public String toString() {
        return "type=host_name (0), value=" + this.hostname;
    }

    /**
     * Un {@link SNIMatcher} que acepta los nombres de host que casen con esa expresion regular.
     *
     * <p>Regular y no una lista literal porque un servidor sirve familias de nombres —todo un
     * dominio y sus subdominios— y enumerarlas seria imposible.
     *
     * @throws NullPointerException si {@code regex} es {@code null}
     * @throws java.util.regex.PatternSyntaxException si la expresion no compila
     */
    public static SNIMatcher createSNIMatcher(String regex) {
        if (regex == null) {
            throw new NullPointerException("regex");
        }
        return new SNIHostNameMatcher(regex);
    }
}
