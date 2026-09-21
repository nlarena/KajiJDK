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
 * Metal's scroll bar.
 *
 * <h2>Free standing or attached</h2>
 *
 * <p>The whole class revolves around {@link #isFreeStanding}. A <strong>free standing</strong>
 * bar draws its own frame on all four sides; an <strong>attached</strong> one -- the one that
 * goes inside a {@code JScrollPane} -- does not draw the side facing the pane, because that
 * frame was already put there by the pane.
 *
 * <p>Who decides is not the bar: it is the pane, which sets the client property
 * {@value #FREE_STANDING_PROP} to {@code false} on putting it in. A bar created free standing
 * starts at {@code true}, and that is measured. See {@link MetalScrollPaneUI}, which is what
 * marks it, and {@link MetalScrollButton}, which shares out the pixel that is saved.
 *
 * <p>The width depends on that: <strong>seventeen free standing and fifteen attached</strong>.
 * The two pixels saved are exactly the frame it does not draw. From there come the other two
 * numbers, because both are computed from the width: the minimum thumb is a square of that side,
 * and the preferred length is {@code width * 3 + 10} -- the two buttons, the minimum track and
 * the air --. Free standing gives {@code 17 x 61} and attached {@code 15 x 55}. All measured.
 */
public class MetalScrollBarUI extends BasicScrollBarUI {

    /** The client property with which the pane reports that the bar goes attached. */
    public static final String FREE_STANDING_PROP = "JScrollBar.isFreeStanding";

    protected MetalScrollButton increaseButton;
    protected MetalScrollButton decreaseButton;
    protected int scrollBarWidth;

    /** Free standing until somebody says otherwise; see the class note. */
    protected boolean isFreeStanding = true;

    public MetalScrollBarUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalScrollBarUI();
    }

    protected void installDefaults() {
        // The order matters: first it is known whether it goes free standing and then the width is
        // chosen.
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

    /** A square of the bar's width: the thumb is never thinner than the track. */
    protected Dimension getMinimumThumbSize() {
        return new Dimension(scrollBarWidth, scrollBarWidth);
    }

    /** {@code width x (width * 3 + 10)}; see the class note. */
    public Dimension getPreferredSize(JComponent c) {
        int length = scrollBarWidth * 3 + 10;
        if (((JScrollBar) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(scrollBarWidth, length);
        }
        return new Dimension(length, scrollBarWidth);
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

    /** The one that hears that the bar went from free standing to attached, or back. */
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
