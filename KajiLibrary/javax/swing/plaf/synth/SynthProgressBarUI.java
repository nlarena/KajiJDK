package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * La barra de progreso de Synth.
 *
 * <p>El fondo y la parte llena son dos imagenes distintas, y esa es toda la diferencia con el
 * basico. Alcanza para que una barra de Synth pueda tener textura o degradado en la parte llena,
 * que con un color plano no se puede.
 */
public class SynthProgressBarUI extends javax.swing.plaf.basic.BasicProgressBarUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthProgressBarUI();
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
        return new SynthContext(c, (r != null) ? r : Region.PROGRESS_BAR, style, state, true);
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
                    .paintProgressBarBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        super.paint(g, context.getComponent());
    }

    /**
     * El texto de adentro de la barra -- el "45%" --.
     *
     * <p>Esta separado del resto del dibujo porque en Synth el texto de una barra de progreso
     * puede ir en dos colores: uno sobre la parte llena y otro sobre la vacia. El aspecto lo
     * resuelve dibujandolo dos veces con el recorte cambiado, y por eso hace falta un metodo que
     * dibuje solo el texto.
     *
     * @param context el contexto
     * @param g donde dibujar
     * @param text el texto, o {@code null}
     */
    protected void paintText(SynthContext context, Graphics g, String text) {
        if (text == null || context == null || context.getStyle() == null) {
            return;
        }
        context.getStyle().getGraphicsUtils(context).paintText(
                context, g, text, 0, 0, -1);
    }

    /** El borde lo dibuja el estilo, no un {@code Border}; ver {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintProgressBarBorder(context, g, x, y, w, h);
        }
    }

    /** Cualquier cambio puede querer otro estilo; ver {@link SynthLookAndFeel#actualizar}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthProgressBarUI() {
    }

    protected void installDefaults() {
        super.installDefaults();
        updateStyle(progressBar);
    }

    protected void uninstallDefaults() {
        style = null;
        super.uninstallDefaults();
    }

    protected void installListeners() {
        super.installListeners();
        progressBar.addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        progressBar.removePropertyChangeListener(this);
        super.uninstallListeners();
    }
}
