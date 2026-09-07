package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * El escritorio de Synth, donde viven las ventanas internas.
 *
 * <p>Agrega dos metodos que ninguna otra clase del paquete tiene:
 * {@link #installDesktopManager} y {@link #uninstallDesktopManager}. El administrador de escritorio
 * es quien decide que pasa al minimizar, maximizar o cerrar una ventana interna, y Synth lo separa
 * del resto de la instalacion porque un aspecto puede querer cambiar ese comportamiento sin tocar
 * nada del dibujo.
 */
public class SynthDesktopPaneUI extends javax.swing.plaf.basic.BasicDesktopPaneUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthDesktopPaneUI();
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
        return new SynthContext(c, (r != null) ? r : Region.DESKTOP_PANE, style, state, true);
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
                    .paintDesktopPaneBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        // El escritorio es todo fondo; ver la nota de la clase.
    }

    /** El borde lo dibuja el estilo, no un {@code Border}; ver {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintDesktopPaneBorder(context, g, x, y, w, h);
        }
    }

    /** Cualquier cambio puede querer otro estilo; ver {@link SynthLookAndFeel#actualizar}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthDesktopPaneUI() {
    }

    protected void installDefaults() {
        updateStyle(desktop);
    }

    protected void uninstallDefaults() {
        style = null;
    }

    protected void installListeners() {
        super.installListeners();
        desktop.addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        desktop.removePropertyChangeListener(this);
        super.uninstallListeners();
    }

    /** Quien maneja minimizar, maximizar y cerrar; ver la nota de la clase. */
    protected void installDesktopManager() {
        super.installDesktopManager();
    }

    protected void uninstallDesktopManager() {
        super.uninstallDesktopManager();
    }
}
