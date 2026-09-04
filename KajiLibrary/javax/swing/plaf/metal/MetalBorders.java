package javax.swing.plaf.metal;

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
}
