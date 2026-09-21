package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

/**
 * One of the two arrows of a Metal scroll bar.
 *
 * <h2>The pixel given to the neighbour</h2>
 *
 * <p>This class's sizes are asymmetric and on purpose. A bar <strong>attached</strong> to a pane
 * does not draw its outer border -- the pane draws it -- so each button gives up two pixels on
 * the side facing that border. A <strong>free standing</strong> bar does draw it, and then the
 * button at the end gives up only one.
 *
 * <p>The asymmetry is exact and is measured: with width 16, the top one measures {@code 16x14}
 * always; the bottom one, {@code 16x14} attached and {@code 16x15} free standing. Horizontally,
 * the right one goes from {@code 14x16} to {@code 15x16} and the left one does not change. It is
 * always the button at the far end that gets the pixel back, because the border that is saved is
 * the other one.
 *
 * <p>A direction that is not one of the four cardinal points gives size zero. It is not an error:
 * it is what the JDK does, and a button of size zero simply is not seen.
 *
 * <p>The maximum is {@code Integer.MAX_VALUE} in both dimensions, so the button lets its
 * container stretch it as much as needed.
 */
public class MetalScrollButton extends BasicArrowButton {

    private static final Dimension MAX =
            new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private int buttonWidth;
    private boolean freeStanding;

    /**
     * @param direction one of the four cardinal points
     * @param width the width of the bar it belongs to
     * @param freeStanding whether the bar draws its own outer border
     */
    public MetalScrollButton(int direction, int width, boolean freeStanding) {
        super(direction);
        this.buttonWidth = width;
        this.freeStanding = freeStanding;
    }

    public void setFreeStanding(boolean freeStanding) {
        this.freeStanding = freeStanding;
    }

    public int getButtonWidth() {
        return buttonWidth;
    }

    /** See the class note: the asymmetry is measured. */
    public Dimension getPreferredSize() {
        int d = getDirection();
        if (d == SwingConstants.NORTH) {
            return new Dimension(buttonWidth, buttonWidth - 2);
        }
        if (d == SwingConstants.SOUTH) {
            return new Dimension(buttonWidth, buttonWidth - (freeStanding ? 1 : 2));
        }
        if (d == SwingConstants.EAST) {
            return new Dimension(buttonWidth - (freeStanding ? 1 : 2), buttonWidth);
        }
        if (d == SwingConstants.WEST) {
            return new Dimension(buttonWidth - 2, buttonWidth);
        }
        return new Dimension(0, 0);
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /** No cap: the container stretches it as much as it likes. */
    public Dimension getMaximumSize() {
        return new Dimension(MAX);
    }

    public void paint(Graphics g) {
        super.paint(g);
    }
}
