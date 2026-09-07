import java.awt.Color;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.plaf.metal.OceanTheme;

/**
 * Los dos temas de Metal, contra el JDK.
 *
 * <p>Se comparan los ocho colores crudos, los cuarenta y pico derivados, las seis tipografias y las
 * entradas que Ocean agrega a la tabla.
 *
 * <p>Las quince entradas de icono de imagen no se comparan: son GIF que el JDK carga del jar y aca
 * no hay de donde sacarlos. Los cinco de la barra de titulo si, porque esos se dibujan; de ellos se
 * compara el tamano, no los pixeles.
 */
public class Metal1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Las claves cuyo valor es una imagen del jar. */
    static boolean esImagen(String k) {
        return k.startsWith("FileChooser.") || k.startsWith("FileView.")
                || k.startsWith("OptionPane.") || k.equals("InternalFrame.icon")
                || k.equals("Tree.closedIcon") || k.equals("Tree.openIcon")
                || k.equals("Tree.leafIcon") || k.equals("Tree.collapsedIcon")
                || k.equals("Tree.expandedIcon");
    }

    static String c(ColorUIResource x) {
        return (x == null) ? "-" : x.getRed() + "," + x.getGreen() + "," + x.getBlue();
    }

    static String f(FontUIResource x) {
        return (x == null) ? "-" : x.getName() + "/" + x.getStyle() + "/" + x.getSize();
    }

    /** Un valor de la tabla, en una forma que no depende de la clase que lo envuelve. */
    static String v(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof Color) {
            Color x = (Color) o;
            return x.getRed() + "," + x.getGreen() + "," + x.getBlue();
        }
        if (o instanceof Insets) {
            Insets i = (Insets) o;
            return i.top + "," + i.left + "," + i.bottom + "," + i.right;
        }
        if (o instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            for (Object x : (List<?>) o) {
                if (sb.length() > 1) {
                    sb.append(" ");
                }
                sb.append(v(x));
            }
            return sb.append("]").toString();
        }
        if (o instanceof LineBorder) {
            LineBorder b = (LineBorder) o;
            return "linea " + b.getThickness() + " " + v(b.getLineColor());
        }
        if (o instanceof Icon) {
            Icon i = (Icon) o;
            return "icono " + i.getIconWidth() + "x" + i.getIconHeight();
        }
        return String.valueOf(o);
    }

    /** Los ocho crudos; el tema los tiene protegidos y hay que heredar para verlos. */
    static class Acero extends DefaultMetalTheme {
        String crudos() {
            return "p=" + c(getPrimary1()) + " / " + c(getPrimary2()) + " / " + c(getPrimary3())
                    + "  s=" + c(getSecondary1()) + " / " + c(getSecondary2())
                    + " / " + c(getSecondary3())
                    + "  negro=" + c(getBlack()) + " blanco=" + c(getWhite());
        }

        /** Que lo derivado sale de verdad de los ocho, y no de constantes sueltas. */
        String cadena() {
            return "control==secundario3=" + (getControl() == getSecondary3())
                    + " sombra==secundario2=" + (getControlShadow() == getSecondary2())
                    + " sombra oscura==secundario1=" + (getControlDarkShadow() == getSecondary1())
                    + " primario==primario3=" + (getPrimaryControl() == getPrimary3())
                    + " brillo==blanco=" + (getControlHighlight() == getWhite())
                    + " info==negro=" + (getControlInfo() == getBlack());
        }
    }

    static class Oceano extends OceanTheme {
        String crudos() {
            return "p=" + c(getPrimary1()) + " / " + c(getPrimary2()) + " / " + c(getPrimary3())
                    + "  s=" + c(getSecondary1()) + " / " + c(getSecondary2())
                    + " / " + c(getSecondary3())
                    + "  negro=" + c(getBlack()) + " blanco=" + c(getWhite());
        }

        String cadena() {
            return "control==secundario3=" + (getControl() == getSecondary3())
                    + " sombra==secundario2=" + (getControlShadow() == getSecondary2())
                    + " escritorio==primario2=" + (getDesktopColor() == getPrimary2())
                    + " info==negro=" + (getControlInfo() == getBlack());
        }
    }

    static void tema(String rot, MetalTheme t) {
        linea("--- " + rot + " ---");
        linea("nombre=" + t.getName());
        linea("fuentes: control=" + f(t.getControlTextFont())
                + " sistema=" + f(t.getSystemTextFont())
                + " usuario=" + f(t.getUserTextFont())
                + " menu=" + f(t.getMenuTextFont())
                + " titulo=" + f(t.getWindowTitleFont())
                + " chica=" + f(t.getSubTextFont()));
        linea("control=" + c(t.getControl())
                + " sombra=" + c(t.getControlShadow())
                + " sombra oscura=" + c(t.getControlDarkShadow())
                + " brillo=" + c(t.getControlHighlight())
                + " info=" + c(t.getControlInfo())
                + " apagado=" + c(t.getControlDisabled()));
        linea("primario=" + c(t.getPrimaryControl())
                + " sombra=" + c(t.getPrimaryControlShadow())
                + " sombra oscura=" + c(t.getPrimaryControlDarkShadow())
                + " brillo=" + c(t.getPrimaryControlHighlight())
                + " info=" + c(t.getPrimaryControlInfo()));
        linea("texto: sistema=" + c(t.getSystemTextColor())
                + " control=" + c(t.getControlTextColor())
                + " usuario=" + c(t.getUserTextColor())
                + " sistema inactivo=" + c(t.getInactiveSystemTextColor())
                + " control inactivo=" + c(t.getInactiveControlTextColor())
                + " resaltado=" + c(t.getHighlightedTextColor()));
        linea("fondo: ventana=" + c(t.getWindowBackground())
                + " escritorio=" + c(t.getDesktopColor())
                + " foco=" + c(t.getFocusColor())
                + " seleccion=" + c(t.getTextHighlightColor()));
        linea("menu: fondo=" + c(t.getMenuBackground())
                + " frente=" + c(t.getMenuForeground())
                + " elegido=" + c(t.getMenuSelectedBackground())
                + " / " + c(t.getMenuSelectedForeground())
                + " apagado=" + c(t.getMenuDisabledForeground())
                + " acelerador=" + c(t.getAcceleratorForeground())
                + " / " + c(t.getAcceleratorSelectedForeground()));
        linea("separador: fondo=" + c(t.getSeparatorBackground())
                + " frente=" + c(t.getSeparatorForeground()));
        linea("titulo: activo=" + c(t.getWindowTitleBackground())
                + " / " + c(t.getWindowTitleForeground())
                + " inactivo=" + c(t.getWindowTitleInactiveBackground())
                + " / " + c(t.getWindowTitleInactiveForeground()));
    }

    /** Que lo derivado sale de verdad de los ocho, y no de constantes sueltas. */
    static void derivacion() {
        linea("--- la derivacion ---");
        Acero d = new Acero();
        linea("Steel: " + d.crudos());
        linea(" " + d.cadena());
        Oceano o = new Oceano();
        linea("Ocean: " + o.crudos());
        linea(" " + o.cadena());
    }

    static void tabla() {
        linea("--- lo que Ocean agrega a la tabla ---");
        UIDefaults t = new UIDefaults();
        new OceanTheme().addCustomEntriesToTable(t);
        List<String> claves = new ArrayList<String>();
        for (Object k : t.keySet()) {
            String s = String.valueOf(k);
            if (!esImagen(s)) {
                claves.add(s);
            }
        }
        Collections.sort(claves);
        linea("entradas que no son imagen: " + claves.size());
        for (String k : claves) {
            linea(" " + k + " = " + v(t.get(k)));
        }

        // Un tema que no agrega nada tiene que dejar la tabla como estaba.
        UIDefaults u = new UIDefaults();
        new DefaultMetalTheme().addCustomEntriesToTable(u);
        linea("Steel agrega " + u.size() + " entradas");
    }

    public static int run() {
        tema("DefaultMetalTheme", new DefaultMetalTheme());
        tema("OceanTheme", new OceanTheme());
        derivacion();
        tabla();
        return 0;
    }
}
