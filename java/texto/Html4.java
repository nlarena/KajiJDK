import java.io.StringReader;

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML$Tag;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit$ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * El analizador de HTML contra el JDK, de punta a punta.
 *
 * <p>Se le da el mismo HTML a las dos bibliotecas y se anota todo lo que el analizador avisa: cada
 * etiqueta que abre, cada una que cierra, cada texto y cada error. Es lo unico que mide de verdad
 * si las reglas de la DTD se estan aplicando igual, porque es donde se ve que etiquetas se
 * inventaron y cuales se cerraron solas.
 */
public class Html4 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Anota cada aviso del analizador, en orden. */
    static class Espia extends HTMLEditorKit$ParserCallback {

        private int nivel = 0;

        private String sangria() {
            String s = "";
            for (int i = 0; i < nivel; i++) {
                s = s + "  ";
            }
            return s;
        }

        /** Los atributos, ordenados por nombre para no depender del orden de la tabla. */
        private String atributos(AttributeSet a) {
            if (a == null || a.getAttributeCount() == 0) {
                return "";
            }
            java.util.Vector<String> v = new java.util.Vector<String>();
            java.util.Enumeration<?> e = a.getAttributeNames();
            while (e.hasMoreElements()) {
                Object k = e.nextElement();
                v.addElement(k + "=" + a.getAttribute(k));
            }
            String[] arr = new String[v.size()];
            v.copyInto(arr);
            java.util.Arrays.sort(arr);
            String s = " {";
            for (int i = 0; i < arr.length; i++) {
                s = s + arr[i] + " ";
            }
            return s + "}";
        }

        public void handleStartTag(HTML$Tag t, MutableAttributeSet a, int pos) {
            linea(sangria() + "abre " + t + atributos(a));
            nivel++;
        }

        public void handleEndTag(HTML$Tag t, int pos) {
            nivel--;
            if (nivel < 0) {
                nivel = 0;
            }
            linea(sangria() + "cierra " + t);
        }

        public void handleSimpleTag(HTML$Tag t, MutableAttributeSet a, int pos) {
            linea(sangria() + "sola " + t + atributos(a));
        }

        /**
         * El texto, con los codigos de los caracteres que no son ASCII.
         *
         * <p>Escribir el caracter y nada mas dependeria de la codificacion de la salida, que no es
         * la misma en las dos maquinas virtuales. Un {@code &nbsp;} se veria distinto sin que
         * nada este mal.
         */
        public void handleText(char[] data, int pos) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                char c = data[i];
                if (c >= 32 && c < 127) {
                    sb.append(c);
                } else {
                    sb.append("<").append((int) c).append(">");
                }
            }
            linea(sangria() + "texto [" + sb + "]");
        }

        public void handleComment(char[] data, int pos) {
            linea(sangria() + "comentario [" + new String(data) + "]");
        }

        int errores = 0;

        public void handleError(String msg, int pos) {
            errores++;
        }
    }

    /**
     * Analiza y anota el arbol; los errores van juntos al final.
     *
     * <p>Se cuentan en lugar de anotarse uno por uno a proposito. La recuperacion de errores del
     * JDK tiene sus propias reglas -- cuando avisar, con que mensaje, en que momento -- que no son
     * parte del arbol que se arma. Mezcladas con el arbol, una diferencia de una sola movia todas
     * las lineas siguientes y tapaba lo que importa comparar.
     */
    static void analizar(String titulo, String html) {
        linea("=== " + titulo);
        Espia e = new Espia();
        try {
            new ParserDelegator().parse(new StringReader(html), e, true);
        } catch (Exception ex) {
            linea("EXCEPCION " + ex.getClass().getName());
        }
        linea("errores=" + e.errores);
    }

    public static int run() {
        analizar("parrafos sin cerrar", "<html><body><p>uno<p>dos</body></html>");
        analizar("texto suelto", "hola mundo");
        analizar("atributos", "<a href=\"x.html\" name=uno>ir</a>");
        analizar("sin valor", "<ul compact><li>a<li>b</ul>");
        analizar("vacias", "un<br>dos<hr>tres<img src=\"a.png\" alt=\"A\">");
        analizar("comentario", "<p>a<!-- nota -->b");
        analizar("entidades", "<p>&amp; &lt; &gt; &#65; &#x42; &nbsp; &zz;");
        analizar("mayusculas", "<P><B>x</B></P>");
        analizar("cierre de mas", "<p>a</b></p>");
        analizar("desconocida", "<blink>x</blink>");
        analizar("anidado", "<div><p>a</p><ul><li>x</li></ul></div>");
        analizar("comillas simples", "<a href='y.html'>z</a>");
        analizar("angulo suelto", "1 < 2 y 3 > 2");
        analizar("vacio", "");
        return 0;
    }
}
