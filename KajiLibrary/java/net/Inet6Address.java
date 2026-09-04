package java.net;

// Una direccion IPv6: dieciseis bytes, y opcionalmente un scope.
//
// El scope es la parte que sorprende. Una direccion link-local como fe80::1 **no identifica un
// host**: identifica un host *en un enlace*, y la misma direccion puede existir en dos placas
// distintas de la misma maquina. Por eso el literal admite el sufijo "%N", y por eso el scope es
// parte del objeto pero **no** de `equals`: dos objetos con la misma direccion y distinto scope son
// iguales, porque la direccion es la misma; lo que cambia es por donde se sale. El JDK hace
// exactamente esto y no es un descuido.
//
// El scope se guarda con un flag aparte y no como "cero significa ninguno", porque cero es un scope
// legal: `getByAddress(host, addr, 0)` produce una direccion que se imprime "...%0", mientras que
// `ofLiteral("::1")` no imprime nada. Colapsarlos perderia esa diferencia.
//
// Sobre la forma "IPv4-mapped" (::ffff:a.b.c.d): esa direccion **es** una IPv4, y tanto los parsers
// como `InetAddress.getByAddress` la colapsan a `Inet4Address`. Es lo que hace el JDK, y la razon es
// que si no, la misma maquina tendria dos objetos distintos y no iguales para la misma direccion.
// La forma "IPv4-compatible" (::a.b.c.d, sin los ffff) **no** se colapsa: esa es una IPv6 de verdad,
// deprecada pero distinta.
//
// El scope se puede nombrar de las dos formas que el JDK admite: por numero
// (`getByAddress(String, byte[], int)`, `getScopeId()`) y por placa
// (`getByAddress(String, byte[], NetworkInterface)`, `getScopedInterface()`). Esta clase decia que
// la segunda no entraba porque `NetworkInterface` no existia en este arbol; ya existe, y entra.
//
// Las dos formas no son intercambiables y por eso se guardan las dos: de una placa se saca su
// indice, pero de un indice **no** se saca la placa sin volver a enumerar --y el JDK devuelve `null`
// en `getScopedInterface()` cuando el scope se dio como numero, no la placa de ese indice--.
public final class Inet6Address extends InetAddress {

    private static final long serialVersionUID = 6880410070516793377L;

    static final int INADDRSZ = 16;

    private final int scopeId;
    private final boolean scopeIdSet;

    // La placa con la que se creo, si se creo con una. `transient` porque la forma serializada de
    // esta clase --la del JDK, que este arbol respeta-- lleva el scope como numero y nada mas: una
    // placa no se puede reconstruir en otra maquina, y guardarla cambiaria el formato.
    private final transient NetworkInterface scopedInterface;

    Inet6Address(String hostName, byte[] addr) {
        super(hostName, addr);
        this.scopeId = 0;
        this.scopeIdSet = false;
        this.scopedInterface = null;
    }

    Inet6Address(String hostName, byte[] addr, int scopeId) {
        super(hostName, addr);
        // Un scope negativo se ignora en vez de rechazarse: es como el JDK distingue "no me pasaron
        // scope" de "me pasaron el scope cero".
        if (scopeId >= 0) {
            this.scopeId = scopeId;
            this.scopeIdSet = true;
        } else {
            this.scopeId = 0;
            this.scopeIdSet = false;
        }
        // Un scope dado como numero no nombra ninguna placa: ver `getScopedInterface`.
        this.scopedInterface = null;
    }

    private int b(int i) {
        return this.addr[i] & 0xff;
    }

    /** ff00::/8. */
    public boolean isMulticastAddress() {
        return this.b(0) == 0xff;
    }

    /** La direccion sin especificar, "::". */
    public boolean isAnyLocalAddress() {
        int i = 0;
        while (i < INADDRSZ) {
            if (this.addr[i] != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** "::1", y solo esa. */
    public boolean isLoopbackAddress() {
        int i = 0;
        while (i < 15) {
            if (this.addr[i] != 0) {
                return false;
            }
            i = i + 1;
        }
        return this.addr[15] == 1;
    }

    /** fe80::/10. */
    public boolean isLinkLocalAddress() {
        return this.b(0) == 0xfe && (this.b(1) & 0xc0) == 0x80;
    }

    /** fec0::/10 (deprecada, pero el predicado sigue significando lo mismo). */
    public boolean isSiteLocalAddress() {
        return this.b(0) == 0xfe && (this.b(1) & 0xc0) == 0xc0;
    }

    /** Multicast con alcance 0xe (global). */
    public boolean isMCGlobal() {
        return this.b(0) == 0xff && (this.b(1) & 0x0f) == 0x0e;
    }

    /** Multicast con alcance 0x1 (interface-local). */
    public boolean isMCNodeLocal() {
        return this.b(0) == 0xff && (this.b(1) & 0x0f) == 0x01;
    }

    /** Multicast con alcance 0x2 (link-local). */
    public boolean isMCLinkLocal() {
        return this.b(0) == 0xff && (this.b(1) & 0x0f) == 0x02;
    }

    /** Multicast con alcance 0x5 (site-local). */
    public boolean isMCSiteLocal() {
        return this.b(0) == 0xff && (this.b(1) & 0x0f) == 0x05;
    }

    /** Multicast con alcance 0x8 (organization-local). */
    public boolean isMCOrgLocal() {
        return this.b(0) == 0xff && (this.b(1) & 0x0f) == 0x08;
    }

    /** Si los primeros doce bytes son cero: la forma "::a.b.c.d" del RFC 4291, ya deprecada. */
    public boolean isIPv4CompatibleAddress() {
        int i = 0;
        while (i < 12) {
            if (this.addr[i] != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public byte[] getAddress() {
        return copy(this.addr);
    }

    /** El scope numerico, o 0 si no tiene. */
    public int getScopeId() {
        return this.scopeId;
    }

    public String getHostAddress() {
        String s = numericToTextFormat(this.addr);
        if (this.scopeIdSet) {
            return s + "%" + this.scopeId;
        }
        return s;
    }

    // Suma de los cuatro grupos de cuatro bytes, leidos como enteros con signo. Es el algoritmo del
    // JDK; no es gran cosa como dispersion, pero cambiarlo haria que dos JDK no coincidan en el
    // orden de iteracion de un HashSet de direcciones, y eso se nota.
    public int hashCode() {
        int hash = 0;
        int i = 0;
        while (i < INADDRSZ) {
            int component = 0;
            int j = 0;
            while (j < 4 && i < INADDRSZ) {
                component = (component << 8) + this.addr[i];
                j = j + 1;
                i = i + 1;
            }
            hash = hash + component;
        }
        return hash;
    }

    // Sin el scope: ver la cabecera.
    public boolean equals(Object obj) {
        if (!(obj instanceof Inet6Address)) {
            return false;
        }
        Inet6Address other = (Inet6Address) obj;
        int i = 0;
        while (i < INADDRSZ) {
            if (this.addr[i] != other.addr[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // ---- factorias ------------------------------------------------------------------------------

    /**
     * La direccion IPv6 con ese nombre, esos bytes y ese scope.
     *
     * <p>Un {@code scopeId} negativo cuenta como "sin scope".
     *
     * @throws UnknownHostException si {@code addr} no mide 16
     */
    public static Inet6Address getByAddress(String host, byte[] addr, int scopeId)
            throws UnknownHostException {
        if (host != null && host.length() > 0 && host.charAt(0) == '[') {
            if (host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
            }
        }
        if (addr == null || addr.length != INADDRSZ) {
            throw new UnknownHostException("addr is of illegal length");
        }
        return new Inet6Address(host, copy(addr), scopeId);
    }

    /**
     * La direccion que describe el literal IPv6 {@code s}, con o sin corchetes.
     *
     * <p>Devuelve un {@link Inet4Address} si el literal es de la forma IPv4-mapped, por lo que el
     * tipo declarado es {@code InetAddress} y no {@code Inet6Address}.
     *
     * <p>El scope solo se acepta en forma numerica: un "%eth0" nombraria una placa, y este arbol no
     * modela placas.
     *
     * @throws IllegalArgumentException si no es un literal IPv6 valido
     */
    public static InetAddress ofLiteral(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        InetAddress a = parseLiteral(s, true);
        if (a == null) {
            throw invalidLiteral(s);
        }
        return a;
    }

    static String numericToTextFormat(byte[] src) {
        StringBuilder sb = new StringBuilder(39);
        int i = 0;
        while (i < 8) {
            if (i > 0) {
                sb.append(':');
            }
            int group = ((src[i * 2] & 0xff) << 8) | (src[i * 2 + 1] & 0xff);
            sb.append(Integer.toHexString(group));
            i = i + 1;
        }
        return sb.toString();
    }

    // Los primeros diez bytes en cero y los dos siguientes en 0xff: la marca de una IPv4 escrita como
    // IPv6. Devuelve los cuatro bytes reales, o null si no es de esa forma.
    static byte[] convertFromIPv4MappedAddress(byte[] addr) {
        if (addr.length != INADDRSZ) {
            return null;
        }
        int i = 0;
        while (i < 10) {
            if (addr[i] != 0) {
                return null;
            }
            i = i + 1;
        }
        if ((addr[10] & 0xff) != 0xff || (addr[11] & 0xff) != 0xff) {
            return null;
        }
        return new byte[] {addr[12], addr[13], addr[14], addr[15]};
    }

    // El parser del RFC 4291, con "::" y con cola IPv4. Devuelve null --no tira-- para que los
    // llamadores encadenen intentos: `InetAddress.ofLiteral` prueba IPv4 primero y IPv6 despues.
    static InetAddress parseLiteral(String s, boolean allowBrackets) {
        if (s == null) {
            return null;
        }
        if (allowBrackets && s.length() > 2 && s.charAt(0) == '['
                && s.charAt(s.length() - 1) == ']') {
            s = s.substring(1, s.length() - 1);
        }
        int scope = -1;
        int pct = s.indexOf('%');
        if (pct != -1) {
            if (pct == s.length() - 1) {
                return null;
            }
            String tail = s.substring(pct + 1);
            long v = 0;
            int i = 0;
            while (i < tail.length()) {
                int d = digit(tail.charAt(i), 10);
                // Un scope no numerico nombraria una placa de red; ver la cabecera.
                if (d < 0) {
                    return null;
                }
                v = v * 10 + d;
                if (v > 0x7fffffffL) {
                    return null;
                }
                i = i + 1;
            }
            scope = (int) v;
            s = s.substring(0, pct);
        }
        byte[] bytes = textToNumericFormat(s);
        if (bytes == null) {
            return null;
        }
        byte[] v4 = convertFromIPv4MappedAddress(bytes);
        if (v4 != null) {
            return new Inet4Address(null, v4);
        }
        if (scope >= 0) {
            return new Inet6Address(null, bytes, scope);
        }
        return new Inet6Address(null, bytes);
    }

    static byte[] textToNumericFormat(String src) {
        int len = src.length();
        // "::" es el literal mas corto que existe.
        if (len < 2) {
            return null;
        }
        byte[] dst = new byte[INADDRSZ];
        // Donde estaba el "::", para saber cuantos ceros insertar despues.
        int colonp = -1;
        int i = 0;
        int j = 0;
        if (src.charAt(i) == ':') {
            i = i + 1;
            if (src.charAt(i) != ':') {
                return null;
            }
        }
        int curtok = i;
        boolean sawDigit = false;
        int val = 0;
        while (i < len) {
            char ch = src.charAt(i);
            i = i + 1;
            int chval = digit(ch, 16);
            if (chval != -1) {
                val = (val << 4) | chval;
                if (val > 0xffff) {
                    return null;
                }
                sawDigit = true;
                continue;
            }
            if (ch == ':') {
                curtok = i;
                if (!sawDigit) {
                    if (colonp != -1) {
                        return null;
                    }
                    colonp = j;
                    continue;
                }
                if (i == len) {
                    return null;
                }
                if (j + 2 > INADDRSZ) {
                    return null;
                }
                dst[j] = (byte) ((val >> 8) & 0xff);
                dst[j + 1] = (byte) (val & 0xff);
                j = j + 2;
                sawDigit = false;
                val = 0;
                continue;
            }
            if (ch == '.' && (j + 4) <= INADDRSZ) {
                String tail = src.substring(curtok);
                // La cola tiene que ser una IPv4 completa: "::1.2.3" no es un literal, aunque
                // "1.2.3" solo si lo sea.
                int dots = 0;
                int k = 0;
                while (k < tail.length()) {
                    if (tail.charAt(k) == '.') {
                        dots = dots + 1;
                    }
                    k = k + 1;
                }
                if (dots != 3) {
                    return null;
                }
                byte[] v4 = Inet4Address.textToNumericFormat(tail);
                if (v4 == null) {
                    return null;
                }
                dst[j] = v4[0];
                dst[j + 1] = v4[1];
                dst[j + 2] = v4[2];
                dst[j + 3] = v4[3];
                j = j + 4;
                sawDigit = false;
                break;
            }
            return null;
        }
        if (sawDigit) {
            if (j + 2 > INADDRSZ) {
                return null;
            }
            dst[j] = (byte) ((val >> 8) & 0xff);
            dst[j + 1] = (byte) (val & 0xff);
            j = j + 2;
        }
        if (colonp != -1) {
            // Se corre a la derecha lo que habia despues del "::" y se rellena de ceros el hueco.
            if (j == INADDRSZ) {
                return null;
            }
            int n = j - colonp;
            int k = 1;
            while (k <= n) {
                dst[INADDRSZ - k] = dst[colonp + n - k];
                dst[colonp + n - k] = 0;
                k = k + 1;
            }
            j = INADDRSZ;
        }
        if (j != INADDRSZ) {
            return null;
        }
        return dst;
    }

    // El constructor que toma la placa. El scope numerico sale de su indice, que es lo que va a la
    // forma textual y a la serializada.
    Inet6Address(String hostName, byte[] addr, NetworkInterface nif) {
        super(hostName, addr);
        if (nif == null) {
            this.scopeId = 0;
            this.scopeIdSet = false;
        } else {
            this.scopeId = nif.getIndex();
            this.scopeIdSet = true;
        }
        this.scopedInterface = nif;
    }

    /**
     * La direccion de esos bytes, con el scope de la placa {@code nif}.
     *
     * <p>El scope numerico que queda es el indice de la placa. Con {@code nif} null la direccion
     * queda **sin scope**, que no es lo mismo que con el scope cero.
     *
     * @throws UnknownHostException si {@code addr} no mide dieciseis bytes
     */
    public static Inet6Address getByAddress(String host, byte[] addr, NetworkInterface nif)
            throws UnknownHostException {
        if (host != null && host.length() > 0 && host.charAt(0) == '[') {
            if (host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
            }
        }
        if (addr == null || addr.length != INADDRSZ) {
            throw new UnknownHostException("addr is of illegal length");
        }
        return new Inet6Address(host, copy(addr), nif);
    }

    /**
     * La placa con la que se creo esta direccion, o null.
     *
     * <p>Null tambien cuando el scope se dio como numero: de un indice no se saca la placa, y
     * devolver la que hoy tenga ese indice seria inventar. Es lo que hace el JDK.
     */
    public NetworkInterface getScopedInterface() {
        return this.scopedInterface;
    }
}
