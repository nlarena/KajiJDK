package java.net;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// El escape de "application/x-www-form-urlencoded": lo que hace un formulario HTML al mandarse.
//
// **No es el escape de una URL**, aunque el nombre lo sugiera, y confundirlos es el error clasico.
// Este formato viene de los formularios: el espacio se codifica como '+' y la barra '/' se
// codifica, lo que arruina cualquier ruta. Para armar una URL esta `java.net.URI`, que aplica RFC
// 3986. Este es para armar el cuerpo de un POST o una query string, y ahi es correcto.
//
// El conjunto que NO se codifica es el del JDK y es mas chico de lo que uno espera: letras, digitos,
// y solo cuatro signos -- '-', '_', '.', '*'. La tilde **si** se codifica, aunque el RFC 3986 la
// considere no reservada, porque este formato es anterior a ese RFC y cambiarlo romperia servidores.
//
// Es computacion pura: no hay nada omitido.
public final class URLEncoder {

    private URLEncoder() {
    }

    /**
     * Codifica con UTF-8.
     *
     * @deprecated El resultado depende del juego de caracteres; usar la sobrecarga que lo pide.
     */
    @Deprecated
    public static String encode(String s) {
        return encode(s, StandardCharsets.UTF_8);
    }

    /**
     * Codifica con el juego de caracteres de ese nombre.
     *
     * @throws UnsupportedEncodingException si el nombre no corresponde a ninguno
     */
    public static String encode(String s, String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException("charsetName");
        }
        Charset cs;
        try {
            cs = Charset.forName(enc);
        } catch (Exception e) {
            throw new UnsupportedEncodingException(enc);
        }
        return encode(s, cs);
    }

    /** Codifica con ese juego de caracteres. */
    public static String encode(String s, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (dontNeedEncoding(c)) {
                out.append(c);
                i = i + 1;
            } else if (c == ' ') {
                out.append('+');
                i = i + 1;
            } else {
                // Un par subrogado son dos `char` que forman un solo caracter: hay que pasarlos
                // juntos al juego de caracteres o salen dos secuencias invalidas en vez de una
                // valida.
                int end = i + 1;
                if (Character.isHighSurrogate(c) && end < s.length()
                        && Character.isLowSurrogate(s.charAt(end))) {
                    end = end + 1;
                }
                byte[] bytes = s.substring(i, end).getBytes(charset);
                int k = 0;
                while (k < bytes.length) {
                    out.append('%');
                    out.append(hex((bytes[k] >> 4) & 0xf));
                    out.append(hex(bytes[k] & 0xf));
                    k = k + 1;
                }
                i = end;
            }
        }
        return out.toString();
    }

    private static char hex(int v) {
        if (v < 10) {
            return (char) ('0' + v);
        }
        return (char) ('A' + (v - 10));
    }

    private static boolean dontNeedEncoding(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '-' || c == '_' || c == '.' || c == '*';
    }
}
