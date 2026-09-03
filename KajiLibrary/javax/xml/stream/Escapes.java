package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

/**
 * El escapado de texto, en un solo lugar.
 *
 * <p>Lo usan el escritor de cursor y el {@code writeAsEncodedUnicode} de cada evento, que tienen
 * que producir exactamente lo mismo.
 *
 * <p>Las reglas no son las mismas adentro y afuera de un atributo, y por eso hay dos metodos. En
 * contenido hay que escapar {@code &} y {@code <} --y {@code >} solo por la secuencia {@code ]]>},
 * aunque se escapa siempre, que es lo que hace todo el mundo y es mas simple que detectarla--. En
 * un valor de atributo hay que escapar ademas las comillas con que se lo delimita, y los tabuladores
 * y saltos de linea: sin eso la normalizacion de valores de atributo los convertiria en espacios al
 * volver a leer, o sea que el documento no diria lo mismo.
 */
final class Escapes {

    private Escapes() {
    }

    /** Escapa texto de contenido. */
    static void content(Writer w, String s) throws IOException {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == '&') {
                w.write("&amp;");
            } else if (c == '<') {
                w.write("&lt;");
            } else if (c == '>') {
                w.write("&gt;");
            } else {
                w.write(c);
            }
        }
    }

    /** Escapa texto de contenido, desde un arreglo. */
    static void content(Writer w, char[] b, int from, int len) throws IOException {
        int end = from + len;
        for (int i = from; i < end; i++) {
            char c = b[i];
            if (c == '&') {
                w.write("&amp;");
            } else if (c == '<') {
                w.write("&lt;");
            } else if (c == '>') {
                w.write("&gt;");
            } else {
                w.write(c);
            }
        }
    }

    /** Escapa un valor de atributo, que va entre comillas dobles. */
    static void attribute(Writer w, String s) throws IOException {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == '&') {
                w.write("&amp;");
            } else if (c == '<') {
                w.write("&lt;");
            } else if (c == '>') {
                w.write("&gt;");
            } else if (c == '"') {
                w.write("&quot;");
            } else if (c == '\t') {
                w.write("&#9;");
            } else if (c == '\n') {
                w.write("&#10;");
            } else if (c == '\r') {
                w.write("&#13;");
            } else {
                w.write(c);
            }
        }
    }
}
