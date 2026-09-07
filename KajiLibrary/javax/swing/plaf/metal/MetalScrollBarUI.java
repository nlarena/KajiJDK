package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * La barra de desplazamiento de Metal.
 *
 * <h2>Suelta o pegada</h2>
 *
 * <p>Toda la clase gira alrededor de {@link #isFreeStanding}. Una barra <strong>suelta</strong>
 * dibuja su propio marco por los cuatro lados; una <strong>pegada</strong> -- la que va adentro de
 * un {@code JScrollPane} -- no dibuja el lado que da al panel, porque ese marco ya lo puso el
 * panel.
 *
 * <p>Quien decide no es la barra: es el panel, que le pone la propiedad de cliente
 * {@value #FREE_STANDING_PROP} en {@code false} al meterla. Una barra creada suelta arranca en
 * {@code true}, y eso esta medido. Ver {@link MetalScrollPaneUI}, que es quien la marca, y
 * {@link MetalScrollButton}, que reparte el pixel que se ahorra.
 *
 * <p>El ancho depende de eso: <strong>diecisiete suelta y quince pegada</strong>. Los dos pixeles
 * que se ahorra son justo el marco que no dibuja. De ahi salen los otros dos numeros, porque los
 * dos se calculan del ancho: el pulgar minimo es un cuadrado de ese lado, y el largo preferido es
 * {@code ancho * 3 + 10} -- los dos botones, la pista minima y el aire --. Suelta da
 * {@code 17 x 61} y pegada {@code 15 x 55}. Todo medido.
 */
public class MetalScrollBarUI extends BasicScrollBarUI {

    /** La propiedad de cliente con la que el panel avisa que la barra va pegada. */
    public static final String FREE_STANDING_PROP = "JScrollBar.isFreeStanding";

    protected MetalScrollButton increaseButton;
    protected MetalScrollButton decreaseButton;
    protected int scrollBarWidth;

    /** Suelta hasta que alguien diga lo contrario; ver la nota de la clase. */
    protected boolean isFreeStanding = true;

    public MetalScrollBarUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalScrollBarUI();
    }

    protected void installDefaults() {
        // El orden importa: primero se sabe si va suelta y despues se elige el ancho.
        Object o = scrollbar.getClientProperty(FREE_STANDING_PROP);
        if (o instanceof Boolean) {
            isFreeStanding = ((Boolean) o).booleanValue();
        }
        scrollBarWidth = isFreeStanding ? 17 : 15;
        super.installDefaults();
    }

    protected void installListeners() {
        super.installListeners();
    }

    protected void configureScrollBarColors() {
        scrollbar.setBackground(MetalLookAndFeel.getControl());
        scrollbar.setForeground(MetalLookAndFeel.getControl());
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new ScrollBarListener();
    }

    protected JButton createDecreaseButton(int orientation) {
        decreaseButton = new MetalScrollButton(orientation, scrollBarWidth, isFreeStanding);
        return decreaseButton;
    }

    protected JButton createIncreaseButton(int orientation) {
        increaseButton = new MetalScrollButton(orientation, scrollBarWidth, isFreeStanding);
        return increaseButton;
    }

    /** Un cuadrado del ancho de la barra: el pulgar nunca es mas fino que la pista. */
    protected Dimension getMinimumThumbSize() {
        return new Dimension(scrollBarWidth, scrollBarWidth);
    }

    /** {@code ancho x (ancho * 3 + 10)}; ver la nota de la clase. */
    public Dimension getPreferredSize(JComponent c) {
        int largo = scrollBarWidth * 3 + 10;
        if (((JScrollBar) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(scrollBarWidth, largo);
        }
        return new Dimension(largo, scrollBarWidth);
    }

    protected void setThumbBounds(int x, int y, int width, int height) {
        super.setThumbBounds(x, y, width, height);
    }

    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(MetalLookAndFeel.getControlShadow());
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        if (!isFreeStanding) {
            return;
        }
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawRect(trackBounds.x, trackBounds.y, trackBounds.width - 1, trackBounds.height - 1);
    }

    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
        g.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
        g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
        g.drawRect(thumbBounds.x, thumbBounds.y, thumbBounds.width - 1, thumbBounds.height - 1);
        g.setColor(MetalLookAndFeel.getPrimaryControl());
        g.drawLine(thumbBounds.x + 1, thumbBounds.y + 1,
                thumbBounds.x + thumbBounds.width - 2, thumbBounds.y + 1);
        g.drawLine(thumbBounds.x + 1, thumbBounds.y + 1,
                thumbBounds.x + 1, thumbBounds.y + thumbBounds.height - 2);
    }

    /** El que se entera de que la barra paso de suelta a pegada, o al reves. */
    private class ScrollBarListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            if (!FREE_STANDING_PROP.equals(e.getPropertyName())) {
                return;
            }
            Object v = e.getNewValue();
            isFreeStanding = (v == null) || Boolean.TRUE.equals(v);
            if (increaseButton != null) {
                increaseButton.setFreeStanding(isFreeStanding);
            }
            if (decreaseButton != null) {
                decreaseButton.setFreeStanding(isFreeStanding);
            }
        }
    }
}
