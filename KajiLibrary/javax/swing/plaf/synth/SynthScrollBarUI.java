package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * La barra de desplazamiento de Synth.
 *
 * <p>Es el componente con mas regiones de todo el paquete: {@code ScrollBar} para la barra entera,
 * {@code ScrollBarTrack} para la pista y {@code ScrollBarThumb} para el pulgar. Tres imagenes
 * distintas para una sola cosa.
 *
 * <p>Que la pista y el pulgar sean regiones y no colores es lo que permite el aspecto que ningun
 * otro paquete puede dar: un pulgar con las puntas redondeadas y un relieve en el medio, dibujado
 * como imagen y no como cuatro lineas.
 */
public class SynthScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthScrollBarUI();
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
        return new SynthContext(c, (r != null) ? r : Region.SCROLL_BAR, style, state, true);
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
                    .paintScrollBarBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
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
     * La pista, en su region propia.
     *
     * <p>La firma toma el contexto y no lo pide: el que llama ya lo tiene, y armarlo de nuevo
     * significaria volver a preguntarle el estado al componente en el medio de un dibujado.
     *
     * @param context el contexto de la pista
     * @param g donde dibujar
     * @param trackBounds donde va
     */
    protected void paintTrack(SynthContext context, Graphics g, Rectangle trackBounds) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context).paintScrollBarTrackBackground(
                    context, g, trackBounds.x, trackBounds.y,
                    trackBounds.width, trackBounds.height);
        }
    }

    /**
     * El pulgar, en la suya.
     *
     * @param context el contexto del pulgar
     * @param g donde dibujar
     * @param thumbBounds donde va
     */
    protected void paintThumb(SynthContext context, Graphics g, Rectangle thumbBounds) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context).paintScrollBarThumbBackground(
                    context, g, thumbBounds.x, thumbBounds.y,
                    thumbBounds.width, thumbBounds.height,
                    scrollbar.getOrientation());
        }
    }

    /** El borde lo dibuja el estilo, no un {@code Border}; ver {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintScrollBarBorder(context, g, x, y, w, h);
        }
    }

    /** Cualquier cambio puede querer otro estilo; ver {@link SynthLookAndFeel#actualizar}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthScrollBarUI() {
    }

    protected void installDefaults() {
        super.installDefaults();
        updateStyle(scrollbar);
    }

    protected void uninstallDefaults() {
        style = null;
        super.uninstallDefaults();
    }

    protected void installListeners() {
        super.installListeners();
        scrollbar.addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        scrollbar.removePropertyChangeListener(this);
        super.uninstallListeners();
    }

    public Dimension getPreferredSize(JComponent c) {
        return super.getPreferredSize(c);
    }

    public Dimension getMinimumSize(JComponent c) {
        return super.getMinimumSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return super.getMaximumSize(c);
    }
}
