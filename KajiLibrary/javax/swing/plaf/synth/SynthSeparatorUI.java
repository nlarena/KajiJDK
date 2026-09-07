package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;

import javax.swing.JSeparator;
import javax.swing.SwingConstants;

/**
 * El separador de Synth.
 *
 * <p>Es la unica clase del paquete que <strong>no</strong> hereda de una {@code BasicXxxUI}: sale
 * directo de {@link javax.swing.plaf.SeparatorUI}. Un separador de Synth es una imagen del estilo
 * de punta a punta, y no le queda nada que reutilizar del basico -- ni siquiera las dos lineas de
 * relieve, que son justo lo que Synth reemplaza --.
 *
 * <p>Por eso los tres tamanos los tiene que dar esta clase, y salen del estilo: la clave
 * {@code "Separator.thickness"}, con dos por omision.
 */
public class SynthSeparatorUI extends javax.swing.plaf.SeparatorUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthSeparatorUI();
    }

    public SynthContext getContext(JComponent c) {
        return getContext(c, SynthLookAndFeel.estadoDe(c));
    }

    /**
     * El contexto con ese estado.
     *
     * <p>La region sale del componente y no de una constante fija, y eso importa en las cadenas de
     * herencia: {@code SynthCheckBoxUI} hereda este metodo de {@code SynthButtonUI} y tiene que
     * contestar {@code CheckBox}, no {@code Button}. Medido.
     */
    private SynthContext getContext(JComponent c, int state) {
        Region r = SynthLookAndFeel.getRegion(c);
        return new SynthContext(c, (r != null) ? r : Region.SEPARATOR, style, state, true);
    }

    /** Le pide el estilo a la fabrica; revienta si no hay, y esta medido. */
    private void updateStyle(JComponent c) {
        style = SynthLookAndFeel.actualizar(getContext(c, SynthConstants.ENABLED));
    }

    /**
     * Dibuja el fondo y despues el contenido.
     *
     * <p>Synth separa las dos cosas: el fondo lo pinta el estilo -- que sabe en que estado esta el
     * componente -- y el contenido lo pinta el aspecto basico. Por eso {@code update} no es
     * {@code paint} con un relleno adelante, como en el basico, sino dos pasos distintos.
     */
    public void update(Graphics g, JComponent c) {
        SynthContext context = getContext(c);
        if (context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintSeparatorBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        // El separador es todo fondo; ver la nota de la clase.
    }

    /** El borde lo dibuja el estilo, no un {@code Border}; ver {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintSeparatorBorder(context, g, x, y, w, h);
        }
    }

    /** Cualquier cambio puede querer otro estilo; ver {@link SynthLookAndFeel#actualizar}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthSeparatorUI() {
    }

    public void installUI(JComponent c) {
        installDefaults((JSeparator) c);
        installListeners((JSeparator) c);
    }

    public void uninstallUI(JComponent c) {
        uninstallListeners((JSeparator) c);
        uninstallDefaults((JSeparator) c);
    }

    public void installDefaults(JSeparator c) {
        updateStyle(c);
    }

    public void uninstallDefaults(JSeparator c) {
        style = null;
    }

    public void installListeners(JSeparator c) {
        c.addPropertyChangeListener(this);
    }

    public void uninstallListeners(JSeparator c) {
        c.removePropertyChangeListener(this);
    }

    /** El grosor sale del estilo; dos si no lo dice. */
    private int grosor(JComponent c) {
        SynthContext context = getContext(c);
        if (context.getStyle() == null) {
            return 2;
        }
        return context.getStyle().getInt(context, "Separator.thickness", 2);
    }

    public Dimension getPreferredSize(JComponent c) {
        int g = grosor(c);
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(g, 0);
        }
        return new Dimension(0, g);
    }

    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** Sin tope a lo largo: un separador se estira todo lo que haga falta. */
    public Dimension getMaximumSize(JComponent c) {
        int g = grosor(c);
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(g, Integer.MAX_VALUE);
        }
        return new Dimension(Integer.MAX_VALUE, g);
    }
}
