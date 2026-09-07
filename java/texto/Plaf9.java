import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicOptionPaneUI;
import javax.swing.plaf.basic.BasicToolBarUI;

/**
 * El panel de opciones y la barra de herramientas, contra el JDK.
 *
 * <p>Los tamanos no se comparan crudos: el dialogo del JDK trae un icono de 32 x 32 de la tabla del
 * aspecto y el nuestro no, asi que mide distinto. Se compara la estructura y los numeros que no
 * dependen del aspecto.
 */
public class Plaf9 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String col(Color c) {
        return (c == null) ? "-" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    static String fue(Font f) {
        return (f == null) ? "-" : f.getFamily() + "/" + f.getStyle() + "/" + f.getSize();
    }

    static String corto(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        return c.substring(c.lastIndexOf('.') + 1);
    }

    static class Opciones extends BasicOptionPaneUI {
        String estado() {
            return "panel=" + (optionPane != null) + " minimo=" + minimumSize
                    + " entrada=" + (inputComponent != null)
                    + " foco inicial=" + (initialFocusComponent != null)
                    + " propios=" + hasCustomComponents
                    + " propiedad=" + (propertyChangeListener != null);
        }

        Dimension minPanel() {
            return getMinimumOptionPaneSize();
        }

        int maxPorLinea() {
            return getMaxCharactersPerLineCount();
        }

        boolean mismoAncho() {
            return getSizeButtonsToSameWidth();
        }

        int cuantosBotones() {
            Object[] b = getButtons();
            return (b == null) ? -1 : b.length;
        }

        Object mensaje() {
            return getMessage();
        }

        Icon icono() {
            return getIcon();
        }

        Icon iconoDe(int t) {
            return getIconForType(t);
        }

        int indiceInicial() {
            return getInitialValueIndex();
        }

        String creados() {
            return "mensaje=" + corto(createMessageArea())
                    + " botones=" + corto(createButtonArea())
                    + " separador=" + createSeparator()
                    + " acomodador=" + corto(createLayoutManager());
        }
    }

    static class Barra extends BasicToolBarUI {
        String estado() {
            return "barra=" + (toolBar != null) + " indice del foco=" + focusedCompIndex
                    + " ventana de arrastre=" + (dragWindow != null)
                    + " restriccion=" + constraintBeforeFloating
                    + " contenedor=" + (toolBarContListener != null)
                    + " foco=" + (toolBarFocusListener != null)
                    + " propiedad=" + (propertyListener != null)
                    + " anclaje=" + (dockingListener != null);
        }

        String colores() {
            return "anclaje=" + col(dockingColor) + " flotante=" + col(floatingColor)
                    + " borde de anclaje=" + col(dockingBorderColor)
                    + " borde flotante=" + col(floatingBorderColor);
        }

        String teclas() {
            return "" + upKey + "/" + downKey + "/" + leftKey + "/" + rightKey;
        }

        Border rollover(AbstractButton b) {
            return getRolloverBorder(b);
        }

        Border noRollover(AbstractButton b) {
            return getNonRolloverBorder(b);
        }
    }

    static void opciones() {
        linea("--- BasicOptionPaneUI ---");
        linea("minimo escrito=" + BasicOptionPaneUI.MinimumWidth
                + "x" + BasicOptionPaneUI.MinimumHeight);
        JOptionPane op = new JOptionPane("Hola", JOptionPane.INFORMATION_MESSAGE);
        Opciones u = new Opciones();
        linea("comparte instancia="
                + (BasicOptionPaneUI.createUI(op) == BasicOptionPaneUI.createUI(op)));
        u.installUI(op);
        linea("estado: " + u.estado());
        linea("minimo del panel=" + u.minPanel() + " maximo por linea=" + u.maxPorLinea()
                + " botones del mismo ancho=" + u.mismoAncho());
        linea("cuantos botones=" + u.cuantosBotones() + " indice inicial=" + u.indiceInicial());
        linea("mensaje=" + u.mensaje());
        linea("creados: " + u.creados());
        linea("fondo=" + col(op.getBackground()) + " frente=" + col(op.getForeground())
                + " fuente=" + fue(op.getFont()) + " opaco=" + op.isOpaque()
                + " acomodador=" + corto(op.getLayout()));
        linea("tiene componentes propios=" + u.containsCustomComponents(op));
        Dimension pref = u.getPreferredSize(op);
        linea("el preferido nunca es menor que el minimo="
                + (pref.width >= BasicOptionPaneUI.MinimumWidth
                        && pref.height >= BasicOptionPaneUI.MinimumHeight));
        linea("el preferido de otro componente="
                + u.getPreferredSize(new javax.swing.JPanel()));

        // Los tipos de dialogo y cuantos botones tiene cada uno.
        int[] tipos = {JOptionPane.DEFAULT_OPTION, JOptionPane.YES_NO_OPTION,
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.OK_CANCEL_OPTION};
        String[] nombres = {"por omision", "si/no", "si/no/cancelar", "aceptar/cancelar"};
        for (int i = 0; i < tipos.length; i++) {
            JOptionPane x = new JOptionPane("m", JOptionPane.PLAIN_MESSAGE, tipos[i]);
            Opciones ux = new Opciones();
            ux.installUI(x);
            linea("  " + nombres[i] + " -> " + ux.cuantosBotones() + " botones");
        }
        // Opciones propias.
        JOptionPane propio = new JOptionPane("m", JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[] {"A", "B", "C"});
        Opciones up = new Opciones();
        up.installUI(propio);
        linea("con opciones propias=" + up.cuantosBotones()
                + " indice inicial=" + up.indiceInicial());

        JOptionPane vacio = new JOptionPane();
        Opciones uv = new Opciones();
        uv.installUI(vacio);
        linea("vacio: mensaje=" + uv.mensaje());
    }

    static void barras() {
        linea("--- BasicToolBarUI ---");
        JToolBar tb = new JToolBar();
        tb.add(new JButton("uno"));
        tb.add(new JButton("dos"));
        Barra u = new Barra();
        linea("comparte instancia="
                + (BasicToolBarUI.createUI(tb) == BasicToolBarUI.createUI(tb)));
        u.installUI(tb);
        linea("estado: " + u.estado());
        linea("colores: " + u.colores());
        linea("teclas: " + u.teclas());
        linea("fondo=" + col(tb.getBackground()) + " frente=" + col(tb.getForeground())
                + " fuente=" + fue(tb.getFont()) + " opaca=" + tb.isOpaque()
                + " flotable=" + tb.isFloatable() + " margen=" + tb.getMargin()
                + " borde pintado=" + tb.isBorderPainted());
        linea("bordes rollover del ui=" + u.isRolloverBorders() + " flotando=" + u.isFloating());
        AbstractButton b0 = (AbstractButton) tb.getComponent(0);
        linea("el borde rollover se comparte=" + (u.rollover(b0) == u.rollover(b0)));
        linea("el de no rollover tambien=" + (u.noRollover(b0) == u.noRollover(b0)));
        linea("y son distintos entre si=" + (u.rollover(b0) != u.noRollover(b0)));
        linea("el boton quedo con el rollover=" + (b0.getBorder() == u.rollover(b0)));
        u.setRolloverBorders(false);
        linea("tras apagarlo: el boton tiene el de no rollover="
                + (b0.getBorder() == u.noRollover(b0)) + " y el ui dice " + u.isRolloverBorders());
        u.setRolloverBorders(true);

        u.setDockingColor(Color.red);
        u.setFloatingColor(Color.blue);
        linea("colores cambiados: anclaje=" + col(u.getDockingColor())
                + " flotante=" + col(u.getFloatingColor()));

        try {
            u.canDock(null, new Point(0, 0));
            linea("anclar en nulo aceptado");
        } catch (NullPointerException e) {
            linea("anclar en nulo revienta");
        }
        u.setOrientation(SwingConstants.VERTICAL);
        linea("tras poner vertical, la barra dice " + tb.getOrientation());
        u.setOrientation(SwingConstants.HORIZONTAL);
    }

    /** Expone lo protegido de {@code BasicColorChooserUI}. */
    static class Selector extends javax.swing.plaf.basic.BasicColorChooserUI {
        String estado() {
            return "selector=" + (chooser != null)
                    + " paneles propios=" + (defaultChoosers == null ? -1 : defaultChoosers.length)
                    + " muestra=" + (previewListener != null)
                    + " propiedad=" + (propertyChangeListener != null);
        }
    }

    static void selectores() {
        linea("--- BasicColorChooserUI ---");
        javax.swing.JColorChooser cc = new javax.swing.JColorChooser();
        Selector u = new Selector();
        linea("comparte instancia="
                + (javax.swing.plaf.basic.BasicColorChooserUI.createUI(cc)
                        == javax.swing.plaf.basic.BasicColorChooserUI.createUI(cc)));
        // No se instala sobre uno que ya tiene aspecto: en el JDK eso revienta, porque al
        // reemplazar los paneles el anterior los desinstala con su referencia ya en nulo.
        // El fondo, la opacidad y el acomodador del selector los pone el aspecto instalado, y
        // aca no hay ninguno; se compara lo que no depende de el.
        linea("del selector: el color arranca en=" + col(cc.getColor())
                + " se puede cambiar=" + cambiaElColor(cc));
        linea("hay modelo de seleccion=" + (cc.getSelectionModel() != null));
        // El estado de un UI recien hecho, sin instalar.
        linea("recien hecho: " + u.estado());
    }

    static boolean cambiaElColor(javax.swing.JColorChooser cc) {
        cc.setColor(new Color(1, 2, 3));
        boolean ok = new Color(1, 2, 3).equals(cc.getColor());
        cc.setColor(Color.white);
        return ok;
    }

    public static int run() {
        opciones();
        barras();
        selectores();
        return 0;
    }
}
