package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;

/**
 * El boton de opcion de Metal.
 *
 * <p>Los mismos tres colores que el boton -- foco, seleccion y texto apagado -- sacados del tema.
 * Lo unico distinto es {@link #paintFocus}, que dibuja el rectangulo punteado alrededor del
 * <em>texto</em> y no del componente entero: el circulito de la izquierda no entra en el marco de
 * foco, porque marcar el circulo ademas del texto se lee como dos cosas enfocadas.
 *
 * <p>De esta clase hereda {@link MetalCheckBoxUI}, y no al reves, aunque una casilla sea mas simple
 * que un boton de opcion. Es porque el dibujo -- icono a la izquierda, texto al lado, foco alrededor
 * del texto -- es el mismo, y lo unico que cambia es de que prefijo salen los valores.
 */
public class MetalRadioButtonUI extends BasicRadioButtonUI {

    private static final MetalRadioButtonUI UNICO = new MetalRadioButtonUI();

    protected Color focusColor;
    protected Color selectColor;
    protected Color disabledTextColor;

    public MetalRadioButtonUI() {
    }

    public static ComponentUI createUI(JComponent b) {
        return UNICO;
    }

    protected Color getFocusColor() {
        if (focusColor == null) {
            focusColor = MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "focus");
        }
        return focusColor;
    }

    protected Color getSelectColor() {
        if (selectColor == null) {
            selectColor = MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "select");
        }
        return selectColor;
    }

    protected Color getDisabledTextColor() {
        if (disabledTextColor == null) {
            disabledTextColor =
                    MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "disabledText");
        }
        return disabledTextColor;
    }

    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        focusColor = MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "focus");
        selectColor = MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "select");
        disabledTextColor =
                MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "disabledText");
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        // Los tres colores no se sueltan: este UI lo comparten todos los botones del
        // programa, y soltarlos al desinstalar uno dejaria a los demas sin color. Medido.
    }

    public synchronized void paint(Graphics g, JComponent c) {
        super.paint(g, c);
    }

    /** Alrededor del texto, no del componente; ver la nota de la clase. */
    protected void paintFocus(Graphics g, Rectangle t, Dimension d) {
        g.setColor(getFocusColor());
        g.drawRect(t.x - 1, t.y - 1, t.width + 1, t.height + 1);
    }
}
