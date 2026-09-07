import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * El panel dividido y su divisor, contra el JDK.
 *
 * <p>Los tamanos si se comparan crudos: salen de dos {@code JLabel} con texto corto, y aunque la
 * tipografia difiera, lo que se compara son las relaciones -- que el minimo del divisor sea el
 * preferido, que el maximo del divisor nunca este por debajo del minimo -- y los numeros que no
 * dependen del texto.
 */
public class Plaf7 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String col(Color c) {
        return (c == null) ? "-" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    static String corto(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        return c.substring(c.lastIndexOf('.') + 1);
    }

    @SuppressWarnings("deprecation")
    static class Panel extends BasicSplitPaneUI {
        String teclas() {
            return "" + upKey + "/" + downKey + "/" + leftKey + "/" + rightKey + "/"
                    + homeKey + "/" + endKey + "/" + dividerResizeToggleKey;
        }

        String acciones() {
            return "" + (keyboardUpLeftListener != null) + "/"
                    + (keyboardDownRightListener != null) + "/"
                    + (keyboardHomeListener != null) + "/" + (keyboardEndListener != null) + "/"
                    + (keyboardResizeToggleListener != null);
        }

        String estado() {
            return "divisor=" + (divider != null) + " tamano=" + dividerSize
                    + " arrastreHW=" + draggingHW
                    + " inicio de arrastre=" + beginDragDividerLocation
                    + " acomodador=" + (layoutManager != null)
                    + " foco=" + (focusListener != null)
                    + " propiedad=" + (propertyChangeListener != null)
                    + " sombra=" + (nonContinuousLayoutDivider != null);
        }

        static String constantes() {
            return NON_CONTINUOUS_DIVIDER + " / " + KEYBOARD_DIVIDER_MOVE_OFFSET;
        }

        int bordeDelDivisor() {
            return getDividerBorderSize();
        }
    }

    static class Divisor extends BasicSplitPaneDivider {
        Divisor(BasicSplitPaneUI ui) {
            super(ui);
        }

        static String constantes() {
            return ONE_TOUCH_SIZE + " / " + ONE_TOUCH_OFFSET;
        }

        void sobre(boolean b) {
            setMouseOver(b);
        }

        String estado() {
            return "orientacion=" + orientation + " tamano=" + dividerSize
                    + " arrastre=" + (dragger != null) + " mouse=" + (mouseHandler != null)
                    + " panel=" + (splitPane != null) + " ui=" + (splitPaneUI != null)
                    + " escondido=" + (hiddenDivider != null)
                    + " botones=" + (leftButton != null) + "/" + (rightButton != null);
        }
    }

    static void bordes() {
        linea("--- los bordes que faltaban de BasicBorders ---");
        Border[] b = {
            BasicBorders.getSplitPaneBorder(),
            BasicBorders.getSplitPaneDividerBorder(),
            BasicBorders.getProgressBarBorder(),
            BasicBorders.getInternalFrameBorder(),
        };
        String[] n = {"panel dividido", "divisor", "barra de progreso", "ventana interna"};
        Component c = new javax.swing.JPanel();
        for (int i = 0; i < b.length; i++) {
            linea(n[i] + ": " + corto(b[i]) + " insets=" + b[i].getBorderInsets(c)
                    + " opaco=" + b[i].isBorderOpaque());
        }
        linea("comparte instancia="
                + (BasicBorders.getSplitPaneBorder() == BasicBorders.getSplitPaneBorder()));
        BasicBorders.SplitPaneBorder propio =
                new BasicBorders.SplitPaneBorder(Color.red, Color.blue);
        linea("uno propio: insets=" + propio.getBorderInsets(c)
                + " opaco=" + propio.isBorderOpaque()
                + " es del aspecto=" + (propio instanceof javax.swing.plaf.UIResource));
    }

    static void paneles() {
        linea("--- BasicSplitPaneUI ---");
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JLabel("izq"), new JLabel("der"));
        Panel u = new Panel();
        linea("comparte instancia="
                + (BasicSplitPaneUI.createUI(sp) == BasicSplitPaneUI.createUI(sp)));
        u.installUI(sp);
        linea("teclas: " + u.teclas());
        linea("acciones: " + u.acciones());
        linea("estado: " + u.estado());
        linea("constantes=" + Panel.constantes() + " borde del divisor=" + u.bordeDelDivisor());
        linea("fondo=" + col(sp.getBackground()) + " opaco=" + sp.isOpaque()
                + " borde=" + corto(sp.getBorder())
                + " tamano del divisor=" + sp.getDividerSize()
                + " continuo=" + sp.isContinuousLayout()
                + " un toque=" + sp.isOneTouchExpandable());
        linea("orientacion=" + u.getOrientation() + " continuo=" + u.isContinuousLayout()
                + " ultimo arrastre=" + u.getLastDragLocation());
        linea("insets=" + u.getInsets(sp));
        linea("maximo=" + u.getMaximumSize(sp));
        linea("el maximo del divisor nunca esta por debajo del minimo="
                + (u.getMaximumDividerLocation(sp) >= u.getMinimumDividerLocation(sp)));
        linea("posicion del divisor=" + u.getDividerLocation(sp));

        BasicSplitPaneDivider d = u.getDivider();
        linea("divisor: " + corto(d) + " tamano=" + d.getDividerSize()
                + " preferido=" + d.getPreferredSize()
                + " el minimo es el preferido=" + d.getMinimumSize().equals(d.getPreferredSize())
                + " insets=" + d.getInsets() + " borde=" + corto(d.getBorder())
                + " hijos=" + d.getComponentCount() + " mouse encima=" + d.isMouseOver());

        sp.setOneTouchExpandable(true);
        linea("con un toque hijos=" + d.getComponentCount());
        sp.setOneTouchExpandable(false);
        linea("sin un toque hijos=" + d.getComponentCount());

        // El divisor vertical mide al reves.
        JSplitPane vs = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JLabel("arriba"), new JLabel("abajo"));
        Panel uv = new Panel();
        uv.installUI(vs);
        linea("vertical: orientacion=" + uv.getOrientation()
                + " divisor preferido=" + uv.getDivider().getPreferredSize());
    }

    static void divisores() {
        linea("--- BasicSplitPaneDivider ---");
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JLabel("izq"), new JLabel("der"));
        Panel u = new Panel();
        u.installUI(sp);
        Divisor d = new Divisor(u);
        linea("constantes=" + Divisor.constantes());
        linea("recien hecho: " + d.estado());
        d.setDividerSize(7);
        linea("tras poner 7: tamano=" + d.getDividerSize()
                + " preferido=" + d.getPreferredSize());
        d.sobre(true);
        linea("mouse encima=" + d.isMouseOver());
        d.sobre(false);
        d.setBorder(BasicBorders.getSplitPaneDividerBorder());
        linea("con borde insets=" + d.getInsets());
        d.setBorder(null);
        linea("sin borde insets=" + d.getInsets());
        linea("su ui es el nuestro=" + (d.getBasicSplitPaneUI() == u));
    }

    public static int run() {
        bordes();
        paneles();
        divisores();
        return 0;
    }
}
