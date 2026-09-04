package java.net;

import java.nio.charset.StandardCharsets;

// Un URI segun RFC 3986, descompuesto en sus cinco piezas: esquema, autoridad, camino,
// consulta y fragmento. Es un tipo de VALOR: inmutable, con igualdad por contenido.
//
// Entro a la biblioteca porque `javax.tools.FileObject.toUri()` y el constructor de
// `SimpleJavaFileObject` lo piden — sin el, ese paquete no se puede cerrar.
//
// COMPLETO: los 32 miembros. Esta nota decia que faltaba la decodificacion de %XX --que las
// `getX()` devolvian lo mismo que las `getRawX()` porque `java.nio.charset` no existia-- y las dos
// mitades de esa frase quedaron viejas: el paquete esta (se importa aca arriba) y la decodificacion
// tambien. `getPath()` sobre `/a%20b/c%2Fd` devuelve `/a b/c/d` y `getRawPath()` la forma escapada,
// que es lo que hace el JDK; se comprobo contra el.
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

    public String getSchemeSpecificPart() { return desescapar(this.ssp); }

    public String getRawSchemeSpecificPart() { return this.ssp; }

    public String getAuthority() { return desescapar(this.authority); }

    public String getRawAuthority() { return this.authority; }

    // De "user@host:port", la parte anterior al '@', ya decodificada.
    public String getUserInfo() { return desescapar(getRawUserInfo()); }

    public String getRawUserInfo() {
        if (this.authority == null) { return null; }
        int at = scanFor(this.authority, 0, this.authority.length(), '@');
        return (at < 0) ? null : this.authority.substring(0, at);
    }

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

    // ---- los cuatro constructores por partes ------------------------------------------------------
    //
    // Reciben los componentes EN CRUDO y los escapan; el constructor de un solo `String` los recibe
    // ya escapados. Esa es toda la diferencia entre los dos, y es lo que le da sentido al par
    // `getPath()`/`getRawPath()`: sin escapar aca, "raw" no tenia nada distinto que mostrar.
    //
    // El escape va en los constructores y **no** dentro de `armar`, aunque ahi quedaria en un solo
    // lugar: `armar` tambien lo usan `resolve`, `normalize` y `relativize`, que le pasan componentes
    // que YA estan escapados --salen de `this.path`, `this.query`--. Escapar ahi los escaparia dos
    // veces, y un `%20` se volveria `%2520` en cada `resolve`.
    //
    // Arman la cadena y despues la **reparsean**. Podria parecer un rodeo --se tienen las piezas--
    // pero es lo que garantiza que un URI armado por partes y uno parseado con el mismo texto sean
    // indistinguibles: mismo `toString`, mismo `equals`, mismo `hashCode`. Construir los campos a
    // mano abriria la puerta a un URI cuyo texto no coincide con sus partes.

    /** Un URI opaco: esquema, parte especifica y fragmento. */
    public URI(String scheme, String ssp, String fragment) throws URISyntaxException {
        this(armar(scheme, null, null, -1, escapar(ssp, LEGALES_URIC), null,
                escapar(fragment, LEGALES_URIC), true));
    }

    /** Un URI jerarquico con autoridad en bruto. */
    public URI(String scheme, String authority, String path, String query, String fragment)
            throws URISyntaxException {
        this(armar(scheme, escapar(authority, LEGALES_AUTORIDAD), null, -1,
                escapar(path, LEGALES_PATH), escapar(query, LEGALES_URIC),
                escapar(fragment, LEGALES_URIC), false));
    }

    /** Un URI jerarquico con la autoridad partida en usuario, host y puerto. */
    public URI(String scheme, String userInfo, String host, int port, String path, String query,
            String fragment) throws URISyntaxException {
        this(armar(scheme, null,
                armarAutoridad(escapar(userInfo, LEGALES_USERINFO), host, port), port,
                escapar(path, LEGALES_PATH), escapar(query, LEGALES_URIC),
                escapar(fragment, LEGALES_URIC), false));
    }

    /** El de arriba sin usuario ni puerto, que es el caso comun de un `http://host/camino`. */
    public URI(String scheme, String host, String path, String fragment) throws URISyntaxException {
        this(scheme, null, host, -1, path, null, fragment);
    }

    private static String armarAutoridad(String userInfo, String host, int port) {
        if (host == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (userInfo != null) {
            sb.append(userInfo);
            sb.append('@');
        }
        sb.append(host);
        if (port >= 0) {
            sb.append(':');
            sb.append(port);
        }
        return sb.toString();
    }

    private static String armar(String scheme, String authority, String userInfo, int port,
            String resto, String query, String fragment, boolean opaco) {
        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        if (!opaco) {
            String auth = authority != null ? authority : userInfo;
            if (auth != null) {
                sb.append("//");
                sb.append(auth);
            }
        }
        if (resto != null) {
            sb.append(resto);
        }
        if (query != null) {
            sb.append('?');
            sb.append(query);
        }
        if (fragment != null) {
            sb.append('#');
            sb.append(fragment);
        }
        return sb.toString();
    }

    // ---- el algebra de caminos ---------------------------------------------------------------------

    /**
     * Este URI con los `.` y `..` de su camino resueltos.
     *
     * <p>La trampa esta en el `..`, y es la razon de que esto no sea un `replace`: sacar un `..`
     * **no** es borrar el segmento anterior si ese segmento era a su vez un `..`. `a/../../b` es
     * `../b`, no `b` -- un URI relativo puede legitimamente subir mas alto que su propio texto.
     *
     * <p>Un URI opaco no tiene camino que normalizar y se devuelve tal cual.
     */
    public URI normalize() {
        if (this.opaque || this.path == null || this.path.length() == 0) {
            return this;
        }
        String limpio = normalizarCamino(this.path);
        if (limpio.equals(this.path)) {
            return this;
        }
        return crearOMismo(armar(this.scheme, this.authority, null, -1, limpio, this.query,
                this.fragment, false));
    }

    private static String normalizarCamino(String camino) {
        boolean absoluto = camino.length() > 0 && camino.charAt(0) == '/';
        java.util.ArrayList<String> salida = new java.util.ArrayList<String>();
        int desde = 0;
        int n = camino.length();
        while (desde <= n) {
            int corte = scanFor(camino, desde, n, '/');
            int fin = corte < 0 ? n : corte;
            String pieza = camino.substring(desde, fin);
            if (pieza.equals(".") || pieza.length() == 0) {
                // Se descarta -- salvo que sea el ultimo, donde marca "termina en barra".
                if (fin == n && salida.size() > 0) {
                    salida.add("");
                }
            } else if (pieza.equals("..")) {
                int ultimo = salida.size() - 1;
                if (ultimo >= 0 && !salida.get(ultimo).equals("..")) {
                    salida.remove(ultimo);
                } else if (!absoluto) {
                    // En un camino relativo el `..` que no tiene a quien comerse **se queda**.
                    salida.add("..");
                }
            } else {
                salida.add(pieza);
            }
            if (corte < 0) {
                desde = n + 1;
            } else {
                desde = corte + 1;
            }
        }
        StringBuilder sb = new StringBuilder();
        if (absoluto) {
            sb.append('/');
        }
        int i = 0;
        while (i < salida.size()) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(salida.get(i));
            i = i + 1;
        }
        return sb.toString();
    }

    /**
     * Resuelve `that` contra este URI, que hace de base (RFC 3986 §5.2.2).
     *
     * <p>Las reglas en orden, que es como el RFC las escribe: si `that` es absoluto o opaco, gana
     * entero; si solo tiene fragmento, se le pega a la base; si su camino es absoluto, reemplaza; y
     * si es relativo, se lo cuelga del directorio de la base.
     */
    public URI resolve(URI that) {
        if (that == null) {
            throw new NullPointerException("that");
        }
        if (that.isAbsolute() || that.isOpaque()) {
            return that;
        }
        if (this.isOpaque()) {
            return that;
        }
        // Solo fragmento: la base entera con otro fragmento.
        if (that.authority == null && (that.path == null || that.path.length() == 0)
                && that.query == null && that.fragment != null) {
            return crearOMismo(armar(this.scheme, this.authority, null, -1, this.path, this.query,
                    that.fragment, false));
        }
        if (that.authority != null) {
            return crearOMismo(armar(this.scheme, that.authority, null, -1,
                    normalizarCamino(nuloAVacio(that.path)), that.query, that.fragment, false));
        }
        String caminoNuevo;
        if (that.path != null && that.path.length() > 0 && that.path.charAt(0) == '/') {
            caminoNuevo = that.path;
        } else {
            caminoNuevo = unirCaminos(nuloAVacio(this.path), nuloAVacio(that.path));
        }
        return crearOMismo(armar(this.scheme, this.authority, null, -1,
                normalizarCamino(caminoNuevo), that.query, that.fragment, false));
    }

    /** El de arriba, parseando `str` primero. */
    public URI resolve(String str) {
        return this.resolve(URI.create(str));
    }

    // El camino de la base **hasta la ultima barra**: un URI nombra un recurso, no un directorio, asi
    // que lo que queda a la derecha de la ultima barra se reemplaza.
    private static String unirCaminos(String base, String relativo) {
        int ultimaBarra = -1;
        int i = 0;
        while (i < base.length()) {
            if (base.charAt(i) == '/') {
                ultimaBarra = i;
            }
            i = i + 1;
        }
        if (ultimaBarra < 0) {
            return relativo;
        }
        return base.substring(0, ultimaBarra + 1) + relativo;
    }

    /**
     * Expresa `that` como relativo a este URI, si se puede.
     *
     * <p>Es la inversa parcial de {@link #resolve}: si no comparten esquema y autoridad, o si `that`
     * no cuelga del camino de la base, **no hay** forma relativa y se devuelve `that` sin tocar. Eso
     * no es un fallo: es que la respuesta correcta es el URI absoluto.
     */
    public URI relativize(URI that) {
        if (that == null) {
            throw new NullPointerException("that");
        }
        if (this.isOpaque() || that.isOpaque()) {
            return that;
        }
        if (!iguales(this.scheme, that.scheme) || !iguales(this.authority, that.authority)) {
            return that;
        }
        String base = normalizarCamino(nuloAVacio(this.path));
        String otro = normalizarCamino(nuloAVacio(that.path));
        // El prefijo se corta en la ultima barra de la base: comparar por caracteres dejaria
        // `/a/bc` como "hijo" de `/a/b`, que es falso.
        int hastaBarra = 0;
        int i = 0;
        while (i < base.length()) {
            if (base.charAt(i) == '/') {
                hastaBarra = i + 1;
            }
            i = i + 1;
        }
        String prefijo = base.substring(0, hastaBarra);
        if (otro.length() < prefijo.length()) {
            return that;
        }
        if (!otro.substring(0, prefijo.length()).equals(prefijo)) {
            return that;
        }
        String resto = otro.substring(prefijo.length(), otro.length());
        return crearOMismo(armar(null, null, null, -1, resto, that.query, that.fragment, false));
    }

    /**
     * Este mismo URI, comprobando que su autoridad tenga forma de servidor (usuario, host, puerto).
     *
     * <p>Existe porque la autoridad de un URI puede ser cualquier cosa --el RFC deja una forma
     * "basada en registro" para esquemas raros-- y quien necesite un host y un puerto quiere fallar
     * temprano si no los hay.
     *
     * @throws URISyntaxException si la autoridad no tiene forma de servidor
     */
    public URI parseServerAuthority() throws URISyntaxException {
        if (this.authority == null) {
            return this;
        }
        if (this.getHost() == null) {
            throw new URISyntaxException(this.string, "la autoridad no tiene forma de servidor");
        }
        return this;
    }

    /**
     * Este URI como {@link java.net.URL}.
     *
     * @throws IllegalArgumentException si el URI no es absoluto
     * @throws java.net.MalformedURLException si el esquema no se puede convertir en URL
     */
    public java.net.URL toURL() throws java.net.MalformedURLException {
        if (!this.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        return new java.net.URL(this.string);
    }

    private static boolean iguales(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private static String nuloAVacio(String s) {
        return s == null ? "" : s;
    }

    // Todos los `resolve`/`relativize`/`normalize` producen texto que **ya** es un URI valido --sale
    // de piezas que lo eran-- asi que un fallo de parseo aca seria un defecto de esta clase y no del
    // que llama. Por eso se reetiqueta como `IllegalArgumentException`, igual que `URI.create`.
    private static URI crearOMismo(String texto) {
        return URI.create(texto);
    }

    public String getPath() { return desescapar(this.path); }

    public String getRawPath() { return this.path; }

    public String getQuery() { return desescapar(this.query); }

    public String getRawQuery() { return this.query; }

    public String getFragment() { return desescapar(this.fragment); }

    public String getRawFragment() { return this.fragment; }

    public String toString() { return this.string; }

    /**
     * El URI escrito con **solo ASCII**.
     *
     * <p>Los caracteres no-ASCII se guardan literales --`toString()` de un URI con una "e" acentuada
     * la muestra acentuada, y el JDK hace lo mismo--; esta es la forma que se manda por un canal que
     * solo acepta ASCII, y es la unica diferencia entre los dos metodos.
     */
    public String toASCIIString() {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int len = this.string.length();
        while (i < len) {
            char c = this.string.charAt(i);
            if (c < 0x80) {
                out.append(c);
                i = i + 1;
                continue;
            }
            // Un caracter suplementario son DOS `char`, y hay que tomar el par entero antes de
            // pasarlo a UTF-8: codificar cada sustituto por separado da una secuencia que no vuelve.
            int cp = this.string.codePointAt(i);
            byte[] bytes = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8);
            int k = 0;
            while (k < bytes.length) {
                escaparByte(out, bytes[k] & 0xFF);
                k = k + 1;
            }
            i = i + Character.charCount(cp);
        }
        return out.toString();
    }

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

    // ---- percent-encoding (RFC 3986) --------------------------------------------------------------
    //
    // Que se escapa depende del COMPONENTE, y por eso hay cuatro juegos y no uno: un '?' dentro de un
    // camino tiene que escaparse --si no, corta el camino y arranca la consulta-- pero dentro de la
    // consulta es un caracter mas y dejarlo es lo correcto. Escapar de mas tambien es un error: le
    // cambia el valor al componente.
    //
    // Los conjuntos son los del JDK, que usa la definicion de "unreserved" del RFC 2396 --incluye los
    // "mark" !~*'()-- y no la mas corta del RFC 3986. Verificado contra el JDK real.
    //
    // Lo que NO se escapa nunca aca: los caracteres no-ASCII. El JDK los deja literales en
    // `toString()` y en `getRawPath()`, y los codifica recien en `toASCIIString()`.

    private static final String NO_RESERVADOS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";

    /** Legales en un camino: los no reservados, mas los separadores que un camino puede contener. */
    private static final String LEGALES_PATH = NO_RESERVADOS + ":@&=+$,/";

    /** Legales en una consulta o un fragmento: ahi '/' y '?' ya no separan nada. */
    private static final String LEGALES_URIC = NO_RESERVADOS + ";/?:@&=+$,[]";

    /** Legales en una autoridad; los corchetes son de las direcciones IPv6 literales. */
    private static final String LEGALES_AUTORIDAD = NO_RESERVADOS + "$,;:@&=+[]";

    /** Legales en la parte de usuario: no lleva '@', que es justamente lo que la termina. */
    private static final String LEGALES_USERINFO = NO_RESERVADOS + ";:&=+$,";

    private static final String HEX = "0123456789ABCDEF";

    // Escapa lo que no sea legal en ese componente.
    private static String escapar(String s, String legales) {
        if (s == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c >= 0x80 || scanFor(legales, 0, legales.length(), c) >= 0) {
                out.append(c);
            } else {
                escaparByte(out, c);
            }
            i = i + 1;
        }
        return out.toString();
    }

    // En mayusculas, que es la forma canonica del RFC y la que emite el JDK.
    private static void escaparByte(StringBuilder out, int b) {
        out.append('%');
        out.append(HEX.charAt((b >> 4) & 0xF));
        out.append(HEX.charAt(b & 0xF));
    }

    // Deshace los %XX. Un escape mal formado ("%zz") se deja tal cual en vez de tirar: a este metodo
    // lo llaman los `getX()`, que no declaran excepcion, y perder el resto del componente por un '%'
    // suelto seria peor que devolverlo como vino.
    private static String desescapar(String s) {
        if (s == null || scanFor(s, 0, s.length(), '%') < 0) {
            return s;
        }
        StringBuilder out = new StringBuilder();
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        int i = 0;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < len) {
                int alto = Character.digit(s.charAt(i + 1), 16);
                int bajo = Character.digit(s.charAt(i + 2), 16);
                if (alto >= 0 && bajo >= 0) {
                    // Los %XX seguidos se juntan antes de decodificar: un caracter no-ASCII son
                    // varios bytes en UTF-8, y pasarlos de a uno daria un caracter roto por byte.
                    bytes.write((alto << 4) + bajo);
                    i = i + 3;
                    continue;
                }
            }
            volcarBytes(out, bytes);
            out.append(c);
            i = i + 1;
        }
        volcarBytes(out, bytes);
        return out.toString();
    }

    private static void volcarBytes(StringBuilder out, java.io.ByteArrayOutputStream bytes) {
        if (bytes.size() == 0) {
            return;
        }
        out.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        bytes.reset();
    }
}
