package java.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

// Una cookie HTTP: un par nombre-valor con reglas sobre a quien se le manda de vuelta.
//
// La clase carga con **tres especificaciones que se contradicen**: el borrador original de Netscape,
// el RFC 2109 y el RFC 2965. La diferencia visible es la version: una cookie version 0 se imprime
// "n=v" y usa el atributo `Expires`; una version 1 se imprime con comillas y `$Path`/`$Domain`, y
// usa `Max-Age`. `parse` no elige la version por gusto sino que la **adivina del texto**, y las
// reglas de esa adivinanza estan en `guessVersion`: si aparece "expires=" es Netscape, si aparece
// "version=" o "max-age" es RFC. Eso explica algo que confunde a todo el mundo: parsear "foo=bar"
// da version 0, pero `new HttpCookie("foo","bar")` da version 1.
//
// La otra consecuencia de la mezcla: el separador entre cookies. En el formato RFC varias cookies
// vienen en un header separadas por comas, y en el de Netscape la coma es parte legal de la fecha
// de `Expires`. Por eso `parse` solo parte por comas cuando adivino version 1 -- y aun ahi respeta
// las comillas.
//
// ===========================================================================================
// LO UNICO QUE NO ES IDENTICO AL JDK: EL PARSEO DE `Expires`
// ===========================================================================================
//
// El JDK prueba seis patrones de `SimpleDateFormat` uno tras otro. Aca la fecha se parsea con un
// lector propio que reconoce las mismas seis formas --dia de la semana, dia, mes, ano de dos o
// cuatro digitos, hora-- pero **no** reproduce las rarezas de `SimpleDateFormat` con entradas
// malformadas. La regla del ano de dos digitos si es la misma que la del RFC 6265 y la del JDK:
// menor a 70 es 20xx, si no 19xx.
//
// Cuando la fecha no se entiende, el resultado es `maxAge = 0`, o sea "ya vencida", que es tambien
// lo que hace el JDK. El modo de falla es conservador: una cookie que no se guarda, nunca una que se
// guarda de mas.
//
// Nada mas omitido. Una cookie es un dato, no una conexion.
public final class HttpCookie implements Cloneable {

    // Que el maximo no este seteado se codifica con -1, que ademas es el valor que el usuario puede
    // poner para decir "hasta que se cierre el navegador". Los dos significan "no vence sola".
    private static final long MAX_AGE_UNSPECIFIED = -1;

    private static final String SET_COOKIE = "set-cookie:";
    private static final String SET_COOKIE2 = "set-cookie2:";

    // Lo que no puede aparecer en un token del RFC 2616. El espacio esta adentro a proposito.
    private static final String TSPECIALS = ",; ";

    private final String name;
    private String value;

    private String comment;
    private String commentURL;
    private boolean toDiscard;
    private String domain;
    private long maxAge = MAX_AGE_UNSPECIFIED;
    private String path;
    private String portlist;
    private boolean secure;
    private boolean httpOnly;
    private int version = 1;

    // El momento en que se creo, que es contra lo que se mide `maxAge`. Sin este campo `hasExpired`
    // no tendria origen: "dura cien segundos" no dice nada sin decir desde cuando.
    private final long whenCreated;

    /**
     * Una cookie nueva con ese nombre y ese valor.
     *
     * <p>Nace en version 1 (RFC 2965). Ver la cabecera: `parse` puede darle version 0.
     *
     * @throws IllegalArgumentException si el nombre no es un token, esta vacio, o empieza con '$'
     *     (ese prefijo lo reserva el protocolo para sus propios atributos)
     */
    public HttpCookie(String name, String value) {
        this(name, value, System.currentTimeMillis());
    }

    HttpCookie(String name, String value, long creationTime) {
        name = name.trim();
        if (name.length() == 0 || !isToken(name) || name.charAt(0) == '$') {
            throw new IllegalArgumentException("Illegal cookie name");
        }
        this.name = name;
        this.value = value;
        this.whenCreated = creationTime;
    }

    /**
     * Las cookies que describe un header {@code Set-Cookie} o {@code Set-Cookie2}.
     *
     * <p>El prefijo del header puede venir o no. Los atributos que no se reconocen se ignoran en
     * silencio, que es lo que manda el protocolo: un servidor nuevo no tiene que romper un cliente
     * viejo.
     *
     * @throws IllegalArgumentException si el texto no tiene un par nombre-valor valido al principio
     */
    public static List<HttpCookie> parse(String header) {
        int version = guessVersion(header);
        String body = header;
        if (startsWithIgnoreCase(body, SET_COOKIE2)) {
            body = body.substring(SET_COOKIE2.length());
        } else if (startsWithIgnoreCase(body, SET_COOKIE)) {
            body = body.substring(SET_COOKIE.length());
        }
        List<HttpCookie> cookies = new ArrayList<HttpCookie>();
        if (version == 0) {
            // Netscape: la coma es parte de la fecha de `Expires`, asi que no se parte por comas y
            // el header trae una sola cookie.
            HttpCookie cookie = parseInternal(body);
            cookie.setVersion(0);
            cookies.add(cookie);
        } else {
            List<String> parts = splitMultiCookies(body);
            int i = 0;
            while (i < parts.size()) {
                HttpCookie cookie = parseInternal(parts.get(i));
                cookie.setVersion(1);
                cookies.add(cookie);
                i = i + 1;
            }
        }
        return cookies;
    }

    /** Si ya vencio segun su {@code maxAge} y el momento en que se creo. */
    public boolean hasExpired() {
        if (this.maxAge == 0) {
            return true;
        }
        if (this.maxAge == MAX_AGE_UNSPECIFIED) {
            return false;
        }
        long delta = (System.currentTimeMillis() - this.whenCreated) / 1000;
        return delta > this.maxAge;
    }

    /** El proposito de la cookie, para mostrarle al usuario. Solo version 1. */
    public void setComment(String purpose) {
        this.comment = purpose;
    }

    public String getComment() {
        return this.comment;
    }

    /** Una URL donde se explica el proposito. Solo version 1. */
    public void setCommentURL(String purpose) {
        this.commentURL = purpose;
    }

    public String getCommentURL() {
        return this.commentURL;
    }

    /** Si el cliente deberia tirarla al cerrarse, ignorando su vencimiento. Solo version 1. */
    public void setDiscard(boolean discard) {
        this.toDiscard = discard;
    }

    public boolean getDiscard() {
        return this.toDiscard;
    }

    /** Los puertos a los que se le puede mandar, separados por comas. Solo version 1. */
    public void setPortlist(String ports) {
        this.portlist = ports;
    }

    public String getPortlist() {
        return this.portlist;
    }

    /** El dominio al que pertenece. Se guarda en minusculas: los dominios no distinguen caja. */
    public void setDomain(String pattern) {
        if (pattern != null) {
            this.domain = pattern.toLowerCase();
        } else {
            this.domain = null;
        }
    }

    public String getDomain() {
        return this.domain;
    }

    /** Segundos de vida. Cero la vence en el acto; negativo significa "hasta cerrar el cliente". */
    public void setMaxAge(long expiry) {
        this.maxAge = expiry;
    }

    public long getMaxAge() {
        return this.maxAge;
    }

    /** El prefijo de ruta al que se le manda. */
    public void setPath(String uri) {
        this.path = uri;
    }

    public String getPath() {
        return this.path;
    }

    /** Si solo viaja por conexiones seguras. */
    public void setSecure(boolean flag) {
        this.secure = flag;
    }

    public boolean getSecure() {
        return this.secure;
    }

    public String getName() {
        return this.name;
    }

    public void setValue(String newValue) {
        this.value = newValue;
    }

    public String getValue() {
        return this.value;
    }

    /** 0 para el formato Netscape, 1 para el del RFC 2965. */
    public int getVersion() {
        return this.version;
    }

    /**
     * @throws IllegalArgumentException si no es 0 ni 1
     */
    public void setVersion(int v) {
        if (v != 0 && v != 1) {
            throw new IllegalArgumentException("cookie version should be 0 or 1");
        }
        this.version = v;
    }

    /** Si es invisible para el codigo de la pagina (la defensa clasica contra robo por XSS). */
    public boolean isHttpOnly() {
        return this.httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     * Si a {@code host} le corresponden las cookies de {@code domain}, segun el RFC 2965.
     *
     * <p>Las reglas raras de aca son todas defensivas y vale la pena leerlas al reves: existen para
     * que ".com" **no** matchee con "banco.com". Un dominio tiene que tener un punto interno, el
     * sobrante del host no puede tener puntos --asi ".foo.com" cubre "x.foo.com" pero no
     * "a.b.foo.com"--, y ".foo.com" cubre a "foo.com" pelado como caso especial.
     */
    public static boolean domainMatches(String domain, String host) {
        if (domain == null || host == null) {
            return false;
        }
        boolean isLocalDomain = ".local".equalsIgnoreCase(domain);
        int embeddedDot = domain.indexOf('.');
        if (embeddedDot == 0) {
            embeddedDot = domain.indexOf('.', 1);
        }
        if (!isLocalDomain && (embeddedDot == -1 || embeddedDot == domain.length() - 1)) {
            return false;
        }
        int firstDotInHost = host.indexOf('.');
        if (firstDotInHost == -1 && isLocalDomain) {
            return true;
        }
        int lengthDiff = host.length() - domain.length();
        if (lengthDiff == 0) {
            return host.equalsIgnoreCase(domain);
        }
        if (lengthDiff > 0) {
            String h = host.substring(0, lengthDiff);
            String d = host.substring(lengthDiff);
            return h.indexOf('.') == -1 && d.equalsIgnoreCase(domain);
        }
        if (lengthDiff == -1) {
            return domain.charAt(0) == '.' && host.equalsIgnoreCase(domain.substring(1));
        }
        return false;
    }

    /** La cookie en el formato que corresponde a su version. */
    public String toString() {
        if (this.getVersion() > 0) {
            return this.toRFC2965HeaderString();
        }
        return this.getName() + "=" + this.getValue();
    }

    private String toRFC2965HeaderString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName()).append("=\"").append(this.getValue()).append('"');
        if (this.getPath() != null) {
            sb.append(";$Path=\"").append(this.getPath()).append('"');
        }
        if (this.getDomain() != null) {
            sb.append(";$Domain=\"").append(this.getDomain()).append('"');
        }
        if (this.getPortlist() != null) {
            sb.append(";$Port=\"").append(this.getPortlist()).append('"');
        }
        return sb.toString();
    }

    // Dos cookies son la misma si coinciden nombre, dominio y ruta -- **no** el valor. Es del RFC
    // 2965 y es lo que hace que guardar una cookie nueva pise a la anterior en vez de acumularlas:
    // esos tres campos son la identidad, el valor es el contenido.
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HttpCookie)) {
            return false;
        }
        HttpCookie other = (HttpCookie) obj;
        return equalsIgnoreCase(this.getName(), other.getName())
                && equalsIgnoreCase(this.getDomain(), other.getDomain())
                && Objects.equals(this.getPath(), other.getPath());
    }

    public int hashCode() {
        int h1 = this.name.toLowerCase().hashCode();
        int h2 = (this.domain != null) ? this.domain.toLowerCase().hashCode() : 0;
        int h3 = (this.path != null) ? this.path.hashCode() : 0;
        return h1 + h2 + h3;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    long getCreationTime() {
        return this.whenCreated;
    }

    // ---- parseo ---------------------------------------------------------------------------------

    // La version sale del texto, no de un atributo: ver la cabecera.
    private static int guessVersion(String header) {
        String h = header.toLowerCase();
        if (h.indexOf("expires=") != -1) {
            return 0;
        }
        if (h.indexOf("version=") != -1) {
            return 1;
        }
        if (h.indexOf("max-age") != -1) {
            return 1;
        }
        if (startsWithIgnoreCase(h, SET_COOKIE2)) {
            return 1;
        }
        return 0;
    }

    private static HttpCookie parseInternal(String header) {
        StringTokenizer tokenizer = new StringTokenizer(header, ";");
        if (!tokenizer.hasMoreTokens()) {
            throw new IllegalArgumentException("Empty cookie header string");
        }
        String pair = tokenizer.nextToken();
        int index = pair.indexOf('=');
        if (index == -1) {
            throw new IllegalArgumentException("Invalid cookie name-value pair");
        }
        String n = pair.substring(0, index).trim();
        String v = pair.substring(index + 1).trim();
        HttpCookie cookie = new HttpCookie(n, stripOffSurroundingQuote(v));
        while (tokenizer.hasMoreTokens()) {
            pair = tokenizer.nextToken();
            index = pair.indexOf('=');
            String an;
            String av;
            if (index != -1) {
                an = pair.substring(0, index).trim();
                av = pair.substring(index + 1).trim();
            } else {
                an = pair.trim();
                av = null;
            }
            assignAttribute(cookie, an, av);
        }
        return cookie;
    }

    // El "primero gana" de casi todos los atributos no es capricho: un header con el atributo
    // repetido es sospechoso, y quedarse con el primero es la unica eleccion que no depende del
    // orden en que un intermediario los haya reordenado.
    private static void assignAttribute(HttpCookie cookie, String attrName, String attrValue) {
        attrValue = stripOffSurroundingQuote(attrValue);
        String key = attrName.toLowerCase();
        if (key.equals("comment")) {
            if (cookie.getComment() == null) {
                cookie.setComment(attrValue);
            }
        } else if (key.equals("commenturl")) {
            if (cookie.getCommentURL() == null) {
                cookie.setCommentURL(attrValue);
            }
        } else if (key.equals("discard")) {
            cookie.setDiscard(true);
        } else if (key.equals("domain")) {
            if (cookie.getDomain() == null) {
                cookie.setDomain(attrValue);
            }
        } else if (key.equals("max-age")) {
            long maxage;
            try {
                maxage = Long.parseLong(attrValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Illegal cookie max-age attribute");
            }
            if (cookie.getMaxAge() == MAX_AGE_UNSPECIFIED) {
                cookie.setMaxAge(maxage);
            }
        } else if (key.equals("path")) {
            if (cookie.getPath() == null) {
                cookie.setPath(attrValue);
            }
        } else if (key.equals("port")) {
            if (cookie.getPortlist() == null) {
                cookie.setPortlist(attrValue == null ? "" : attrValue);
            }
        } else if (key.equals("secure")) {
            cookie.setSecure(true);
        } else if (key.equals("httponly")) {
            cookie.setHttpOnly(true);
        } else if (key.equals("version")) {
            try {
                cookie.setVersion(Integer.parseInt(attrValue));
            } catch (NumberFormatException e) {
                // Un numero de version que no se entiende no invalida la cookie.
            }
        } else if (key.equals("expires")) {
            if (cookie.getMaxAge() == MAX_AGE_UNSPECIFIED) {
                long delta = cookie.expiryDate2DeltaSeconds(attrValue);
                cookie.setMaxAge(delta > 0 ? delta : 0);
            }
        }
        // Cualquier otro atributo se ignora: ver el javadoc de `parse`.
    }

    // Convierte una fecha absoluta de `Expires` en los segundos que le quedan de vida contados desde
    // que esta cookie se creo. Cero --o menos-- significa vencida.
    private long expiryDate2DeltaSeconds(String dateString) {
        long millis = parseCookieDate(dateString);
        if (millis == Long.MIN_VALUE) {
            return 0;
        }
        return (millis - this.whenCreated) / 1000;
    }

    private static final String[] MONTHS = {
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};

    // Lector de las fechas de cookie. Ver la cabecera para el alcance exacto. Devuelve
    // `Long.MIN_VALUE` cuando no entiende.
    static long parseCookieDate(String s) {
        if (s == null) {
            return Long.MIN_VALUE;
        }
        int day = -1;
        int month = -1;
        int year = -1;
        boolean twoDigitYear = false;
        int hh = -1;
        int mm = -1;
        int ss = -1;
        StringTokenizer st = new StringTokenizer(s, " ,-\t");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.indexOf(':') != -1) {
                if (hh != -1) {
                    return Long.MIN_VALUE;
                }
                StringTokenizer ht = new StringTokenizer(tok, ":");
                int[] parts = new int[3];
                int k = 0;
                while (ht.hasMoreTokens() && k < 3) {
                    parts[k] = parseUnsigned(ht.nextToken());
                    if (parts[k] < 0) {
                        return Long.MIN_VALUE;
                    }
                    k = k + 1;
                }
                if (k != 3 || ht.hasMoreTokens()) {
                    return Long.MIN_VALUE;
                }
                hh = parts[0];
                mm = parts[1];
                ss = parts[2];
                continue;
            }
            int m = monthIndex(tok);
            if (m >= 0) {
                if (month != -1) {
                    return Long.MIN_VALUE;
                }
                month = m;
                continue;
            }
            int num = parseUnsigned(tok);
            if (num >= 0) {
                // Un token numerico es el dia si todavia no hay dia y entra en un mes; si no, el
                // ano. Es la unica ambiguedad de estas gramaticas y se resuelve por posicion.
                if (day == -1 && tok.length() <= 2 && num >= 1 && num <= 31) {
                    day = num;
                } else if (year == -1) {
                    year = num;
                    twoDigitYear = tok.length() <= 2;
                } else {
                    return Long.MIN_VALUE;
                }
                continue;
            }
            // Dia de la semana, "GMT", desplazamientos horarios: se ignoran. Todas las formas que
            // acepta el JDK tienen la hora en GMT.
        }
        if (day == -1 || month == -1 || year == -1 || hh == -1) {
            return Long.MIN_VALUE;
        }
        if (twoDigitYear) {
            // La regla del RFC 6265, igual que la del JDK.
            if (year < 70) {
                year = year + 2000;
            } else {
                year = year + 1900;
            }
        }
        if (hh > 23 || mm > 59 || ss > 59 || day > daysInMonth(month, year)) {
            return Long.MIN_VALUE;
        }
        long days = daysFromCivil(year, month + 1, day);
        return ((days * 24 + hh) * 60 + mm) * 60000L + ss * 1000L;
    }

    private static int monthIndex(String tok) {
        if (tok.length() < 3) {
            return -1;
        }
        String p = tok.substring(0, 3).toLowerCase();
        int i = 0;
        while (i < MONTHS.length) {
            if (MONTHS[i].equals(p)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    private static int parseUnsigned(String tok) {
        if (tok.length() == 0) {
            return -1;
        }
        int v = 0;
        int i = 0;
        while (i < tok.length()) {
            char c = tok.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
            if (v > 999999) {
                return -1;
            }
            i = i + 1;
        }
        return v;
    }

    private static boolean isLeap(int y) {
        return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0;
    }

    private static int daysInMonth(int month0, int year) {
        int[] d = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (month0 == 1 && isLeap(year)) {
            return 29;
        }
        return d[month0];
    }

    // Dias desde 1970-01-01. El truco es correr el ano para que empiece en marzo: asi el dia
    // bisiesto queda al final y la cuenta de dias por mes se vuelve una formula sin tabla.
    private static long daysFromCivil(int y, int m, int d) {
        long yy = y;
        yy = yy - (m <= 2 ? 1 : 0);
        long era = (yy >= 0 ? yy : yy - 399) / 400;
        long yoe = yy - era * 400;
        long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    // ---- utilidades de texto --------------------------------------------------------------------

    private static boolean isToken(String value) {
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c < 0x20 || c >= 0x7f || TSPECIALS.indexOf(c) != -1) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static String stripOffSurroundingQuote(String str) {
        if (str != null && str.length() > 2 && str.charAt(0) == '"'
                && str.charAt(str.length() - 1) == '"') {
            return str.substring(1, str.length() - 1);
        }
        if (str != null && str.length() > 2 && str.charAt(0) == '\''
                && str.charAt(str.length() - 1) == '\'') {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static boolean equalsIgnoreCase(String s, String t) {
        if (s == t) {
            return true;
        }
        if (s != null && t != null) {
            return s.equalsIgnoreCase(t);
        }
        return false;
    }

    private static boolean startsWithIgnoreCase(String s, String start) {
        if (s == null || start == null) {
            return false;
        }
        return s.length() >= start.length()
                && start.equalsIgnoreCase(s.substring(0, start.length()));
    }

    // Parte por comas, pero solo por las que estan fuera de comillas: la lista de puertos se escribe
    // Port="80,81" y esa coma no separa cookies.
    private static List<String> splitMultiCookies(String header) {
        List<String> cookies = new ArrayList<String>();
        int quoteCount = 0;
        int q = 0;
        int p = 0;
        while (p < header.length()) {
            char c = header.charAt(p);
            if (c == '"') {
                quoteCount = quoteCount + 1;
            }
            if (c == ',' && (quoteCount % 2 == 0)) {
                cookies.add(header.substring(q, p));
                q = p + 1;
            }
            p = p + 1;
        }
        cookies.add(header.substring(q));
        return cookies;
    }
}
