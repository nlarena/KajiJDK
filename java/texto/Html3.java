import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.CSS$Attribute;
import javax.swing.text.html.StyleSheet;

/**
 * La hoja de estilos contra el JDK: colores, tamanos de letra y lectura de reglas.
 *
 * <p>Son las partes que se pueden comparar sin pantalla. El dibujo de cajas y vinetas no entra:
 * depende de la tipografia, que en esta biblioteca es una sola cara de mapa de bits.
 */
public class Html3 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String color(java.awt.Color c) {
        return (c == null) ? "nulo" : (c.getRed() + "," + c.getGreen() + "," + c.getBlue());
    }

    /** Los atributos, ordenados por nombre para que el orden de la tabla no cuente. */
    static void volcar(String titulo, AttributeSet a) {
        java.util.Vector<String> v = new java.util.Vector<String>();
        java.util.Enumeration<?> e = a.getAttributeNames();
        while (e.hasMoreElements()) {
            Object k = e.nextElement();
            v.addElement(k + "=" + a.getAttribute(k));
        }
        String[] arr = new String[v.size()];
        v.copyInto(arr);
        java.util.Arrays.sort(arr);
        linea(titulo + " (" + arr.length + ")");
        for (int i = 0; i < arr.length; i++) {
            linea("  " + arr[i]);
        }
    }

    public static int run() {
        StyleSheet ss = new StyleSheet();

        float[] tam = {0f, 1f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 20f,
            24f, 25f, 36f, 48f, 100f};
        for (int i = 0; i < tam.length; i++) {
            linea("indice de " + tam[i] + " = " + StyleSheet.getIndexOfSize(tam[i]));
        }
        for (int i = 0; i <= 8; i++) {
            linea("puntos de " + i + " = " + ss.getPointSize(i));
        }
        String[] rel = {"1", "4", "7", "+1", "-1", "+3", "-3", "0", "9"};
        for (int i = 0; i < rel.length; i++) {
            linea("puntos de [" + rel[i] + "] = " + ss.getPointSize(rel[i]));
        }

        String[] c = {"black", "white", "red", "green", "blue", "yellow", "cyan", "magenta",
            "gray", "silver", "maroon", "navy", "olive", "purple", "teal", "lime", "aqua",
            "fuchsia", "#ff0000", "#F00", "#00ff80", "#ff00", "#ff000000", "#gg00", "#", "#f",
            "rgb(1,2,3)", "rgb(1,2)", "rgb()", "rgb(1,2,3,4)", "rgb( 1 , 2 , 3 )",
            "RGB(1,2,3)", "rgb(300,2,3)", "rgb(-1,2,3)", "rgb(a,2,3)", "ff0000",
            "zz", "", "Black", "RED", " red", "red "};
        for (int i = 0; i < c.length; i++) {
            linea("color [" + c[i] + "] = " + color(ss.stringToColor(c[i])));
        }

        // El tamano base solo mueve los relativos.
        ss.setBaseFontSize(5);
        linea("base 5: [1]=" + ss.getPointSize("1") + " [+1]=" + ss.getPointSize("+1")
                + " [-1]=" + ss.getPointSize("-1") + " entero 4=" + ss.getPointSize(4));
        ss.setBaseFontSize("+2");
        linea("base +2: [+0]=" + ss.getPointSize("+0"));
        ss.setBaseFontSize("-9");
        linea("base -9: [+0]=" + ss.getPointSize("+0"));
        try {
            ss.setBaseFontSize("zz");
            linea("base zz: sin queja, [+0]=" + ss.getPointSize("+0"));
        } catch (NumberFormatException nfe) {
            linea("base zz: NumberFormatException");
        }

        // Las declaraciones y las reglas.
        volcar("decl simple", ss.getDeclaration("color: green; margin-top: 5"));
        volcar("decl con basura", ss.getDeclaration("color green; ; zzz: 1; font-size: 14pt"));
        volcar("decl con comentario", ss.getDeclaration("color: red /* rojo */ ; /* x */"));

        ss.addRule("p { color: blue; font-size: 14pt }");
        ss.addRule("h1, h2 { color: red }");
        ss.addRule("body p { margin-left: 10 }");
        linea("nombre de la regla p = " + ss.getRule("p").getName());
        volcar("regla p", ss.getRule("p"));
        volcar("regla h1", ss.getRule("h1"));
        volcar("regla h2", ss.getRule("h2"));
        volcar("regla body p", ss.getRule("body p"));
        volcar("regla html body p", ss.getRule("html body p"));
        volcar("regla desconocida", ss.getRule("zz"));

        // Poner una propiedad, con y sin control del valor.
        MutableAttributeSet m = new SimpleAttributeSet();
        ss.addCSSAttribute(m, CSS$Attribute.FONT_SIZE, "12pt");
        linea("desdeHTML color bueno=" + ss.addCSSAttributeFromHTML(m, CSS$Attribute.COLOR, "red"));
        linea("desdeHTML color malo=" + ss.addCSSAttributeFromHTML(m, CSS$Attribute.COLOR, "zz"));
        linea("desdeHTML vacio=" + ss.addCSSAttributeFromHTML(m, CSS$Attribute.COLOR, ""));
        volcar("atributos", m);

        linea("base=" + ss.getBase() + " hojas=" + ss.getStyleSheets());
        StyleSheet otra = new StyleSheet();
        ss.addStyleSheet(otra);
        linea("tras agregar: hojas=" + ss.getStyleSheets().length);
        ss.addStyleSheet(otra);
        linea("agregar dos veces: hojas=" + ss.getStyleSheets().length);
        ss.removeStyleSheet(otra);
        linea("tras sacar: hojas=" + ss.getStyleSheets());

        return 0;
    }
}
