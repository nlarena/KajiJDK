package java.net;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// La inversa de `URLEncoder`: '+' vuelve a ser espacio y "%XX" vuelve a ser un byte.
//
// El unico punto donde esto es mas que un bucle: los "%XX" **consecutivos hay que juntarlos** antes
// de decodificarlos. Un caracter fuera de ASCII ocupa varios bytes en UTF-8, y decodificar cada byte
// por separado da tres caracteres rotos en vez de uno bueno. Por eso el bucle interno acumula toda
// la corrida de escapes y recien despues arma el `String`.
//
// Es computacion pura: no hay nada omitido.
public final class URLDecoder {

    private URLDecoder() {
    }

    /**
     * Decodifica con UTF-8.
     *
     * @deprecated El resultado depende del juego de caracteres; usar la sobrecarga que lo pide.
     */
    @Deprecated
    public static String decode(String s) {
        return decode(s, StandardCharsets.UTF_8);
    }

    /**
     * Decodifica con el juego de caracteres de ese nombre.
     *
     * @throws UnsupportedEncodingException si el nombre no corresponde a ninguno
     */
    public static String decode(String s, String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException("charsetName");
        }
        Charset cs;
        try {
            cs = Charset.forName(enc);
        } catch (Exception e) {
            throw new UnsupportedEncodingException(enc);
        }
        return decode(s, cs);
    }

    /**
     * Decodifica con ese juego de caracteres.
     *
     * @throws IllegalArgumentException si hay un '%' sin dos digitos hexadecimales detras
     */
    public static String decode(String s, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        boolean changed = false;
        int numChars = s.length();
        StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
        byte[] bytes = null;
        int i = 0;
        while (i < numChars) {
            char c = s.charAt(i);
            if (c == '+') {
                sb.append(' ');
                i = i + 1;
                changed = true;
            } else if (c == '%') {
                if (bytes == null) {
                    bytes = new byte[(numChars - i) / 3];
                }
                int pos = 0;
                while ((i + 2) < numChars && c == '%') {
                    int hi = hexDigit(s.charAt(i + 1));
                    int lo = hexDigit(s.charAt(i + 2));
                    bytes[pos] = (byte) ((hi << 4) | lo);
                    pos = pos + 1;
                    i = i + 3;
                    if (i < numChars) {
                        c = s.charAt(i);
                    }
                }
                if (i < numChars && c == '%') {
                    throw new IllegalArgumentException(
                            "URLDecoder: Incomplete trailing escape (%) pattern");
                }
                sb.append(new String(bytes, 0, pos, charset));
                changed = true;
            } else {
                sb.append(c);
                i = i + 1;
            }
        }
        if (changed) {
            return sb.toString();
        }
        return s;
    }

    private static int hexDigit(char c) {
        int v = InetAddress.digit(c, 16);
        if (v < 0) {
            throw new IllegalArgumentException(
                    "URLDecoder: Illegal hex characters in escape (%) pattern - "
                            + "not a hexadecimal digit: " + c + " = " + ((int) c));
        }
        return v;
    }
}
