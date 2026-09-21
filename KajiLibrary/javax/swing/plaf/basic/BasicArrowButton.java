package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * A small button with an arrow: the one that goes at a scroll bar's ends.
 *
 * <h2>It paints itself, without going through its look and feel</h2>
 *
 * <p>It redefines {@link #paint} entirely, so the {@code ButtonUI} it has installed never
 * draws. It is deliberate: the arrow has to look the same whatever look and feel it comes from,
 * because its size and its four colours are given to it by whoever creates it -- the bar -- and
 * not by a table of values.
 *
 * <p>The relief is the same as the bar's thumb, and it is measured in JDK 25: a one-pixel frame
 * in the dark shadow of which only the right side and the bottom one stay in sight, the
 * background filled, a highlight line inside at the top and on the left, and a shadow one
 * inside at the bottom and on the right. Pressed, the frame becomes light shadow and the arrow
 * shifts one pixel: it is all that is needed for it to look sunken.
 *
 * <p>The arrow is a triangle of lines, not a filled shape: that way it comes out symmetrical
 * pixel by pixel in the four directions. Disabled it is drawn twice, in shadow and in highlight
 * shifted one pixel, which is the same relief a disabled label uses.
 */
public class BasicArrowButton extends JButton implements SwingConstants {

    /** Which way it points: {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}. */
    protected int direction;

    private Color shadow;
    private Color darkShadow;
    private Color highlight;

    /** A button with those four colours; they are given by whoever creates it. */
    public BasicArrowButton(int direction, Color background, Color shadow, Color darkShadow,
            Color highlight) {
        super();
        setRequestFocusEnabled(false);
        setDirection(direction);
        setBackground(background);
        this.shadow = shadow;
        this.darkShadow = darkShadow;
        this.highlight = highlight;
    }

    /**
     * A button with the look and feel's colours: control, shadow, dark shadow and light
     * highlight.
     *
     * <p>The four are Metal's measured in JDK 25: (238, 238, 238), (184, 207, 229),
     * (122, 138, 153) and white. With no {@code UIManager}, they are written here.
     */
    public BasicArrowButton(int direction) {
        this(direction, new Color(238, 238, 238), new Color(184, 207, 229),
                new Color(122, 138, 153), new Color(255, 255, 255));
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
        setFocusable(false);
    }

    /** See the class note. */
    public void paint(Graphics g) {
        int w = getSize().width;
        int h = getSize().height;
        Color origColor = g.getColor();
        boolean isPressed = getModel().isPressed();
        boolean isEnabled = isEnabled();

        if (isPressed) {
            g.setColor(getBackground());
            g.fillRect(1, 1, w - 2, h - 2);
            g.setColor(shadow);
            g.drawRect(0, 0, w - 1, h - 1);
        } else {
            g.setColor(darkShadow);
            g.drawRect(0, 0, w - 1, h - 1);
            g.setColor(getBackground());
            g.fillRect(0, 0, w - 1, h - 1);
            g.setColor(highlight);
            g.drawLine(1, 1, 1, h - 2);
            g.drawLine(2, 1, w - 3, 1);
            g.setColor(shadow);
            g.drawLine(1, h - 2, w - 2, h - 2);
            g.drawLine(w - 2, 1, w - 2, h - 2);
        }

        // With no room for the arrow, the relief is all that is left.
        if (h < 5 || w < 5) {
            g.setColor(origColor);
            return;
        }

        if (isPressed) {
            g.translate(1, 1);
        }

        int size = Math.min((h - 4) / 3, (w - 4) / 3);
        size = Math.max(size, 2);
        paintTriangle(g, (w - size) / 2, (h - size) / 2, size, direction, isEnabled);

        if (isPressed) {
            g.translate(-1, -1);
        }
        g.setColor(origColor);
    }

    /** Sixteen by sixteen: the size the bar takes the length of its ends from. */
    public Dimension getPreferredSize() {
        return new Dimension(16, 16);
    }

    /** Five by five: less than that and there is no arrow left, only relief. */
    public Dimension getMinimumSize() {
        return new Dimension(5, 5);
    }

    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /** No: the focus goes to the bar, not to its arrows. */
    public boolean isFocusTraversable() {
        return false;
    }

    /**
     * It draws the triangle in that box and in that direction.
     *
     * <p>It is public because other looks and feels use it in order to draw the same arrow
     * somewhere else -- a drop-down's button, a menu's tip --, and none of them would want to
     * repeat the arithmetic.
     */
    public void paintTriangle(Graphics g, int x, int y, int size, int direction,
            boolean isEnabled) {
        Color oldColor = g.getColor();
        int mid;
        int i;
        int j;

        j = 0;
        size = Math.max(size, 2);
        mid = (size / 2) - 1;

        g.translate(x, y);
        if (isEnabled) {
            g.setColor(darkShadow);
        } else {
            g.setColor(shadow);
        }

        if (direction == NORTH) {
            for (i = 0; i < size; i++) {
                g.drawLine(mid - i, i, mid + i, i);
            }
            if (!isEnabled) {
                g.setColor(highlight);
                g.drawLine(mid - i + 2, i, mid + i, i);
            }
        } else if (direction == SOUTH) {
            if (!isEnabled) {
                g.translate(1, 1);
                g.setColor(highlight);
                for (i = size - 1; i >= 0; i--) {
                    g.drawLine(mid - i, j, mid + i, j);
                    j++;
                }
                g.translate(-1, -1);
                g.setColor(shadow);
            }
            j = 0;
            for (i = size - 1; i >= 0; i--) {
                g.drawLine(mid - i, j, mid + i, j);
                j++;
            }
        } else if (direction == WEST) {
            for (i = 0; i < size; i++) {
                g.drawLine(i, mid - i, i, mid + i);
            }
            if (!isEnabled) {
                g.setColor(highlight);
                g.drawLine(i, mid - i + 2, i, mid + i);
            }
        } else if (direction == EAST) {
            if (!isEnabled) {
                g.translate(1, 1);
                g.setColor(highlight);
                for (i = size - 1; i >= 0; i--) {
                    g.drawLine(j, mid - i, j, mid + i);
                    j++;
                }
                g.translate(-1, -1);
                g.setColor(shadow);
            }
            j = 0;
            for (i = size - 1; i >= 0; i--) {
                g.drawLine(j, mid - i, j, mid + i);
                j++;
            }
        }
        g.translate(-x, -y);
        g.setColor(oldColor);
    }
}
