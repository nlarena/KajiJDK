import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * El deslizador, contra el JDK.
 *
 * <p>Los rectangulos si se comparan crudos: no salen del texto sino del tamano del pulgar y de los
 * margenes, que son numeros escritos. Los de un deslizador sin tamano dan negativos, y eso tambien
 * se compara -- ver la nota de la clase del UI --.
 */
public class Plaf10 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String col(Color c) {
        return (c == null) ? "-" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    static String fue(Font f) {
        return (f == null) ? "-" : f.getFamily() + "/" + f.getStyle() + "/" + f.getSize();
    }

    static String r(Rectangle x) {
        return (x == null) ? "-" : x.x + "," + x.y + "," + x.width + "," + x.height;
    }

    static class Desliza extends BasicSliderUI {
        Desliza(JSlider s) {
            super(s);
        }

        String rectangulos() {
            return "foco=" + r(focusRect) + " contenido=" + r(contentRect)
                    + " pista=" + r(trackRect) + " marcas=" + r(tickRect)
                    + " etiquetas=" + r(labelRect) + " pulgar=" + r(thumbRect);
        }

        String estado() {
            return "buffer=" + trackBuffer + " insets=" + insetCache
                    + " izq a der=" + leftToRightCache + " arrastrando=" + isDragging();
        }

        String escuchas() {
            return "cambio=" + (changeListener != null) + " pista=" + (trackListener != null)
                    + " propiedad=" + (propertyChangeListener != null)
                    + " componente=" + (componentListener != null)
                    + " foco=" + (focusListener != null)
                    + " desplazamiento=" + (scrollListener != null)
                    + " reloj=" + (scrollTimer != null);
        }

        String colores() {
            return "foco=" + col(getFocusColor()) + " brillo=" + col(getHighlightColor())
                    + " sombra=" + col(getShadowColor());
        }

        String referencias() {
            return "prefH=" + getPreferredHorizontalSize() + " prefV=" + getPreferredVerticalSize()
                    + " minH=" + getMinimumHorizontalSize() + " minV=" + getMinimumVerticalSize();
        }

        int tick() {
            return getTickLength();
        }

        boolean invertido() {
            return drawInverted();
        }

        String extremos() {
            // Los dos primeros van por `String.valueOf((Object) ...)`: concatenar un `Integer`
            // nulo directo revienta en esta VM. Ver el hallazgo #521.
            return "bajo=" + String.valueOf((Object) getLowestValue())
                    + " alto=" + String.valueOf((Object) getHighestValue())
                    + " alto de etiquetas=" + getHeightOfTallestLabel()
                    + " ancho bajo=" + getWidthOfLowValueLabel()
                    + " ancho alto=" + getWidthOfHighValueLabel();
        }

        Rectangle pulgar() {
            return new Rectangle(thumbRect);
        }

        String posiciones() {
            return "x de 0=" + xPositionForValue(0) + " de 50=" + xPositionForValue(50)
                    + " de 100=" + xPositionForValue(100);
        }
    }

    static void deslizadores() {
        linea("--- BasicSliderUI ---");
        linea("constantes=" + BasicSliderUI.POSITIVE_SCROLL + "/"
                + BasicSliderUI.NEGATIVE_SCROLL + "/" + BasicSliderUI.MIN_SCROLL + "/"
                + BasicSliderUI.MAX_SCROLL);
        JSlider s = new JSlider(0, 100, 50);
        Desliza u = new Desliza(s);
        linea("comparte instancia="
                + (BasicSliderUI.createUI(s) == BasicSliderUI.createUI(s)));
        u.installUI(s);
        linea("rectangulos: " + u.rectangulos());
        linea("estado: " + u.estado());
        linea("escuchas: " + u.escuchas());
        linea("colores: " + u.colores());
        linea("referencias: " + u.referencias());
        linea("tick=" + u.tick() + " invertido=" + u.invertido());
        linea("extremos: " + u.extremos());
        linea("posiciones: " + u.posiciones());
        linea("preferido=" + u.getPreferredSize(s) + " minimo=" + u.getMinimumSize(s)
                + " maximo=" + u.getMaximumSize(s));
        linea("fondo=" + col(s.getBackground()) + " frente=" + col(s.getForeground())
                + " fuente=" + fue(s.getFont()) + " opaco=" + s.isOpaque()
                + " borde=" + s.getBorder() + " orientacion=" + s.getOrientation()
                + " invertido=" + s.getInverted());

        // Mover el valor mueve el pulgar.
        Rectangle antes = u.pulgar();
        s.setValue(0);
        linea("con valor 0 el pulgar se movio=" + !antes.equals(u.pulgar())
                + " y esta en " + r(u.pulgar()));
        s.setValue(50);

        // Poner el pulgar a mano.
        u.setThumbLocation(3, 4);
        linea("puesto a mano=" + r(u.pulgar()));

        // Avanzar de a bloques y de a unidades.
        s.setValue(50);
        u.scrollByBlock(BasicSliderUI.POSITIVE_SCROLL);
        int trasBloque = s.getValue();
        s.setValue(50);
        u.scrollByUnit(BasicSliderUI.POSITIVE_SCROLL);
        int trasUnidad = s.getValue();
        linea("tras un bloque=" + trasBloque + " tras una unidad=" + trasUnidad);
        s.setValue(50);
        u.scrollByBlock(BasicSliderUI.NEGATIVE_SCROLL);
        linea("tras un bloque para atras=" + s.getValue());

        JSlider v = new JSlider(JSlider.VERTICAL, 0, 10, 5);
        Desliza uv = new Desliza(v);
        uv.installUI(v);
        linea("vertical: rectangulos " + uv.rectangulos());
        linea("vertical: preferido=" + uv.getPreferredSize(v)
                + " minimo=" + uv.getMinimumSize(v) + " maximo=" + uv.getMaximumSize(v));

        // Invertido: las posiciones se dan vuelta.
        JSlider inv = new JSlider(0, 100, 50);
        inv.setInverted(true);
        Desliza ui = new Desliza(inv);
        ui.installUI(inv);
        linea("invertido: dibuja al reves=" + ui.invertido()
                + " rectangulos " + ui.rectangulos());
    }

    public static int run() {
        deslizadores();
        return 0;
    }
}
