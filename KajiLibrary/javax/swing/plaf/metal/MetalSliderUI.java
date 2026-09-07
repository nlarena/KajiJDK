package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * El deslizador de Metal.
 *
 * <h2>La pista se pinta hasta donde va el pulgar</h2>
 *
 * <p>{@link #filledSlider} arranca en {@code true} y es la diferencia visible con el basico: la
 * parte de la pista que ya se recorrio se rellena. Un programa lo apaga con la propiedad de
 * cliente {@value #SLIDER_FILL}, y de nuevo es una propiedad de cliente porque es una idea de
 * Metal y no de {@code JSlider}.
 *
 * <h2>El largo de una marca no es {@code tickLength}</h2>
 *
 * <p>{@link #getTickLength} devuelve once y el campo {@link #tickLength} vale seis. La cuenta es
 * {@code tickLength + TICK_BUFFER + 1}: seis de raya, cuatro de aire y uno de la linea de la
 * pista. Es la clase de numero que solo se entiende midiendo, y esta medido.
 *
 * <p>{@link #getTrackLength} puede dar negativo -- da {@code -14} en un deslizador sin tamano --
 * por la misma razon que en el basico: el buffer de cada lado se resta de un ancho que todavia es
 * cero. Esta medido y no se corrige.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>{@link #thumbColor} y {@link #darkShadowColor} quedan en {@code null}, y esta medido: salen de
 * dos claves que la tabla de Metal no define. {@link #highlightColor} si tiene valor.
 */
public class MetalSliderUI extends BasicSliderUI {

    /** La propiedad de cliente que apaga el relleno. */
    protected final String SLIDER_FILL = "JSlider.isFilled";

    /** El aire entre la pista y las marcas. */
    protected final int TICK_BUFFER = 4;

    protected static Color thumbColor;
    protected static Color highlightColor;
    protected static Color darkShadowColor;
    protected static int trackWidth = 7;
    protected static int tickLength = 6;
    protected static Icon horizThumbIcon;
    protected static Icon vertThumbIcon;

    protected boolean filledSlider = true;

    public MetalSliderUI() {
        super(null);
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalSliderUI();
    }

    public void installUI(JComponent c) {
        thumbColor = MetalLookAndFeel.colorDeLaTabla("Slider.thumb");
        highlightColor = MetalLookAndFeel.colorDeLaTabla("Slider.highlight");
        darkShadowColor = MetalLookAndFeel.colorDeLaTabla("Slider.darkShadow");
        horizThumbIcon = MetalIconFactory.getHorizontalSliderThumbIcon();
        vertThumbIcon = MetalIconFactory.getVerticalSliderThumbIcon();
        super.installUI(c);
        Object o = c.getClientProperty(SLIDER_FILL);
        if (o instanceof Boolean) {
            filledSlider = ((Boolean) o).booleanValue();
        }
    }

    protected PropertyChangeListener createPropertyChangeListener(JSlider slider) {
        return new MetalPropertyListener();
    }

    /** El del icono del pulgar, que depende de la orientacion. */
    protected Dimension getThumbSize() {
        Icon i = (slider != null && slider.getOrientation() == JSlider.VERTICAL)
                ? vertThumbIcon : horizThumbIcon;
        if (i == null) {
            return super.getThumbSize();
        }
        return new Dimension(i.getIconWidth(), i.getIconHeight());
    }

    protected int getTrackWidth() {
        return trackWidth;
    }

    /** Puede dar negativo; ver la nota de la clase. */
    protected int getTrackLength() {
        if (slider != null && slider.getOrientation() == JSlider.HORIZONTAL) {
            return trackRect.width;
        }
        return (trackRect == null) ? 0 : trackRect.height;
    }

    /** Cuanto sobresale el pulgar de la pista. */
    protected int getThumbOverhang() {
        return (getThumbSize().height - getTrackWidth()) / 2;
    }

    /** Once, no seis; ver la nota de la clase. */
    public int getTickLength() {
        return tickLength + TICK_BUFFER + 1;
    }

    protected void scrollDueToClickInTrack(int dir) {
        scrollByBlock(dir);
    }

    public void paintThumb(Graphics g) {
        Icon i = (slider.getOrientation() == JSlider.VERTICAL)
                ? vertThumbIcon : horizThumbIcon;
        if (i != null) {
            i.paintIcon(slider, g, thumbRect.x, thumbRect.y);
        }
    }

    public void paintTrack(Graphics g) {
        Rectangle t = trackRect;
        if (t == null || t.width <= 0 || t.height <= 0) {
            return;
        }
        g.setColor(MetalLookAndFeel.getControlShadow());
        g.fillRect(t.x, t.y, t.width, t.height);
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawRect(t.x, t.y, t.width - 1, t.height - 1);
        if (!filledSlider) {
            return;
        }
        // Lo recorrido, en el color primario; ver la nota de la clase.
        g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int hasta = thumbRect.x + thumbRect.width / 2 - t.x;
            if (hasta > 0) {
                g.fillRect(t.x + 1, t.y + 1, Math.min(hasta, t.width - 2), t.height - 2);
            }
        } else {
            int desde = thumbRect.y + thumbRect.height / 2;
            int alto = t.y + t.height - desde;
            if (alto > 0) {
                g.fillRect(t.x + 1, desde, t.width - 2, Math.min(alto, t.height - 2));
            }
        }
    }

    public void paintFocus(Graphics g) {
        g.setColor(MetalLookAndFeel.getFocusColor());
        g.drawRect(focusRect.x, focusRect.y, focusRect.width - 1, focusRect.height - 1);
    }

    protected void paintMinorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
        g.setColor(MetalLookAndFeel.getControlInfo());
        g.drawLine(x, TICK_BUFFER, x, TICK_BUFFER + tickLength / 2);
    }

    protected void paintMajorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
        g.setColor(MetalLookAndFeel.getControlInfo());
        g.drawLine(x, TICK_BUFFER, x, TICK_BUFFER + tickLength);
    }

    protected void paintMinorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
        g.setColor(MetalLookAndFeel.getControlInfo());
        g.drawLine(TICK_BUFFER, y, TICK_BUFFER + tickLength / 2, y);
    }

    protected void paintMajorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
        g.setColor(MetalLookAndFeel.getControlInfo());
        g.drawLine(TICK_BUFFER, y, TICK_BUFFER + tickLength, y);
    }

    /** El que mira la propiedad de cliente del relleno. */
    private class MetalPropertyListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            if (SLIDER_FILL.equals(e.getPropertyName())) {
                Object v = e.getNewValue();
                filledSlider = !(v instanceof Boolean) || ((Boolean) v).booleanValue();
                if (slider != null) {
                    slider.repaint();
                }
            }
        }
    }
}
