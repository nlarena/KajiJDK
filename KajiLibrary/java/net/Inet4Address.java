package java.net;

// Una direccion IPv4: cuatro bytes.
//
// Todo lo de aca es aritmetica sobre esos cuatro bytes y gramatica de literales, o sea que se puede
// escribir completo sin red. Los dos parsers publicos --`ofLiteral` y `ofPosixLiteral`-- son
// gramaticas **distintas** y la diferencia importa:
//
//   - `ofLiteral` es la forma de la plataforma Java: entre uno y cuatro campos, **siempre
//     decimales**. Los ceros a la izquierda no significan octal ("010.1.1.1" es 10.1.1.1).
//   - `ofPosixLiteral` es la de `inet_aton(3)`: los mismos campos, pero con las convenciones de C
//     para la base -- "0x" es hexadecimal y un cero a la izquierda es octal ("010.1.1.1" es
//     8.1.1.1).
//
// Que existan las dos no es redundancia: la de POSIX es la que usan `ping`, `curl` y el resto del
// sistema, y leer un literal con la gramatica equivocada cambia la direccion en silencio. Por eso
// tener las dos con nombres distintos es mas seguro que tener una "que adivine".
//
// La forma corta (menos de cuatro campos) tampoco es un capricho: el ultimo campo absorbe todos los
// bytes que faltan, asi que "127.1" es 127.0.0.1 y "2130706433" tambien.
//
// No hay nada omitido en esta clase.
public final class Inet4Address extends InetAddress {

    private static final long serialVersionUID = 3286316764910316507L;

    static final int INADDRSZ = 4;

    Inet4Address(String hostName, byte[] addr) {
        super(hostName, addr);
    }

    // La comodin, 0.0.0.0. Lleva "0.0.0.0" como nombre y no null, igual que en el JDK: es la unica
    // direccion que se imprime "0.0.0.0/0.0.0.0", y `InetSocketAddress(int)` depende de eso.
    Inet4Address() {
        super("0.0.0.0", new byte[] {0, 0, 0, 0});
    }

    private int b(int i) {
        return this.addr[i] & 0xff;
    }

    /** 224.0.0.0/4. */
    public boolean isMulticastAddress() {
        return (this.addr[0] & 0xf0) == 0xe0;
    }

    /** 0.0.0.0. */
    public boolean isAnyLocalAddress() {
        return this.b(0) == 0 && this.b(1) == 0 && this.b(2) == 0 && this.b(3) == 0;
    }

    /** 127.0.0.0/8. */
    public boolean isLoopbackAddress() {
        return this.b(0) == 127;
    }

    /** 169.254.0.0/16. */
    public boolean isLinkLocalAddress() {
        return this.b(0) == 169 && this.b(1) == 254;
    }

    /** 10/8, 172.16/12 y 192.168/16: los tres rangos privados del RFC 1918. */
    public boolean isSiteLocalAddress() {
        return this.b(0) == 10
                || (this.b(0) == 172 && this.b(1) >= 16 && this.b(1) <= 31)
                || (this.b(0) == 192 && this.b(1) == 168);
    }

    /** Multicast global: todo 224/4 menos el bloque reservado 224.0.0.0/24. */
    public boolean isMCGlobal() {
        return this.b(0) >= 224 && this.b(0) <= 238
                && !(this.b(0) == 224 && this.b(1) == 0 && this.b(2) == 0);
    }

    /** IPv4 no tiene alcance "nodo", asi que nunca. */
    public boolean isMCNodeLocal() {
        return false;
    }

    /** 224.0.0.0/24. */
    public boolean isMCLinkLocal() {
        return this.b(0) == 224 && this.b(1) == 0 && this.b(2) == 0;
    }

    /** 239.255.0.0/16. */
    public boolean isMCSiteLocal() {
        return this.b(0) == 239 && this.b(1) == 255;
    }

    /** 239.192.0.0/14. */
    public boolean isMCOrgLocal() {
        return this.b(0) == 239 && this.b(1) >= 192 && this.b(1) <= 195;
    }

    public byte[] getAddress() {
        return copy(this.addr);
    }

    public String getHostAddress() {
        return numericToTextFormat(this.addr);
    }

    // Los cuatro bytes empaquetados en el int, que es la representacion natural de una IPv4 y con la
    // que dos direcciones distintas nunca colisionan.
    public int hashCode() {
        return (this.b(0) << 24) | (this.b(1) << 16) | (this.b(2) << 8) | this.b(3);
    }

    // Una IPv4 nunca es igual a una IPv6, aunque los bytes coincidan: son direcciones de espacios
    // distintos. (La forma "IPv4-mapped" no rompe esto porque se convierte a Inet4Address al
    // construirse, no al comparar.)
    public boolean equals(Object obj) {
        if (!(obj instanceof Inet4Address)) {
            return false;
        }
        Inet4Address other = (Inet4Address) obj;
        int i = 0;
        while (i < INADDRSZ) {
            if (this.addr[i] != other.addr[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // ---- literales ------------------------------------------------------------------------------

    /**
     * La direccion que describe el literal decimal {@code s} (uno a cuatro campos).
     *
     * @throws IllegalArgumentException si no lo es
     */
    public static Inet4Address ofLiteral(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        byte[] a = textToNumericFormat(s);
        if (a == null) {
            throw invalidLiteral(s);
        }
        return new Inet4Address(null, a);
    }

    /**
     * La direccion que describe {@code s} con las reglas de {@code inet_aton(3)}: "0x" es hex y un
     * cero adelante es octal.
     *
     * @throws IllegalArgumentException si no es un literal POSIX valido
     */
    public static Inet4Address ofPosixLiteral(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        byte[] a = posixToNumericFormat(s);
        if (a == null) {
            throw invalidLiteral(s);
        }
        return new Inet4Address(null, a);
    }

    static String numericToTextFormat(byte[] src) {
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff) + "." + (src[3] & 0xff);
    }

    // La gramatica de la plataforma: campos decimales separados por puntos, entre uno y cuatro. Los
    // primeros campos valen un byte cada uno; el ultimo se reparte en todos los bytes que quedan,
    // que es de donde salen "127.1" y "2130706433".
    //
    // El tope de quince caracteres es del JDK y no es decorativo: sin el, "0000000000000000000001"
    // seria una direccion valida y ademas desbordaria el acumulador.
    static byte[] textToNumericFormat(String src) {
        int len = src.length();
        if (len == 0 || len > 15) {
            return null;
        }
        byte[] res = new byte[INADDRSZ];
        long value = 0;
        int currByte = 0;
        boolean newOctet = true;
        int i = 0;
        while (i < len) {
            char c = src.charAt(i);
            if (c == '.') {
                if (newOctet || value > 0xff || currByte == 3) {
                    return null;
                }
                res[currByte] = (byte) (value & 0xff);
                currByte = currByte + 1;
                value = 0;
                newOctet = true;
            } else {
                int d = digit(c, 10);
                if (d < 0) {
                    return null;
                }
                value = value * 10 + d;
                newOctet = false;
            }
            i = i + 1;
        }
        if (newOctet || value >= (1L << ((4 - currByte) * 8))) {
            return null;
        }
        return spread(res, currByte, value);
    }

    // Igual que la anterior, pero cada campo se lee con las bases de C. Se separa en vez de agregarle
    // un flag a la otra porque las dos gramaticas divergen en el primer caracter ('0') y mezclarlas
    // hace que un error en una se filtre a la otra.
    static byte[] posixToNumericFormat(String src) {
        int len = src.length();
        if (len == 0) {
            return null;
        }
        byte[] res = new byte[INADDRSZ];
        int currByte = 0;
        int start = 0;
        long value = 0;
        int i = 0;
        while (true) {
            if (i == len || src.charAt(i) == '.') {
                if (i == start) {
                    return null;
                }
                Long field = parsePosixField(src.substring(start, i));
                if (field == null) {
                    return null;
                }
                value = field.longValue();
                if (i == len) {
                    break;
                }
                if (currByte == 3 || value > 0xff) {
                    return null;
                }
                res[currByte] = (byte) (value & 0xff);
                currByte = currByte + 1;
                start = i + 1;
            }
            i = i + 1;
        }
        if (value < 0 || value >= (1L << ((4 - currByte) * 8))) {
            return null;
        }
        return spread(res, currByte, value);
    }

    // El ultimo campo ocupa desde `currByte` hasta el final, en orden de red.
    private static byte[] spread(byte[] res, int currByte, long value) {
        int b = 3;
        while (b >= currByte) {
            res[b] = (byte) ((value >> (8 * (3 - b))) & 0xff);
            b = b - 1;
        }
        return res;
    }

    private static Long parsePosixField(String f) {
        int radix = 10;
        int from = 0;
        if (f.length() > 1 && f.charAt(0) == '0') {
            if (f.length() > 2 && (f.charAt(1) == 'x' || f.charAt(1) == 'X')) {
                radix = 16;
                from = 2;
            } else {
                radix = 8;
                from = 1;
            }
        }
        if (from >= f.length()) {
            return null;
        }
        long v = 0;
        int i = from;
        while (i < f.length()) {
            int d = digit(f.charAt(i), radix);
            if (d < 0) {
                return null;
            }
            v = v * radix + d;
            if (v > 0xffffffffL) {
                return null;
            }
            i = i + 1;
        }
        return Long.valueOf(v);
    }
}
