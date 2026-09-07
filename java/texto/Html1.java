import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML$Attribute;
import javax.swing.text.html.HTML$Tag;
import javax.swing.text.html.HTML$UnknownTag;

/**
 * Las constantes de HTML contra el JDK: los nombres, el orden y las banderas de cada etiqueta.
 *
 * <p>Las banderas no se deducen de nada: hay que saber que <code>table</code> arma bloque y no
 * corta la linea, y que <code>br</code> corta sin armar bloque. Por eso se comparan una por una.
 */
public class Html1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    public static int run() {
        linea("nulo=" + HTML.NULL_ATTRIBUTE_VALUE);

        HTML$Tag[] ts = HTML.getAllTags();
        linea("etiquetas=" + ts.length);
        for (int i = 0; i < ts.length; i++) {
            linea("  " + i + " " + ts[i] + " bloque=" + ts[i].isBlock()
                    + " corta=" + ts[i].breaksFlow() + " literal=" + ts[i].isPreformatted());
        }

        HTML$Attribute[] as = HTML.getAllAttributeKeys();
        linea("atributos=" + as.length);
        String juntos = "";
        for (int i = 0; i < as.length; i++) {
            juntos = juntos + as[i] + " ";
        }
        linea("  " + juntos.trim());

        // Las tres que no son etiquetas de verdad no aparecen en la lista.
        linea("implicito=" + HTML$Tag.IMPLIED + " bloque=" + HTML$Tag.IMPLIED.isBlock());
        linea("contenido=" + HTML$Tag.CONTENT);
        linea("comentario=" + HTML$Tag.COMMENT);

        String[] nombres = {"p", "P", "br", "table", "nobr", "content", "comment", "p-implied",
            "zzz", ""};
        for (int i = 0; i < nombres.length; i++) {
            linea("busca [" + nombres[i] + "] etiqueta=" + HTML.getTag(nombres[i])
                    + " atributo=" + HTML.getAttributeKey(nombres[i]));
        }

        // La copia: cambiar lo que devuelve getAllTags no puede tocar la lista de adentro.
        HTML$Tag[] copia = HTML.getAllTags();
        copia[0] = null;
        linea("copia intacta=" + (HTML.getAllTags()[0] == HTML$Tag.A));

        // El entero de un atributo, con y sin valor util.
        SimpleAttributeSet at = new SimpleAttributeSet();
        at.addAttribute(HTML$Attribute.WIDTH, "120");
        at.addAttribute(HTML$Attribute.HEIGHT, "tres");
        linea("ancho=" + HTML.getIntegerAttributeValue(at, HTML$Attribute.WIDTH, -1));
        linea("alto=" + HTML.getIntegerAttributeValue(at, HTML$Attribute.HEIGHT, -1));
        linea("borde=" + HTML.getIntegerAttributeValue(at, HTML$Attribute.BORDER, 7));

        // Las desconocidas se comparan por nombre, no por identidad.
        HTML$UnknownTag u1 = new HTML$UnknownTag("blink");
        HTML$UnknownTag u2 = new HTML$UnknownTag("blink");
        HTML$UnknownTag u3 = new HTML$UnknownTag("marquee");
        linea("desconocida=" + u1 + " iguales=" + u1.equals(u2) + " distintas=" + u1.equals(u3)
                + " hash=" + (u1.hashCode() == u2.hashCode())
                + " contra etiqueta=" + u1.equals(HTML$Tag.A));

        HTML$Tag vacia = new HTML$Tag();
        linea("vacia=" + vacia + " bloque=" + vacia.isBlock() + " corta=" + vacia.breaksFlow());
        return 0;
    }
}
