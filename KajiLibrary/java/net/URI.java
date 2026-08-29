package java.net;

// Un URI segun RFC 3986, descompuesto en sus cinco piezas: esquema, autoridad, camino,
// consulta y fragmento. Es un tipo de VALOR: inmutable, con igualdad por contenido.
//
// Entro a la biblioteca porque `javax.tools.FileObject.toUri()` y el constructor de
// `SimpleJavaFileObject` lo piden — sin el, ese paquete no se puede cerrar.
//
// SUBCONJUNTO, y conviene decir que falta y por que:
//
//   - **Sin decodificacion de %XX.** Las variantes `getRawX()` existen y devuelven lo mismo
//     que las `getX()`. Un URI con escapes se lee tal cual. Decodificar exige tablas de
//     charset (`java.nio.charset` no existe) y cambiaria el valor devuelto en silencio, que
//     es peor que no hacerlo: aca al menos las dos formas coinciden y se puede razonar.
//   - **Sin `toURL()`** — necesita `java.net.URL`, que no existe.
//   - **Sin `resolve`/`relativize`/`normalize`/`parseServerAuthority`** — son algebra de
//     caminos relativos; nadie en la biblioteca los usa todavia.
//   - **Solo el constructor de un argumento.** Los otros cuatro del JDK arman el URI por
//     partes; el que hace falta es el que parsea.
//
// El parser usa `charAt` y `substring(int,int)` porque nuestro `String` no tiene `indexOf`
// ni `substring(int)` — de ahi los `scan*` de abajo.
public final class URI implements Comparable<URI> {

    private final String string;
    private final String scheme;
    private final String ssp;          // scheme-specific part: todo lo que sigue al "esquema:"
    private final String authority;
    private final String path;
    private final String query;
    private final String fragment;
    private final boolean opaque;

    public URI(String str) throws URISyntaxException {
        if (str == null) {
            throw new NullPointerException();
        }
        this.string = str;
        int len = str.length();

        // 1. esquema: letras/digitos/+-. hasta el primer ':', y solo si aparece antes que
        //    cualquier '/', '?' o '#'. "foo/bar:baz" NO tiene esquema.
        int colon = -1;
        int i = 0;
        while (i < len) {
            char c = str.charAt(i);
            if (c == ':') { colon = i; break; }
            if (c == '/' || c == '?' || c == '#') { break; }
            i = i + 1;
        }
        String sch = null;
        int rest = 0;
        if (colon > 0) {
            if (!isSchemeStart(str.charAt(0))) {
                throw new URISyntaxException(str, "Illegal character in scheme name", 0);
            }
            int k = 1;
            while (k < colon) {
                if (!isSchemeChar(str.charAt(k))) {
                    throw new URISyntaxException(str, "Illegal character in scheme name", k);
                }
                k = k + 1;
            }
            sch = str.substring(0, colon);
            rest = colon + 1;
        } else if (colon == 0) {
            throw new URISyntaxException(str, "Expected scheme name", 0);
        }
        this.scheme = sch;

        // 2. fragmento: desde el ultimo '#' hasta el final (no puede haber otro despues).
        int hash = scanFor(str, rest, len, '#');
        int endOfRest = (hash < 0) ? len : hash;
        this.fragment = (hash < 0) ? null : str.substring(hash + 1, len);

        this.ssp = str.substring(rest, endOfRest);

        // 3. Opaco = tiene esquema y su parte especifica NO arranca con '/'  ("mailto:x@y").
        //    En ese caso no hay autoridad, camino ni consulta que separar.
        this.opaque = (sch != null) && !(this.ssp.length() > 0 && this.ssp.charAt(0) == '/');
        if (this.opaque) {
            if (this.ssp.isEmpty()) {
                throw new URISyntaxException(str, "Expected scheme-specific part", rest);
            }
            this.authority = null;
            this.path = null;
            this.query = null;
            return;
        }

        // 4. jerarquico: [ "//" autoridad ] camino [ "?" consulta ]
        int p = rest;
        String auth = null;
        if (p + 1 < endOfRest && str.charAt(p) == '/' && str.charAt(p + 1) == '/') {
            int authStart = p + 2;
            int authEnd = authStart;
            while (authEnd < endOfRest) {
                char c = str.charAt(authEnd);
                if (c == '/' || c == '?') { break; }
                authEnd = authEnd + 1;
            }
            auth = str.substring(authStart, authEnd);
            p = authEnd;
        }
        this.authority = auth;

        int qmark = scanFor(str, p, endOfRest, '?');
        int pathEnd = (qmark < 0) ? endOfRest : qmark;
        this.path = str.substring(p, pathEnd);
        this.query = (qmark < 0) ? null : str.substring(qmark + 1, endOfRest);
    }

    // Igual que el constructor pero para strings que se sabe que estan bien (constantes del
    // programa): convierte el fallo en no chequeado, como en el JDK.
    public static URI create(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage());
        }
    }

    public String getScheme() { return this.scheme; }

    // Absoluto = tiene esquema. Es lo que distingue "http://a/b" de "/b".
    public boolean isAbsolute() { return this.scheme != null; }

    // Opaco = "mailto:x@y": hay esquema pero lo que sigue no es un camino jerarquico.
    public boolean isOpaque() { return this.opaque; }

    public String getSchemeSpecificPart() { return this.ssp; }

    public String getRawSchemeSpecificPart() { return this.ssp; }

    public String getAuthority() { return this.authority; }

    public String getRawAuthority() { return this.authority; }

    // De "user@host:port", la parte anterior al '@'.
    public String getUserInfo() {
        if (this.authority == null) { return null; }
        int at = scanFor(this.authority, 0, this.authority.length(), '@');
        return (at < 0) ? null : this.authority.substring(0, at);
    }

    public String getRawUserInfo() { return getUserInfo(); }

    public String getHost() {
        if (this.authority == null) { return null; }
        int len = this.authority.length();
        int start = scanFor(this.authority, 0, len, '@') + 1;   // -1 + 1 = 0 si no hay '@'
        int colon = scanFor(this.authority, start, len, ':');
        int end = (colon < 0) ? len : colon;
        return this.authority.substring(start, end);
    }

    // -1 cuando no hay puerto, igual que el JDK.
    public int getPort() {
        if (this.authority == null) { return -1; }
        int len = this.authority.length();
        int start = scanFor(this.authority, 0, len, '@') + 1;
        int colon = scanFor(this.authority, start, len, ':');
        if (colon < 0 || colon + 1 >= len) { return -1; }
        int value = 0;
        int i = colon + 1;
        while (i < len) {
            char c = this.authority.charAt(i);
            if (c < '0' || c > '9') { return -1; }
            value = value * 10 + (c - '0');
            i = i + 1;
        }
        return value;
    }

    public String getPath() { return this.path; }

    public String getRawPath() { return this.path; }

    public String getQuery() { return this.query; }

    public String getRawQuery() { return this.query; }

    public String getFragment() { return this.fragment; }

    public String getRawFragment() { return this.fragment; }

    public String toString() { return this.string; }

    // Sin decodificacion no hay nada que re-escapar: la forma ASCII es la original.
    public String toASCIIString() { return this.string; }

    public boolean equals(Object other) {
        if (other == this) { return true; }
        if (other == null) { return false; }
        if (!(other instanceof URI)) { return false; }
        URI that = (URI) other;
        return this.string.equals(that.string);
    }

    public int hashCode() { return this.string.hashCode(); }

    public int compareTo(URI that) { return this.string.compareTo(that.string); }

    // ---- helpers de parseo (nuestro String no tiene indexOf) ----

    // Primer `ch` en [from, to), o -1.
    private static int scanFor(String s, int from, int to, char ch) {
        int i = from;
        while (i < to) {
            if (s.charAt(i) == ch) { return i; }
            i = i + 1;
        }
        return -1;
    }

    private static boolean isSchemeStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isSchemeChar(char c) {
        return isSchemeStart(c) || (c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.';
    }
}
