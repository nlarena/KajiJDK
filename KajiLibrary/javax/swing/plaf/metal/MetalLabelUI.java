package javax.swing.plaf.metal;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 * La etiqueta de Metal.
 *
 * <p>Cambia una sola cosa del basico, y es como se ve una etiqueta apagada. El basico la dibuja
 * dos veces corrida un pixel -- blanco y despues gris -- que es el grabado de Windows 95. Metal la
 * dibuja una sola vez en el gris del tema. Es mas plano y es lo que corresponde a un aspecto que
 * no imita a nadie.
 *
 * <p>Comparte instancia: {@link #createUI} devuelve siempre la misma. Puede hacerlo porque no
 * guarda nada de la etiqueta que dibuja, y una sola instancia para las mil etiquetas de un programa
 * es la razon de que exista {@link #metalLabelUI}.
 */
public class MetalLabelUI extends BasicLabelUI {

    /** La unica; ver la nota de la clase. */
    protected static MetalLabelUI metalLabelUI = new MetalLabelUI();

    public MetalLabelUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return metalLabelUI;
    }

    /** Una sola pasada en el gris del tema; ver la nota de la clase. */
    protected void paintDisabledText(JLabel l, Graphics g, String s, int textX, int textY) {
        int indice = l.getDisplayedMnemonicIndex();
        g.setColor(MetalLookAndFeel.getInactiveSystemTextColor());
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, s, indice, textX, textY);
    }
}
