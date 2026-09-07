package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.BorderUIResource$CompoundBorderUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicBorders$MarginBorder;

/**
 * Los bordes del aspecto Metal; por ahora, el del boton.
 *
 * <p>Metal tiene dos temas, Steel y Ocean, y desde el JDK 6 el que se ve es Ocean. Este borde pinta
 * lo que pinta Ocean, medido en el JDK 25: un rectangulo de un pixel en la sombra oscura del tema
 * (122, 138, 153); hundido, ese mismo color en dos pixeles arriba y a la izquierda y uno abajo y a
 * la derecha; deshabilitado, el gris del texto inactivo (153, 153, 153). Los colores estan aca como
 * constantes porque {@code MetalLookAndFeel}, que los tendria como tema, no esta.
 *
 * <p>Lo que Ocean pinta ademas —el degradado del fondo del boton— no es del borde sino de
 * {@code MetalButtonUI.update}, y no esta: el fondo es plano.
 */
public class MetalBorders {

    private static final Color SOMBRA_OSCURA = new Color(122, 138, 153);
    private static final Color CONTROL_PRIMARIO = new Color(184, 207, 229);
    private static final Color TEXTO_INACTIVO = new Color(153, 153, 153);
    private static final Color BRILLO = new Color(255, 255, 255);
    private static final Color CONTROL = new Color(238, 238, 238);

    private static Border buttonBorder;
    private static Border toggleButtonBorder;

    public MetalBorders() {
    }

    /** El borde de un boton en Ocean; ver la nota de la clase. */
    public static class ButtonBorder extends AbstractBorder implements UIResource {

        /** Tres pixeles por lado; el margen del boton va adentro de estos. */
        protected static Insets borderInsets = new Insets(3, 3, 3, 3);

        public ButtonBorder() {
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (!(c instanceof AbstractButton)) {
                return;
            }
            AbstractButton boton = (AbstractButton) c;
            ButtonModel modelo = boton.getModel();
            g.translate(x, y);
            if (modelo.isEnabled()) {
                boolean porOmision = (c instanceof JButton) && ((JButton) c).isDefaultButton();
                if (porOmision) {
                    g.setColor(SOMBRA_OSCURA);
                    g.drawRect(0, 0, w - 1, h - 1);
                    g.drawRect(1, 1, w - 3, h - 3);
                } else if (modelo.isPressed()) {
                    g.setColor(SOMBRA_OSCURA);
                    g.fillRect(0, 0, w, 2);
                    g.fillRect(0, 2, 2, h - 2);
                    g.fillRect(w - 1, 1, 1, h - 1);
                    g.fillRect(1, h - 1, w - 2, 1);
                } else if (modelo.isRollover() && boton.isRolloverEnabled()) {
                    g.setColor(CONTROL_PRIMARIO);
                    g.drawRect(0, 0, w - 1, h - 1);
                    g.drawRect(1, 1, w - 3, h - 3);
                    g.setColor(SOMBRA_OSCURA);
                    g.drawRect(0, 0, w - 1, h - 1);
                } else {
                    g.setColor(SOMBRA_OSCURA);
                    g.drawRect(0, 0, w - 1, h - 1);
                }
            } else {
                g.setColor(TEXTO_INACTIVO);
                g.drawRect(0, 0, w - 1, h - 1);
                if ((c instanceof JButton) && ((JButton) c).isDefaultButton()) {
                    g.drawRect(1, 1, w - 3, h - 3);
                }
            }
            g.translate(-x, -y);
        }

        public Insets getBorderInsets(Component c, Insets newInsets) {
            newInsets.top = 3;
            newInsets.left = 3;
            newInsets.bottom = 3;
            newInsets.right = 3;
            return newInsets;
        }
    }

    /**
     * El borde de un boton con estado en Ocean.
     *
     * <p>Es el mismo trazo que {@link ButtonBorder}, medido: seleccionado sin apretar se ve como
     * en reposo, y apretado se hunde igual que un boton comun. Lo que distingue a un boton con
     * estado seleccionado es el fondo, que lo pinta su UI, no el borde.
     */
    public static class ToggleButtonBorder extends ButtonBorder {

        public ToggleButtonBorder() {
        }
    }

    /**
     * El borde de un panel con barras de desplazamiento.
     *
     * <p>Un rectangulo oscuro, dos lineas de brillo por fuera abajo y a la derecha —blancas, o sea
     * invisibles sobre un fondo blanco— y <strong>dos pixeles sueltos</strong> del color del
     * control: uno arriba a la derecha y otro abajo a la izquierda, donde terminan las cabeceras.
     * Esos dos pixeles son los que hacen que el marco no se cierre justo donde una cabecera de
     * fila o de columna se apoya contra el, y estan medidos igual que el resto.
     *
     * <p>Los insets son asimetricos —(1, 1, 2, 2)— porque las lineas de brillo van por fuera del
     * rectangulo, abajo y a la derecha.
     */
    public static class ScrollPaneBorder extends AbstractBorder implements UIResource {

        private static final Insets INSETS = new Insets(1, 1, 2, 2);

        public ScrollPaneBorder() {
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (!(c instanceof JScrollPane)) {
                return;
            }
            JScrollPane scroll = (JScrollPane) c;
            JComponent colHeader = scroll.getColumnHeader();
            int colHeaderHeight = 0;
            if (colHeader != null) {
                colHeaderHeight = colHeader.getHeight();
            }
            JComponent rowHeader = scroll.getRowHeader();
            int rowHeaderWidth = 0;
            if (rowHeader != null) {
                rowHeaderWidth = rowHeader.getWidth();
            }

            g.translate(x, y);

            g.setColor(SOMBRA_OSCURA);
            g.drawRect(0, 0, w - 2, h - 2);
            g.setColor(BRILLO);
            g.drawLine(w - 1, 1, w - 1, h - 1);
            g.drawLine(1, h - 1, w - 1, h - 1);

            g.setColor(CONTROL);
            g.drawLine(w - 2, 2 + colHeaderHeight, w - 2, 2 + colHeaderHeight);
            g.drawLine(1 + rowHeaderWidth, h - 2, 1 + rowHeaderWidth, h - 2);

            g.translate(-x, -y);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = INSETS.top;
            insets.left = INSETS.left;
            insets.bottom = INSETS.bottom;
            insets.right = INSETS.right;
            return insets;
        }
    }

    /**
     * El borde que Metal instala en un {@code JButton}: el de Ocean por fuera y el margen del boton
     * por dentro. Compartido: no guarda nada del boton.
     */
    public static Border getButtonBorder() {
        if (buttonBorder == null) {
            buttonBorder = new BorderUIResource$CompoundBorderUIResource(new ButtonBorder(),
                    new BasicBorders$MarginBorder());
        }
        return buttonBorder;
    }

    /** El borde que Metal instala en un {@code JToggleButton}, con el margen adentro. */
    public static Border getToggleButtonBorder() {
        if (toggleButtonBorder == null) {
            toggleButtonBorder = new BorderUIResource$CompoundBorderUIResource(
                    new ToggleButtonBorder(), new BasicBorders$MarginBorder());
        }
        return toggleButtonBorder;
    }

    /**
     * El marco de una ventana interna.
     *
     * <p>Cuatro pixeles de grueso, y el color depende de si la ventana esta activa: el primario
     * oscuro del tema cuando lo esta y la sombra cuando no. Es lo unico que distingue a simple
     * vista la ventana con la que se esta trabajando de las de atras, porque las dos tienen barra
     * de titulo.
     */
    public static class InternalFrameBorder extends AbstractBorder implements UIResource {

        private static final Insets MARGENES = new Insets(4, 4, 4, 4);

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            boolean activa = (c instanceof javax.swing.JInternalFrame)
                    && ((javax.swing.JInternalFrame) c).isSelected();
            g.setColor(activa
                    ? MetalLookAndFeel.getPrimaryControlDarkShadow()
                    : MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(x, y, w - 1, h - 1);
            g.drawRect(x + 1, y + 1, w - 3, h - 3);
            g.setColor(activa
                    ? MetalLookAndFeel.getPrimaryControlShadow()
                    : MetalLookAndFeel.getControlShadow());
            g.drawRect(x + 2, y + 2, w - 5, h - 5);
            g.drawRect(x + 3, y + 3, w - 7, h - 7);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(MARGENES.top, MARGENES.left, MARGENES.bottom, MARGENES.right);
            return insets;
        }
    }

    /**
     * El marco de una ventana interna en modo paleta: un solo pixel.
     *
     * <p>Cuatro pixeles serian la cuarta parte del alto de una paleta chica. Que sea uno es lo que
     * hace que una paleta se vea como una ventanita y no como un marco con algo adentro.
     */
    public static class PaletteBorder extends AbstractBorder implements UIResource {

        private static final Insets MARGENES = new Insets(1, 1, 1, 1);

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(x, y, w - 1, h - 1);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(MARGENES.top, MARGENES.left, MARGENES.bottom, MARGENES.right);
            return insets;
        }
    }

    private static Border paletteBorder;

    /** El marco de una paleta; ver {@link PaletteBorder}. */
    public static Border getPaletteBorder() {
        if (paletteBorder == null) {
            paletteBorder = new PaletteBorder();
        }
        return paletteBorder;
    }

    private static Border internalFrameBorder;

    /** El marco de una ventana interna; ver {@link InternalFrameBorder}. */
    public static Border getInternalFrameBorder() {
        if (internalFrameBorder == null) {
            internalFrameBorder = new InternalFrameBorder();
        }
        return internalFrameBorder;
    }

    private static Border textBorder;
    private static Border textFieldBorder;
    private static Border desktopIconBorder;

    /**
     * El borde de un componente de texto: un pixel de marco y un pixel de aire.
     *
     * <p>Los margenes son {@code (2,2,2,2)} y no {@code (1,1,1,1)}, y ese pixel de mas es lo que
     * hace que el cursor no quede pegado a la linea del marco. Medido.
     *
     * <p>Es el mismo objeto que devuelve {@link #getTextFieldBorder}: los dos existen porque el
     * JDK los declara distintos, pero valen lo mismo.
     */
    public static Border getTextBorder() {
        if (textBorder == null) {
            textBorder = new BorderUIResource$CompoundBorderUIResource(
                    new Flush3DBorder(), new BasicBorders$MarginBorder());
        }
        return textBorder;
    }

    /** El de un {@code JTextField}; ver {@link #getTextBorder}. */
    public static Border getTextFieldBorder() {
        if (textFieldBorder == null) {
            textFieldBorder = new BorderUIResource$CompoundBorderUIResource(
                    new Flush3DBorder(), new BasicBorders$MarginBorder());
        }
        return textFieldBorder;
    }

    /**
     * El de una ventana interna minimizada.
     *
     * <p>Los margenes son {@code (3,3,2,3)}: uno menos abajo. La asimetria es del JDK y esta
     * medida; el icono de escritorio se dibuja como una ventanita apoyada, y la base lleva menos
     * aire que los costados.
     */
    public static Border getDesktopIconBorder() {
        if (desktopIconBorder == null) {
            desktopIconBorder = new BorderUIResource$CompoundBorderUIResource(
                    new LineBorder(MetalLookAndFeel.getControlDarkShadow(), 1),
                    new MatteBorder(2, 2, 1, 2, MetalLookAndFeel.getControl()));
        }
        return desktopIconBorder;
    }

    /** Un marco de un pixel en la sombra del tema, con un pixel de aire adentro. */
    public static class Flush3DBorder extends AbstractBorder implements UIResource {

        private static final Insets MARGENES = new Insets(2, 2, 2, 2);

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(x, y, w - 1, h - 1);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(MARGENES.top, MARGENES.left, MARGENES.bottom, MARGENES.right);
            return insets;
        }
    }

}
