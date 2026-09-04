package javax.management.remote;

import java.io.Serializable;
import java.net.MalformedURLException;

/**
 * KajiLibrary's javax.management.remote.JMXServiceURL -- la direccion de un conector JMX.
 *
 * <p>La forma es {@code service:jmx:<protocolo>://[<host>][:<puerto>][<camino>]}. El prefijo
 * {@code service:jmx:} viene del estandar de URL de servicio y es obligatorio.
 *
 * <h2>La gramatica del host, que es mas estricta de lo que parece</h2>
 *
 * <p>Se acepta una de tres cosas:
 *
 * <ul>
 *   <li>una direccion IPv4 en cuatro numeros, cada uno de 0 a 255. {@code 1.2.3.4} vale;
 *       {@code 1.2.3}, {@code 1.2.3.4.5} y {@code 256.1.1.1} no;
 *   <li>una direccion IPv6 numerica, con o sin corchetes. Se detecta por tener dos puntos, y por eso
 *       cualquier host con {@code :} adentro se valida como IPv6 y falla si no lo es;
 *   <li>un nombre de maquina: etiquetas separadas por puntos, cada una de letras, digitos y guiones,
 *       sin empezar ni terminar en guion. La <b>primera</b> etiqueta puede empezar con digito
 *       --{@code 12a.b} vale-- pero las siguientes tienen que empezar con letra, asi que
 *       {@code abc.123} no vale. Es la regla que impide confundir un nombre con una direccion.
 * </ul>
 *
 * <p>Un host vacio esta permitido y significa "la maquina local, sin decir cual"; en ese caso el
 * puerto tiene que ser 0. Pasar null a los constructores de tres y cuatro argumentos es distinto:
 * ahi si se resuelve el nombre de la maquina.
 *
 * <h2>{@code hashCode} coherente con {@code equals}</h2>
 *
 * <p>{@link #equals} ignora mayusculas en el host, como manda el DNS. El JDK calcula
 * {@link #hashCode} sobre {@link #toString}, que conserva las mayusculas del host, asi que dos URL
 * iguales pueden tener hash distinto -- se comprobo contra el JDK 25 y es asi. Eso rompe el contrato
 * de {@code Object} y hace que una tabla hash con estas claves falle.
 *
 * <p>Aca el hash se calcula sobre la forma que usa {@code equals}, con el host en minusculas. Es la
 * unica divergencia deliberada de esta clase y es a favor del contrato.
 */
public class JMXServiceURL implements Serializable {

    private static final long serialVersionUID = 8173364409860779292L;

    /** El protocolo, en minusculas. */
    private final String protocol;

    /** El host, con las mayusculas que se hayan pasado. */
    private final String host;

    /** El puerto; 0 significa sin puerto. */
    private final int port;

    /** El camino, o vacio. */
    private final String urlPath;

    /** La forma de texto, que no cambia. */
    private transient String toString;

    /**
     * Analiza una URL completa.
     *
     * @throws MalformedURLException si no arranca con {@code service:jmx:} o algo no cierra
     * @throws NullPointerException si es null
     */
    public JMXServiceURL(String serviceURL) throws MalformedURLException {
        final String prefix = "service:jmx:";
        if (!serviceURL.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw new MalformedURLException("Service URL must start with " + prefix);
        }
        int at = prefix.length();
        final int sep = serviceURL.indexOf("://", at);
        if (sep < 0) {
            throw new MalformedURLException("Missing \"://\" after protocol name");
        }
        String proto = serviceURL.substring(at, sep);
        at = sep + 3;
        String rest = serviceURL.substring(at);
        String hostPart;
        String pathPart;
        int cut = firstIndexOf(rest, "/;");
        if (cut < 0) {
            hostPart = rest;
            pathPart = "";
        } else {
            hostPart = rest.substring(0, cut);
            pathPart = rest.substring(cut);
        }
        String hostText;
        int portValue = 0;
        if (hostPart.startsWith("[")) {
            int close = hostPart.indexOf(']');
            if (close < 0) {
                throw new MalformedURLException("Bad IPv6 address: " + hostPart);
            }
            hostText = hostPart.substring(0, close + 1);
            String after = hostPart.substring(close + 1);
            portValue = parsePort(after);
        } else {
            int colon = hostPart.lastIndexOf(':');
            if (colon >= 0) {
                hostText = hostPart.substring(0, colon);
                portValue = parsePort(hostPart.substring(colon));
            } else {
                hostText = hostPart;
            }
        }
        this.protocol = proto.toLowerCase();
        this.host = unbracket(hostText);
        this.port = portValue;
        this.urlPath = pathPart;
        validate();
    }

    /**
     * Arma una URL sin camino.
     *
     * @param protocol el protocolo; null significa {@code jmxmp}
     * @param host el host; null significa el nombre de esta maquina
     * @param port el puerto, o 0
     * @throws MalformedURLException si algo no cierra
     */
    public JMXServiceURL(String protocol, String host, int port) throws MalformedURLException {
        this(protocol, host, port, null);
    }

    /**
     * Arma una URL completa.
     *
     * @param urlPath el camino; tiene que empezar con {@code /} o {@code ;}, o ser vacio o null
     * @throws MalformedURLException si algo no cierra
     */
    public JMXServiceURL(String protocol, String host, int port, String urlPath)
        throws MalformedURLException {
        if (protocol == null) {
            protocol = "jmxmp";
        }
        if (host == null) {
            host = localHostName();
        }
        if (host.startsWith("[")) {
            if (!host.endsWith("]")) {
                throw new MalformedURLException("Host starts with [ but does not end with ]");
            }
            host = host.substring(1, host.length() - 1);
            if (!isNumericIPv6Address(host)) {
                throw new MalformedURLException(
                    "Address inside [...] must be numeric IPv6 address");
            }
        }
        if (urlPath == null) {
            urlPath = "";
        }
        this.protocol = protocol.toLowerCase();
        this.host = host;
        this.port = port;
        this.urlPath = urlPath;
        validate();
    }

    /** El protocolo, siempre en minusculas. */
    public String getProtocol() {
        return this.protocol;
    }

    /** El host, sin corchetes aunque sea IPv6, y con las mayusculas originales. */
    public String getHost() {
        return this.host;
    }

    /** El puerto, o 0 si no se dio. */
    public int getPort() {
        return this.port;
    }

    /** El camino, o vacio. */
    public String getURLPath() {
        return this.urlPath;
    }

    /**
     * La URL completa.
     *
     * <p>El puerto 0 no se escribe, y un host IPv6 sale entre corchetes.
     */
    @Override
    public String toString() {
        if (this.toString != null) {
            return this.toString;
        }
        StringBuilder sb = new StringBuilder("service:jmx:");
        sb.append(this.protocol).append("://");
        if (isNumericIPv6Address(this.host)) {
            sb.append('[').append(this.host).append(']');
        } else {
            sb.append(this.host);
        }
        if (this.port != 0) {
            sb.append(':').append(this.port);
        }
        sb.append(this.urlPath);
        this.toString = sb.toString();
        return this.toString;
    }

    /**
     * Protocolo y host sin distinguir mayusculas, puerto igual, camino exacto.
     *
     * <p>Que el camino si distinga no es un descuido: puede ser un nombre JNDI, y esos si distinguen.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JMXServiceURL)) {
            return false;
        }
        JMXServiceURL other = (JMXServiceURL) obj;
        return this.protocol.equalsIgnoreCase(other.protocol)
            && this.host.equalsIgnoreCase(other.host)
            && this.port == other.port
            && this.urlPath.equals(other.urlPath);
    }

    /** Coherente con {@link #equals}. Ver la nota de la clase. */
    @Override
    public int hashCode() {
        return this.protocol.hashCode() * 31 * 31 * 31
            + this.host.toLowerCase().hashCode() * 31 * 31
            + this.port * 31
            + this.urlPath.hashCode();
    }

    /** El nombre de esta maquina, o {@code localhost} si no se puede averiguar. */
    private static String localHostName() throws MalformedURLException {
        String name;
        try {
            name = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Throwable e) {
            return "localhost";
        }
        if (name == null || !isValidHostName(name)) {
            return "localhost";
        }
        return name;
    }

    /** Saca los corchetes de un host IPv6 escrito con ellos. */
    private static String unbracket(String h) {
        if (h.length() >= 2 && h.charAt(0) == '[' && h.charAt(h.length() - 1) == ']') {
            return h.substring(1, h.length() - 1);
        }
        return h;
    }

    /** El primer indice donde aparece alguno de esos caracteres, o -1. */
    private static int firstIndexOf(String s, String chars) {
        int i = 0;
        while (i < s.length()) {
            if (chars.indexOf(s.charAt(i)) >= 0) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /** Lee {@code ":1234"}; vacio significa 0. */
    private static int parsePort(String s) throws MalformedURLException {
        if (s.length() == 0) {
            return 0;
        }
        if (s.charAt(0) != ':') {
            throw new MalformedURLException("Bad port number: \"" + s + "\"");
        }
        String digits = s.substring(1);
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            throw new MalformedURLException("Bad port number: \"" + digits + "\": " + e);
        }
    }

    /** Los tres controles que comparten los constructores. */
    private void validate() throws MalformedURLException {
        if (!isValidProtocol(this.protocol)) {
            throw new MalformedURLException(
                "Missing or invalid protocol name: \"" + this.protocol + "\"");
        }
        if (this.host.length() == 0) {
            if (this.port != 0) {
                throw new MalformedURLException("Cannot give port number without host name");
            }
        } else if (isNumericIPv6Address(this.host)) {
            if (!isWellFormedIPv6(this.host)) {
                throw new MalformedURLException("Bad IPv6 address: " + this.host);
            }
        } else if (!isValidHostName(this.host)) {
            throw new MalformedURLException("Bad host: \"" + this.host + "\"");
        }
        if (this.port < 0) {
            throw new MalformedURLException("Bad port: " + this.port);
        }
        if (this.urlPath.length() > 0 && !this.urlPath.startsWith("/")
            && !this.urlPath.startsWith(";")) {
            throw new MalformedURLException("Bad URL path: " + this.urlPath);
        }
    }

    /** Una letra seguida de letras, digitos, {@code +} y {@code -}. */
    private static boolean isValidProtocol(String p) {
        if (p.length() == 0 || !isAlpha(p.charAt(0))) {
            return false;
        }
        int i = 1;
        while (i < p.length()) {
            char c = p.charAt(i);
            if (!isAlpha(c) && !isDigit(c) && c != '+' && c != '-') {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Un IPv4 en cuatro numeros, o un nombre. Ver la nota de la clase. */
    private static boolean isValidHostName(String h) {
        if (isNumericIPv4(h)) {
            return true;
        }
        int start = 0;
        boolean first = true;
        while (true) {
            int dot = h.indexOf('.', start);
            int end;
            if (dot < 0) {
                end = h.length();
            } else {
                end = dot;
            }
            if (!isValidLabel(h, start, end, first)) {
                return false;
            }
            if (dot < 0) {
                return true;
            }
            start = dot + 1;
            first = false;
        }
    }

    /**
     * Una etiqueta de nombre.
     *
     * @param first si es la primera; solo esa puede empezar con digito
     */
    private static boolean isValidLabel(String h, int start, int end, boolean first) {
        if (end <= start) {
            return false;
        }
        char firstChar = h.charAt(start);
        if (first) {
            if (!isAlpha(firstChar) && !isDigit(firstChar)) {
                return false;
            }
        } else if (!isAlpha(firstChar)) {
            return false;
        }
        if (!isAlpha(h.charAt(end - 1)) && !isDigit(h.charAt(end - 1))) {
            return false;
        }
        int i = start;
        while (i < end) {
            char c = h.charAt(i);
            if (!isAlpha(c) && !isDigit(c) && c != '-') {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Cuatro numeros de 0 a 255 separados por puntos. */
    private static boolean isNumericIPv4(String s) {
        int start = 0;
        int parts = 0;
        while (parts < 4) {
            int dot = s.indexOf('.', start);
            int end;
            if (parts == 3) {
                if (dot >= 0) {
                    return false;
                }
                end = s.length();
            } else {
                if (dot < 0) {
                    return false;
                }
                end = dot;
            }
            if (end <= start || end - start > 3) {
                return false;
            }
            int value = 0;
            int i = start;
            while (i < end) {
                char c = s.charAt(i);
                if (!isDigit(c)) {
                    return false;
                }
                value = value * 10 + (c - '0');
                i = i + 1;
            }
            if (value > 255) {
                return false;
            }
            start = end + 1;
            parts = parts + 1;
        }
        return true;
    }

    /** Un host con dos puntos se trata como IPv6, valido o no. Es lo que hace el JDK. */
    private static boolean isNumericIPv6Address(String s) {
        return s.indexOf(':') >= 0;
    }

    /**
     * Si ese texto es un literal IPv6 bien formado.
     *
     * <p>Acepta la abreviatura {@code ::} una sola vez, un IPv4 en el ultimo grupo, y un identificador
     * de ambito numerico despues de {@code %}.
     *
     * <p>Un ambito con <b>nombre</b> --{@code %eth0}-- se rechaza. El JDK lo acepta solo si esa placa
     * existe en la maquina, asi que no es una propiedad del texto sino del equipo; sin placas que
     * consultar, rechazarlo es lo unico que se puede decir con certeza.
     */
    private static boolean isWellFormedIPv6(String s) {
        int pct = s.indexOf('%');
        if (pct >= 0) {
            String scope = s.substring(pct + 1);
            if (scope.length() == 0) {
                return false;
            }
            int i = 0;
            while (i < scope.length()) {
                if (!isDigit(scope.charAt(i))) {
                    return false;
                }
                i = i + 1;
            }
            s = s.substring(0, pct);
        }
        int dbl = s.indexOf("::");
        String head;
        String tail;
        if (dbl >= 0) {
            if (s.indexOf("::", dbl + 1) >= 0) {
                return false;
            }
            head = s.substring(0, dbl);
            tail = s.substring(dbl + 2);
        } else {
            head = s;
            tail = null;
        }
        int[] headCount = new int[1];
        if (!countGroups(head, headCount, tail == null)) {
            return false;
        }
        if (tail == null) {
            return headCount[0] == 8;
        }
        int[] tailCount = new int[1];
        if (!countGroups(tail, tailCount, true)) {
            return false;
        }
        return headCount[0] + tailCount[0] <= 7;
    }

    /**
     * Cuenta los grupos de una mitad y dice si estan bien formados.
     *
     * @param allowIPv4 si el ultimo grupo puede ser un IPv4, que cuenta como dos
     */
    private static boolean countGroups(String s, int[] count, boolean allowIPv4) {
        if (s.length() == 0) {
            count[0] = 0;
            return true;
        }
        int total = 0;
        int start = 0;
        while (true) {
            int colon = s.indexOf(':', start);
            int end;
            if (colon < 0) {
                end = s.length();
            } else {
                end = colon;
            }
            if (colon < 0 && allowIPv4 && s.indexOf('.', start) >= 0) {
                if (!isNumericIPv4(s.substring(start))) {
                    return false;
                }
                total = total + 2;
                count[0] = total;
                return true;
            }
            if (end <= start || end - start > 4) {
                return false;
            }
            int i = start;
            while (i < end) {
                if (!isHex(s.charAt(i))) {
                    return false;
                }
                i = i + 1;
            }
            total = total + 1;
            if (colon < 0) {
                count[0] = total;
                return true;
            }
            start = colon + 1;
        }
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isHex(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
