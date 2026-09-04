package java.net;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// El permiso de hablar con una URL: que esquema, que host, que ruta, con que metodos y mandando que
// headers.
//
// Es el unico permiso de este paquete que se puede escribir entero sin red, y la razon esta en su
// propio contrato: **URLPermission no resuelve nombres**. Compara los textos tal como se los dieron
// -- no canonicaliza el host, no consulta DNS, no hace busqueda inversa. Eso lo convierte en
// computacion pura sobre cadenas, que es exactamente lo que se puede hacer aca.
//
// (`SocketPermission` es lo contrario y por eso no esta en este arbol: su `implies` esta definido en
// terminos de resolver los dos nombres y comparar direcciones. Ver mas abajo.)
//
// La gramatica de la ruta tiene tres formas y la diferencia importa:
//
//   /a/b     exactamente esa
//   /a/*     esa y **un** nivel mas: cubre /a/b pero no /a/b/c
//   /a/-     esa y todo lo que cuelgue, recursivo
//
// Las acciones son "metodos:headers", las dos listas separadas por comas, y `*` en cualquiera de las
// dos significa todos. Se normalizan --ordenadas, y siempre con los dos puntos-- para que dos
// permisos que dicen lo mismo se comparen iguales: "POST,GET" y "GET,POST" son el mismo permiso.
//
// ===========================================================================================
// POR QUE `SocketPermission` NO ESTA
// ===========================================================================================
//
// `SocketPermission.implies` esta especificado en terminos de resolucion de nombres: para decidir si
// el permiso sobre "ejemplo.org" cubre al permiso sobre "1.2.3.4" hay que resolver el primero y
// comparar direcciones. Sin resolver, la respuesta seria distinta de la del JDK **en silencio**, y
// siempre en la direccion de negar de mas. En una clase de seguridad eso es justamente lo peor: un
// permiso que contesta que no cuando el JDK contesta que si es un metodo que miente sobre el
// resultado de una decision de autorizacion. No declararlo hace que el codigo que lo necesita falle
// al compilar, que es donde se puede ver.
//
// Nada mas omitido de esta clase.
//
// @deprecated El Security Manager quedo deprecado para remocion; estos permisos ya no se chequean.
@Deprecated
public final class URLPermission extends Permission {

    private static final long serialVersionUID = -2702463814894478682L;

    private String scheme;
    private String ssp;
    private String path;
    private Authority authority;
    private List<String> methods;
    private List<String> requestHeaders;
    private String actions;

    /**
     * El permiso sobre esa URL, con esas acciones.
     *
     * @param url esquema://autoridad/ruta, o "esquema:*"
     * @param actions "metodos:headers"; los dos puntos son opcionales si no hay headers
     * @throws IllegalArgumentException si la URL o las acciones no se entienden
     */
    public URLPermission(String url, String actions) {
        super(url);
        this.init(actions);
    }

    /** El permiso sobre esa URL para todos los metodos y todos los headers ("*:*"). */
    public URLPermission(String url) {
        this(url, "*:*");
    }

    private void init(String actions) {
        this.parseURI(this.getName());
        int colon = actions.indexOf(':');
        // Un segundo ':' significaria un tercer campo que no existe; es un error de escritura, no
        // una lista de headers con dos puntos adentro.
        if (actions.lastIndexOf(':') != colon) {
            throw new IllegalArgumentException("Invalid actions string: \"" + actions + "\"");
        }
        String meths;
        String heads;
        if (colon == -1) {
            meths = actions;
            heads = "";
        } else {
            meths = actions.substring(0, colon);
            heads = actions.substring(colon + 1);
        }
        this.methods = normalize(meths, "methods");
        this.requestHeaders = normalize(heads, "headers");
        this.actions = this.buildActions();
    }

    private void parseURI(String url) {
        int len = url.length();
        int delim = url.indexOf(':');
        if (delim == -1 || delim + 1 == len) {
            throw new IllegalArgumentException("Invalid URL string: \"" + url + "\"");
        }
        this.scheme = url.substring(0, delim).toLowerCase();
        this.ssp = url.substring(delim + 1);
        if (!this.ssp.startsWith("//")) {
            // La unica forma sin autoridad que se admite es "esquema:*": todo ese esquema.
            if (!this.ssp.equals("*")) {
                throw new IllegalArgumentException("Invalid URL string: \"" + url + "\"");
            }
            this.authority = new Authority(this.scheme, "*");
            return;
        }
        String authpath = this.ssp.substring(2);
        delim = authpath.indexOf('/');
        String auth;
        if (delim == -1) {
            this.path = "";
            auth = authpath;
        } else {
            auth = authpath.substring(0, delim);
            this.path = authpath.substring(delim);
        }
        this.authority = new Authority(this.scheme, auth.toLowerCase());
    }

    // Ordenadas y con la caja canonica, para que dos permisos equivalentes tengan la misma cadena
    // de acciones. `campo` es "methods" o "headers" y decide dos cosas: como se normaliza la caja y
    // que dice el mensaje de error.
    //
    // Los repetidos NO se sacan --"GET,GET" queda "GET,GET"-- porque es lo que hace el JDK, y la
    // cadena de acciones es observable por `getActions`. Se habia hecho al reves y la prueba de
    // comportamiento lo agarro.
    //
    // El espacio en blanco es un error, no algo para recortar: un permiso escrito " GET " casi
    // siempre viene de una cadena armada a mano mal, y aceptarlo callado da un permiso que no es el
    // que se quiso escribir. El JDK tira, y el mensaje cita el campo ENTERO sin tocar, no el token.
    private static List<String> normalize(String s, String campo) {
        List<String> out = new ArrayList<String>();
        if (s == null || s.length() == 0) {
            return Collections.unmodifiableList(out);
        }
        int i = 0;
        while (i < s.length()) {
            if (Character.isWhitespace(s.charAt(i))) {
                throw new IllegalArgumentException(
                        "White space not allowed in " + campo + ": \"" + s + "\"");
            }
            i = i + 1;
        }
        int start = 0;
        while (start <= s.length()) {
            int comma = s.indexOf(',', start);
            String tok;
            if (comma == -1) {
                tok = s.substring(start);
                start = s.length() + 1;
            } else {
                tok = s.substring(start, comma);
                start = comma + 1;
            }
            if (tok.length() > 0) {
                out.add("methods".equals(campo) ? tok.toUpperCase() : canonHeader(tok));
            }
        }
        Collections.sort(out);
        return Collections.unmodifiableList(out);
    }

    // "accept" -> "Accept", "x-Y-z" -> "X-Y-Z": mayuscula inicial en cada tramo separado por
    // guion, minuscula el resto. Es la escritura canonica de un header HTTP, y normalizar a ella
    // --en vez de a minuscula-- es lo que hace que `getActions` devuelva algo que se pueda pegar
    // tal cual en una cabecera.
    private static String canonHeader(String tok) {
        StringBuilder b = new StringBuilder();
        boolean inicio = true;
        int i = 0;
        while (i < tok.length()) {
            char c = tok.charAt(i);
            if (c == '-') {
                b.append(c);
                inicio = true;
            } else {
                b.append(inicio ? Character.toUpperCase(c) : Character.toLowerCase(c));
                inicio = false;
            }
            i = i + 1;
        }
        return b.toString();
    }

    private String buildActions() {
        StringBuilder b = new StringBuilder();
        String sep = "";
        int i = 0;
        while (i < this.methods.size()) {
            b.append(sep).append(this.methods.get(i));
            sep = ",";
            i = i + 1;
        }
        b.append(':');
        sep = "";
        i = 0;
        while (i < this.requestHeaders.size()) {
            b.append(sep).append(this.requestHeaders.get(i));
            sep = ",";
            i = i + 1;
        }
        return b.toString();
    }

    /** Las acciones normalizadas, en la forma "metodos:headers". */
    public String getActions() {
        return this.actions;
    }

    /** Si este permiso cubre a {@code p}. Ver la cabecera para las tres formas de ruta. */
    public boolean implies(Permission p) {
        if (!(p instanceof URLPermission)) {
            return false;
        }
        URLPermission that = (URLPermission) p;
        if (this.methods.isEmpty() && !that.methods.isEmpty()) {
            return false;
        }
        if (!this.methods.isEmpty() && !this.methods.get(0).equals("*")
                && !this.methods.containsAll(that.methods)) {
            return false;
        }
        if (this.requestHeaders.isEmpty() && !that.requestHeaders.isEmpty()) {
            return false;
        }
        if (!this.requestHeaders.isEmpty() && !this.requestHeaders.get(0).equals("*")
                && !this.requestHeaders.containsAll(that.requestHeaders)) {
            return false;
        }
        if (!this.scheme.equals(that.scheme)) {
            return false;
        }
        if (this.ssp.equals("*")) {
            return true;
        }
        if (!this.authority.implies(that.authority)) {
            return false;
        }
        if (this.path == null) {
            return that.path == null;
        }
        if (that.path == null) {
            return false;
        }
        if (this.path.endsWith("/-")) {
            String prefix = this.path.substring(0, this.path.length() - 1);
            return that.path.startsWith(prefix);
        }
        if (this.path.endsWith("/*")) {
            String prefix = this.path.substring(0, this.path.length() - 1);
            if (!that.path.startsWith(prefix)) {
                return false;
            }
            String suffix = that.path.substring(prefix.length());
            // Un solo nivel: si queda una barra, la otra ruta baja mas hondo de lo permitido. Y "-"
            // como sufijo seria un comodin recursivo colandose por la puerta de atras.
            return suffix.indexOf('/') == -1 && !suffix.equals("-");
        }
        return this.path.equals(that.path);
    }

    public boolean equals(Object p) {
        if (!(p instanceof URLPermission)) {
            return false;
        }
        URLPermission that = (URLPermission) p;
        if (!this.scheme.equals(that.scheme)) {
            return false;
        }
        if (!this.getActions().equals(that.getActions())) {
            return false;
        }
        if (!this.authority.equals(that.authority)) {
            return false;
        }
        if (this.path != null) {
            return this.path.equals(that.path);
        }
        return that.path == null;
    }

    public int hashCode() {
        return this.getActions().hashCode()
                + this.scheme.hashCode()
                + this.authority.hashCode()
                + (this.path == null ? 0 : this.path.hashCode());
    }

    // La autoridad de la URL: host --con sus comodines-- y rango de puertos.
    //
    // Un host puede ser "*" (cualquiera), "*.dominio" (cualquiera dentro de ese dominio) o un
    // nombre/literal exacto. **Nunca se resuelve**: ver la cabecera de la clase.
    private static class Authority {

        private final String host;
        private final int portLow;
        private final int portHigh;

        Authority(String scheme, String authority) {
            String h = authority;
            int low;
            int high;
            int colon = h.lastIndexOf(':');
            // Un ':' dentro de corchetes es de un literal IPv6, no el separador del puerto.
            int bracket = h.lastIndexOf(']');
            if (colon != -1 && colon > bracket) {
                String p = h.substring(colon + 1);
                h = h.substring(0, colon);
                if (p.equals("*")) {
                    low = 0;
                    high = 65535;
                } else {
                    int dash = p.indexOf('-');
                    if (dash == -1) {
                        low = parsePort(p);
                        high = low;
                    } else if (dash == 0) {
                        low = 0;
                        high = parsePort(p.substring(1));
                    } else if (dash == p.length() - 1) {
                        low = parsePort(p.substring(0, dash));
                        high = 65535;
                    } else {
                        low = parsePort(p.substring(0, dash));
                        high = parsePort(p.substring(dash + 1));
                    }
                }
            } else {
                // Sin puerto vale el del esquema: comparar "http://x.com" con "http://x.com:80"
                // tiene que dar lo mismo, porque nombran lo mismo.
                low = defaultPort(scheme);
                high = low;
            }
            this.host = h;
            this.portLow = low;
            this.portHigh = high;
        }

        private static int parsePort(String p) {
            try {
                int v = Integer.parseInt(p);
                if (v < 0 || v > 65535) {
                    throw new IllegalArgumentException("Invalid port range");
                }
                return v;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port range");
            }
        }

        private static int defaultPort(String scheme) {
            if (scheme.equals("http")) {
                return 80;
            }
            if (scheme.equals("https")) {
                return 443;
            }
            return -1;
        }

        boolean implies(Authority that) {
            if (that.portLow < this.portLow || that.portHigh > this.portHigh) {
                return false;
            }
            if (this.host.equals("*")) {
                return true;
            }
            if (this.host.startsWith("*.")) {
                String domain = this.host.substring(1);
                return that.host.endsWith(domain);
            }
            return this.host.equals(that.host);
        }

        public boolean equals(Object o) {
            if (!(o instanceof Authority)) {
                return false;
            }
            Authority a = (Authority) o;
            return this.host.equals(a.host) && this.portLow == a.portLow
                    && this.portHigh == a.portHigh;
        }

        public int hashCode() {
            return this.host.hashCode() + this.portLow + this.portHigh;
        }
    }
}
