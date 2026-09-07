package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.JPanel;

/**
 * El panel de Synth.
 *
 * <p>Un panel no dibuja nada suyo salvo el fondo, asi que aca la separacion entre fondo y
 * contenido queda a la vista: {@code paint(SynthContext, Graphics)} no hace nada, y todo lo que se
 * ve lo pinto {@code update}.
 */
public class SynthPanelUI extends javax.swing.plaf.basic.BasicPanelUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthPanelUI();
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
        return new SynthContext(c, (r != null) ? r : Region.PANEL, style, state, true);
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
                    .paintPanelBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        // Un panel no tiene contenido propio; ver la nota de la clase.
    }

    /** El borde lo dibuja el estilo, no un {@code Border}; ver {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintPanelBorder(context, g, x, y, w, h);
        }
    }

    /** Cualquier cambio puede querer otro estilo; ver {@link SynthLookAndFeel#actualizar}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthPanelUI() {
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    protected void installDefaults(JPanel p) {
        updateStyle(p);
    }

    protected void uninstallDefaults(JPanel p) {
        style = null;
    }

    protected void installListeners(JPanel p) {
        p.addPropertyChangeListener(this);
    }

    protected void uninstallListeners(JPanel p) {
        p.removePropertyChangeListener(this);
    }
}
