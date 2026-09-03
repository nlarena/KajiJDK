package javax.security.auth.x500;

/**
 * KajiLibrary's javax.security.auth.x500.Valor -- el valor de un `type=value`, leido y escrito.
 *
 * <p>Es la parte con mas reglas de todo el RFC 2253, y la que decide si dos implementaciones se
 * entienden. Se separo en su propia clase porque leer y escribir tienen que ser **inversas exactas**:
 * si no lo son, un nombre pasado por `getName()` y vuelto a parsear no da el mismo nombre, y eso
 * rompe cualquier cosa que guarde DN como texto.
 */
final class AttrValue {

    private AttrValue() {
    }

    // Los que hay que escapar siempre al escribir (RFC 4514 §2.4). La coma y el `+` separan; el `"` y
    // la `\` son la sintaxis misma; `<`, `>` y `;` vienen del RFC 2253 viejo; el `#` solo molesta al
    // principio, porque ahi significa "lo que sigue es hexadecimal".
    private static final String ESPECIALES = ",+\"\\<>;";

    /**
     * El valor **desescapado** que representa ese texto.
     *
     * @throws IllegalArgumentException si el escape esta mal formado
     */
    static String read(String rawBytes) {
        String s = rawBytes;
        // Los espacios de los bordes no cuentan salvo que esten escapados, y por eso se recortan
        // **antes** de desescapar: despues ya no se distingue un espacio escrito de uno escapado.
        int from = 0;
        while (from < s.length() && s.charAt(from) == ' ') {
            from = from + 1;
        }
        int to = s.length();
        while (to > from && s.charAt(to - 1) == ' ' && !isEscaped(s, to - 1)) {
            to = to - 1;
        }
        s = s.substring(from, to);
        if (s.length() == 0) {
            return "";
        }
        if (s.charAt(0) == '#') {
            return fromHex(s.substring(1, s.length()));
        }
        if (s.charAt(0) == '"') {
            return unquote(s);
        }
        return unescape(s);
    }

    // Si el caracter en `i` esta precedido por un numero **impar** de barras: dos barras son una
    // barra literal, no un escape.
    private static boolean isEscaped(String s, int i) {
        int barras = 0;
        int k = i - 1;
        while (k >= 0 && s.charAt(k) == '\\') {
            barras = barras + 1;
            k = k - 1;
        }
        return barras % 2 == 1;
    }

    private static String unquote(String s) {
        if (s.length() < 2 || s.charAt(s.length() - 1) != '"') {
            throw new IllegalArgumentException("faltan las comillas de cierre: " + s);
        }
        return unescape(s.substring(1, s.length() - 1));
    }

    private static String unescape(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != '\\') {
                out.append(c);
                i = i + 1;
                continue;
            }
            if (i + 1 >= s.length()) {
                throw new IllegalArgumentException("el valor termina en una barra: " + s);
            }
            char sig = s.charAt(i + 1);
            // `\XX` con dos digitos hexadecimales es un **byte**, no un caracter escapado. La
            // diferencia importa: `\41` es una `A` y no un `4` seguido de un `1`.
            if (isHex(sig) && i + 2 < s.length() && isHex(s.charAt(i + 2))) {
                out.append((char) ((hexValue(sig) << 4) | hexValue(s.charAt(i + 2))));
                i = i + 3;
            } else {
                out.append(sig);
                i = i + 2;
            }
        }
        return out.toString();
    }

    private static String fromHex(String hex) {
        if (hex.length() == 0 || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("hexadecimal de largo impar: #" + hex);
        }
        byte[] bytes = new byte[hex.length() / 2];
        int i = 0;
        while (i < bytes.length) {
            char a = hex.charAt(2 * i);
            char b = hex.charAt(2 * i + 1);
            if (!isHex(a) || !isHex(b)) {
                throw new IllegalArgumentException("no es hexadecimal: #" + hex);
            }
            bytes[i] = (byte) ((hexValue(a) << 4) | hexValue(b));
            i = i + 1;
        }
        // Los bytes son el DER del valor. Se lee lo que se pueda leer como texto; si no, se guarda la
        // forma hexadecimal tal cual, que es lo que el JDK muestra para un tipo desconocido.
        try {
            return Der.readAttributeValue(bytes);
        } catch (java.io.IOException e) {
            return "#" + hex;
        }
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        return c - 'A' + 10;
    }

    /**
     * El texto **escapado** de ese valor, listo para ir en un DN.
     *
     * <p>Inversa exacta de {@link #read}: lo que sale de aca, parseado, vuelve a dar el mismo valor.
     */
    static String write(String value) {
        if (value.length() == 0) {
            return "";
        }
        // Un valor que ya viene en forma hexadecimal --porque su tipo no se pudo leer como texto-- se
        // pasa tal cual: escaparlo lo convertiria en el texto `#30...` en vez del valor.
        if (value.charAt(0) == '#' && looksHex(value)) {
            return value;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            boolean borde = i == 0 || i == value.length() - 1;
            if (ESPECIALES.indexOf(c) >= 0) {
                out.append('\\').append(c);
            } else if (c == ' ' && borde) {
                // Solo los espacios de los bordes: adentro no hace falta y ensuciaria el nombre.
                out.append("\\ ");
            } else if (c == '#' && i == 0) {
                out.append("\\#");
            } else if (c < 0x20) {
                out.append('\\').append(hexDigit(c >> 4)).append(hexDigit(c & 0xf));
            } else {
                out.append(c);
            }
            i = i + 1;
        }
        return out.toString();
    }

    private static boolean looksHex(String s) {
        if (s.length() < 3 || (s.length() - 1) % 2 != 0) {
            return false;
        }
        int i = 1;
        while (i < s.length()) {
            if (!isHex(s.charAt(i))) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static char hexDigit(int n) {
        return (char) (n < 10 ? '0' + n : 'A' + n - 10);
    }

    /**
     * El valor en forma **canonica**: minusculas, sin espacios en los bordes y con los internos
     * colapsados a uno.
     *
     * <p>Es la unica transformacion del formato canonico, y es la que hace que dos nombres escritos
     * distinto den la misma cadena. Colapsar los espacios internos --no solo recortar los bordes-- es
     * la parte que se olvida: `CN=Juan  Perez` y `CN=Juan Perez` son el mismo nombre.
     */
    static String canonical(String value) {
        StringBuilder out = new StringBuilder();
        boolean spacing = false;
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                spacing = true;
            } else {
                if (spacing && out.length() > 0) {
                    out.append(' ');
                }
                spacing = false;
                out.append(Character.toLowerCase(c));
            }
            i = i + 1;
        }
        return write(out.toString());
    }
}
