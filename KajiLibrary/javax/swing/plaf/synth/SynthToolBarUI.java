package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * La barra de herramientas de Synth.
 *
 * <p>Tres regiones: la barra, el contenido y la manija de arrastre. Como en todo el paquete, lo que
 * el basico resuelve con colores y bordes aca es una imagen por estado.
 */
public class SynthToolBarUI extends javax.swing.plaf.basic.BasicToolBarUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthToolBarUI();
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
        return new SynthContext(c, (r != null) ? r : Region.TOOL_BAR, style, state, true);
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
                    .paintToolBarBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        // Los botones se dibujan solos; ver la nota de la clase.
    }

    /**
     * El contenido de la barra, en su region propia.
     *
     * <p>La barra tiene dos regiones y no una: {@code ToolBar} para el marco entero y
     * {@code ToolBarContent} para donde van los botones. Estan separadas porque una barra puede
     * tener manija de arrastre, y la manija va adentro del marco pero afuera del contenido.
     *
     * @param context el contexto del contenido
     * @param g donde dibujar
     * @param bounds donde va
     */
    protected void paintContent(SynthContext context, Graphics g, Rectangle bounds) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context).paintToolBarContentBackground(
                    context, g, bounds.x, bounds.y, bounds.width, bounds.height,
                    toolBar.getOrientation());
        }
    }

    /** La distribucion de la barra; la del basico alcanza. */
    protected java.awt.LayoutManager createLayout() {
        return null;
    }

    /** El borde lo dibuja el estilo, no un {@code Border}; ver {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintToolBarBorder(context, g, x, y, w, h);
        }
    }

    /** Cualquier cambio puede querer otro estilo; ver {@link SynthLookAndFeel#actualizar}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthToolBarUI() {
    }

    protected void installDefaults() {
        super.installDefaults();
        updateStyle(toolBar);
    }

    protected void uninstallDefaults() {
        style = null;
        super.uninstallDefaults();
    }

    protected void installListeners() {
        super.installListeners();
        toolBar.addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        toolBar.removePropertyChangeListener(this);
        super.uninstallListeners();
    }
}
