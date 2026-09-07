package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * El deslizador de Synth.
 *
 * <p>Tres regiones, como la barra de desplazamiento: {@code Slider}, {@code SliderTrack} y
 * {@code SliderThumb}. Y una vuelta mas: el pulgar de un deslizador de Synth puede tener un
 * <strong>tamano distinto por estado</strong>, porque su icono es un {@link SynthIcon}. Un pulgar
 * que crece al pasarle el mouse es algo que el basico no puede expresar.
 */
public class SynthSliderUI extends javax.swing.plaf.basic.BasicSliderUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthSliderUI((javax.swing.JSlider) c);
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
        return new SynthContext(c, (r != null) ? r : Region.SLIDER, style, state, true);
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
                    .paintSliderBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
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
     * @param context el contexto de la pista
     * @param g donde dibujar
     * @param trackBounds donde va
     */
    protected void paintTrack(SynthContext context, Graphics g, Rectangle trackBounds) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context).paintSliderTrackBackground(
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
            context.getStyle().getPainter(context).paintSliderThumbBackground(
                    context, g, thumbBounds.x, thumbBounds.y,
                    thumbBounds.width, thumbBounds.height,
                    slider.getOrientation());
        }
    }

    /**
     * Rehace los seis rectangulos.
     *
     * <p>Se llama sola cada vez que hace falta y no forma parte de instalar nada: el pulgar de
     * Synth puede cambiar de tamano al cambiar de estado, asi que la distribucion no se puede
     * calcular una sola vez.
     */
    protected void layout() {
        calculateGeometry();
    }

    /** El borde lo dibuja el estilo, no un {@code Border}; ver {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintSliderBorder(context, g, x, y, w, h);
        }
    }

    /** Cualquier cambio puede querer otro estilo; ver {@link SynthLookAndFeel#actualizar}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    /**
     * Uno para ese deslizador.
     *
     * <p>Es el unico constructor y es {@code protected}: quien instala es {@link #createUI}. No hay
     * version sin argumentos, y no es un olvido -- un deslizador de Synth necesita el componente
     * desde el principio para poder preguntarle su estado --. Medido.
     *
     * @param c el deslizador
     */
    protected SynthSliderUI(javax.swing.JSlider c) {
        super(c);
    }

    protected void installDefaults(javax.swing.JSlider s) {
        super.installDefaults(s);
        updateStyle(s);
    }

    protected void uninstallDefaults(javax.swing.JSlider s) {
        style = null;
        super.uninstallDefaults(s);
    }

    protected void installListeners(javax.swing.JSlider s) {
        super.installListeners(s);
        s.addPropertyChangeListener(this);
    }

    protected void uninstallListeners(javax.swing.JSlider s) {
        s.removePropertyChangeListener(this);
        super.uninstallListeners(s);
    }
}
