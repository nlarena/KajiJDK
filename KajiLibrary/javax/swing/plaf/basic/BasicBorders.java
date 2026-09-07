package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource$CompoundBorderUIResource;
import javax.swing.plaf.BorderUIResource$LineBorderUIResource;
import javax.swing.plaf.UIResource;

/**
 * Los bordes del aspecto basico: bisel de boton, campo grabado, margen, barra de menu.
 *
 * <p>Los colores de las fabricas estaticas son los que {@code UIManager} daria bajo Metal, medidos
 * en el JDK 25: sombra (184, 207, 229), sombra oscura (122, 138, 153), brillo y brillo claro
 * blancos. Sin {@code UIManager}, van escritos aca.
 *
 * <p>{@link MarginBorder} es el que hace que el margen de un boton cuente: no pinta nada, solo
 * declara como insets lo que {@code AbstractButton.getMargin} dice. El JDK tambien lo aplica a
 * {@code JToolBar} y a los componentes de texto, que no estan; para cualquier otro componente los
 * insets son cero.
 *
 * <p>No estan {@code SplitPaneBorder} ni {@code getSplitPaneBorder},
 * {@code getSplitPaneDividerBorder} y {@code getInternalFrameBorder}: dependen de
 * {@code JSplitPane} y de colores de {@code InternalFrame.*} que no se midieron.
 */
public class BasicBorders {

    private static final Color SOMBRA = new Color(184, 207, 229);
    private static final Color SOMBRA_OSCURA = new Color(122, 138, 153);
    private static final Color BRILLO = new Color(255, 255, 255);
    private static final Color BRILLO_CLARO = new Color(255, 255, 255);

    public BasicBorders() {
    }

    /** El borde de un boton: bisel por fuera, margen por dentro. */
    public static Border getButtonBorder() {
        return new BorderUIResource$CompoundBorderUIResource(
                new ButtonBorder(SOMBRA, SOMBRA_OSCURA, BRILLO, BRILLO_CLARO), new MarginBorder());
    }

    public static Border getRadioButtonBorder() {
        return new BorderUIResource$CompoundBorderUIResource(
                new RadioButtonBorder(SOMBRA, SOMBRA_OSCURA, BRILLO, BRILLO_CLARO),
                new MarginBorder());
    }

    public static Border getToggleButtonBorder() {
        return new BorderUIResource$CompoundBorderUIResource(
                new ToggleButtonBorder(SOMBRA, SOMBRA_OSCURA, BRILLO, BRILLO_CLARO),
                new MarginBorder());
    }

    public static Border getMenuBarBorder() {
        return new MenuBarBorder(SOMBRA, BRILLO);
    }

    public static Border getTextFieldBorder() {
        return new FieldBorder(SOMBRA, SOMBRA_OSCURA, BRILLO, BRILLO_CLARO);
    }

    /**
     * El borde de un panel dividido: una linea alrededor, y nada donde va el divisor.
     *
     * <p>El hueco del divisor es la parte interesante: el borde no dibuja su propio trazo ahi,
     * porque el divisor tiene el suyo y los dos juntos se verian como una linea doble.
     */
    public static Border getSplitPaneBorder() {
        return new SplitPaneBorder(BRILLO, SOMBRA_OSCURA);
    }

    /** El borde del divisor en si; una linea de un pixel de cada lado. */
    public static Border getSplitPaneDividerBorder() {
        return new SplitPaneDividerBorder(BRILLO, SOMBRA_OSCURA);
    }

    /** El de una barra de progreso: dos pixeles de linea. */
    public static Border getProgressBarBorder() {
        return new BorderUIResource$LineBorderUIResource(SOMBRA_OSCURA, 2);
    }

    /** El de una ventana interna: dos lineas por fuera y una por dentro. */
    public static Border getInternalFrameBorder() {
        return new BorderUIResource$CompoundBorderUIResource(
                new BorderUIResource$LineBorderUIResource(SOMBRA_OSCURA, 2),
                new BorderUIResource$LineBorderUIResource(SOMBRA, 1));
    }

    /**
     * El borde de un panel dividido; ver {@link #getSplitPaneBorder}.
     *
     * <p>Los dos colores son publicos-protegidos y estan medidos: el claro arriba y a la izquierda,
     * el oscuro abajo y a la derecha, que es lo que hace que el panel se vea hundido.
     */
    public static class SplitPaneBorder implements Border, UIResource {

        protected Color highlight;
        protected Color shadow;

        public SplitPaneBorder(Color highlight, Color shadow) {
            this.highlight = highlight;
            this.shadow = shadow;
        }

        /**
         * La linea de alrededor, salteando el ancho del divisor.
         *
         * <p>Si el componente no es un panel dividido se dibuja el rectangulo entero: es lo unico
         * que se puede hacer sin saber donde esta el divisor.
         */
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (!(c instanceof javax.swing.JSplitPane)) {
                g.setColor(shadow);
                g.drawRect(x, y, width - 1, height - 1);
                return;
            }
            javax.swing.JSplitPane splitPane = (javax.swing.JSplitPane) c;
            Component izq = splitPane.getLeftComponent();
            Component der = splitPane.getRightComponent();
            g.setColor(highlight);
            g.drawLine(x, y, x + width - 1, y);
            g.drawLine(x, y, x, y + height - 1);
            g.setColor(shadow);
            g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
            g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
            // El hueco: donde termina un hijo y empieza el otro no va nada.
            if (izq != null && der != null) {
                g.setColor(c.getBackground());
                if (splitPane.getOrientation() == javax.swing.JSplitPane.HORIZONTAL_SPLIT) {
                    int dx = izq.getWidth() + x;
                    g.drawLine(dx, y, dx + splitPane.getDividerSize() - 1, y);
                    g.drawLine(dx, y + height - 1, dx + splitPane.getDividerSize() - 1,
                            y + height - 1);
                } else {
                    int dy = izq.getHeight() + y;
                    g.drawLine(x, dy, x, dy + splitPane.getDividerSize() - 1);
                    g.drawLine(x + width - 1, dy, x + width - 1,
                            dy + splitPane.getDividerSize() - 1);
                }
            }
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    /**
     * El borde del divisor de un panel dividido.
     *
     * <p>No es publico en el JDK y aca tampoco: lo unico publico es
     * {@link #getSplitPaneDividerBorder}, que devuelve uno. El nombre igual se ve por
     * {@code getClass()}, asi que es el del JDK.
     */
    static class SplitPaneDividerBorder implements Border, UIResource {

        Color highlight;
        Color shadow;

        SplitPaneDividerBorder(Color highlight, Color shadow) {
            this.highlight = highlight;
            this.shadow = shadow;
        }

        /** Una linea de cada lado, en el sentido perpendicular al que divide. */
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Component padre = c.getParent();
            boolean horizontal = true;
            if (padre instanceof javax.swing.JSplitPane) {
                horizontal = ((javax.swing.JSplitPane) padre).getOrientation()
                        == javax.swing.JSplitPane.HORIZONTAL_SPLIT;
            }
            g.setColor(highlight);
            if (horizontal) {
                g.drawLine(x, y, x, y + height - 1);
                g.setColor(shadow);
                g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
            } else {
                g.drawLine(x, y, x + width - 1, y);
                g.setColor(shadow);
                g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
            }
        }

        /**
         * Uno de cada lado, en el sentido que divide.
         *
         * <p>Un divisor horizontal tiene sus lineas a izquierda y derecha, asi que sus insets son
         * (0, 1, 0, 1); uno vertical, al reves. Y un componente que no es un divisor -- o uno que
         * todavia no tiene panel -- se lleva uno de cada lado. Los tres casos estan medidos.
         */
        public Insets getBorderInsets(Component c) {
            if (c instanceof BasicSplitPaneDivider) {
                BasicSplitPaneUI ui = ((BasicSplitPaneDivider) c).getBasicSplitPaneUI();
                if (ui != null) {
                    javax.swing.JSplitPane sp = ui.getSplitPane();
                    if (sp != null) {
                        if (sp.getOrientation() == javax.swing.JSplitPane.HORIZONTAL_SPLIT) {
                            return new Insets(0, 1, 0, 1);
                        }
                        return new Insets(1, 0, 1, 0);
                    }
                }
            }
            return new Insets(1, 1, 1, 1);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    /** El bisel de un boton: levantado en reposo, hundido al apretar, con marco si es el por omision. */
    public static class ButtonBorder extends AbstractBorder implements UIResource {

        protected Color shadow;
        protected Color darkShadow;
        protected Color highlight;
        protected Color lightHighlight;

        public ButtonBorder(Color shadow, Color darkShadow, Color highlight,
                Color lightHighlight) {
            this.shadow = shadow;
            this.darkShadow = darkShadow;
            this.highlight = highlight;
            this.lightHighlight = lightHighlight;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            boolean apretado = false;
            boolean porOmision = false;
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                ButtonModel modelo = b.getModel();
                apretado = modelo.isPressed() && modelo.isArmed();
                if (c instanceof JButton) {
                    porOmision = ((JButton) c).isDefaultButton();
                }
            }
            BasicGraphicsUtils.drawBezel(g, x, y, width, height, apretado, porOmision, shadow,
                    darkShadow, highlight, lightHighlight);
        }

        /** Dos arriba y tres en los otros lados: el pixel de menos arriba deja lugar al marco. */
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 2;
            insets.left = 3;
            insets.bottom = 3;
            insets.right = 3;
            return insets;
        }
    }

    /** El bisel de un boton con estado: hundido mientras esta seleccionado. */
    public static class ToggleButtonBorder extends ButtonBorder {

        public ToggleButtonBorder(Color shadow, Color darkShadow, Color highlight,
                Color lightHighlight) {
            super(shadow, darkShadow, highlight, lightHighlight);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            boolean hundido = false;
            if (c instanceof AbstractButton) {
                ButtonModel modelo = ((AbstractButton) c).getModel();
                hundido = (modelo.isArmed() && modelo.isPressed()) || modelo.isSelected();
            }
            if (hundido) {
                BasicGraphicsUtils.drawLoweredBezel(g, x, y, width, height, shadow, darkShadow,
                        highlight, lightHighlight);
            } else {
                BasicGraphicsUtils.drawBezel(g, x, y, width, height, false, false, shadow,
                        darkShadow, highlight, lightHighlight);
            }
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 2;
            insets.left = 2;
            insets.bottom = 2;
            insets.right = 2;
            return insets;
        }
    }

    /** El bisel de un boton de radio: hundido si esta seleccionado, con marco si tiene el foco. */
    public static class RadioButtonBorder extends ButtonBorder {

        public RadioButtonBorder(Color shadow, Color darkShadow, Color highlight,
                Color lightHighlight) {
            super(shadow, darkShadow, highlight, lightHighlight);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                ButtonModel modelo = b.getModel();
                if ((modelo.isArmed() && modelo.isPressed()) || modelo.isSelected()) {
                    BasicGraphicsUtils.drawLoweredBezel(g, x, y, width, height, shadow,
                            darkShadow, highlight, lightHighlight);
                } else {
                    BasicGraphicsUtils.drawBezel(g, x, y, width, height, false,
                            b.isFocusPainted() && b.hasFocus(), shadow, darkShadow, highlight,
                            lightHighlight);
                }
            } else {
                BasicGraphicsUtils.drawBezel(g, x, y, width, height, false, false, shadow,
                        darkShadow, highlight, lightHighlight);
            }
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 2;
            insets.left = 2;
            insets.bottom = 2;
            insets.right = 2;
            return insets;
        }
    }

    /** El margen de un boton, como borde; ver la nota de la clase. */
    public static class MarginBorder extends AbstractBorder implements UIResource {

        public MarginBorder() {
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            Insets margen = null;
            if (c instanceof AbstractButton) {
                margen = ((AbstractButton) c).getMargin();
            }
            insets.top = margen != null ? margen.top : 0;
            insets.left = margen != null ? margen.left : 0;
            insets.bottom = margen != null ? margen.bottom : 0;
            insets.right = margen != null ? margen.right : 0;
            return insets;
        }
    }

    /**
     * El borde de un campo de texto: un rectangulo grabado.
     *
     * <p>En el JDK los insets suman el margen del {@code JTextComponent}; sin componentes de texto,
     * son los dos pixeles del grabado.
     */
    public static class FieldBorder extends AbstractBorder implements UIResource {

        protected Color shadow;
        protected Color darkShadow;
        protected Color highlight;
        protected Color lightHighlight;

        public FieldBorder(Color shadow, Color darkShadow, Color highlight,
                Color lightHighlight) {
            this.shadow = shadow;
            this.darkShadow = darkShadow;
            this.highlight = highlight;
            this.lightHighlight = lightHighlight;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            BasicGraphicsUtils.drawEtchedRect(g, x, y, width, height, shadow, darkShadow,
                    highlight, lightHighlight);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 2;
            insets.left = 2;
            insets.bottom = 2;
            insets.right = 2;
            return insets;
        }
    }

    /** El borde de una barra de menu: una linea de sombra y una de brillo, abajo. */
    public static class MenuBarBorder extends AbstractBorder implements UIResource {

        private Color shadow;
        private Color highlight;

        public MenuBarBorder(Color shadow, Color highlight) {
            this.shadow = shadow;
            this.highlight = highlight;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color viejo = g.getColor();
            g.translate(x, y);
            g.setColor(shadow);
            g.drawLine(0, height - 2, width, height - 2);
            g.setColor(highlight);
            g.drawLine(0, height - 1, width, height - 1);
            g.translate(-x, -y);
            g.setColor(viejo);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 0;
            insets.left = 0;
            insets.bottom = 2;
            insets.right = 0;
            return insets;
        }
    }
}
