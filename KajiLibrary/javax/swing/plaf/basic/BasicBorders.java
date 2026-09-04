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
