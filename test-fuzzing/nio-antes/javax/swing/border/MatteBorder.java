package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.Icon;

/**
 * A solid border, of one colour or tiled with an icon.
 *
 * <h2>The two things that set it apart from {@link LineBorder}</h2>
 *
 * <p>The first is that the four sides may have <strong>different thicknesses</strong> -- hence
 * it extends {@link EmptyBorder}, which already knows how to carry four margins. A three-pixel
 * line only at the top is a separator, and with {@code LineBorder} it cannot be written.
 *
 * <p>The second is the icon: instead of a colour, the border is filled by repeating an image.
 * There the drawing is clipped to each side of the frame before tiling, because otherwise the
 * tiles would spill over the content.
 *
 * <p>With the constructor that takes a single {@link Icon} the thicknesses are taken
 * <strong>from the icon's size</strong>, which is the only reasonable measure available.
 */
public class MatteBorder extends EmptyBorder {

    private static final long serialVersionUID = 4422248989617298224L;

    protected Color color;
    protected Icon tileIcon;

    /** With the four thicknesses and a colour. */
    public MatteBorder(int top, int left, int bottom, int right, Color matteColor) {
        super(top, left, bottom, right);
        this.color = matteColor;
    }

    /** With an {@link Insets}'s thicknesses and a colour. */
    public MatteBorder(Insets borderInsets, Color matteColor) {
        super(borderInsets);
        this.color = matteColor;
    }

    /** With the four thicknesses, tiling with an icon. */
    public MatteBorder(int top, int left, int bottom, int right, Icon tileIcon) {
        super(top, left, bottom, right);
        this.tileIcon = tileIcon;
    }

    /** With an {@link Insets}'s thicknesses, tiling with an icon. */
    public MatteBorder(Insets borderInsets, Icon tileIcon) {
        super(borderInsets);
        this.tileIcon = tileIcon;
    }

    /** With the icon alone: the thicknesses come from its size. */
    public MatteBorder(Icon tileIcon) {
        this(-1, -1, -1, -1, tileIcon);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Insets i = getBorderInsets(c, new Insets(0, 0, 0, 0));
        Color old = g.getColor();
        g.translate(x, y);

        if (this.color != null) {
            g.setColor(this.color);
            g.fillRect(0, 0, width - i.right, i.top);
            g.fillRect(0, i.top, i.left, height - i.top);
            g.fillRect(i.left, height - i.bottom, width - i.left, i.bottom);
            g.fillRect(width - i.right, 0, i.right, height - i.bottom);
        } else if (this.tileIcon != null) {
            int tileWidth = this.tileIcon.getIconWidth();
            int tileHeight = this.tileIcon.getIconHeight();
            // The four sides of the frame, tiled. It is clipped before drawing because a tile
            // almost never fits a whole number of times, and the last one would spill.
            tile(c, g, 0, 0, width - i.right, i.top, tileWidth, tileHeight);
            tile(c, g, 0, i.top, i.left, height - i.top, tileWidth, tileHeight);
            tile(c, g, i.left, height - i.bottom, width - i.left, i.bottom,
                    tileWidth, tileHeight);
            tile(c, g, width - i.right, 0, i.right, height - i.bottom,
                    tileWidth, tileHeight);
        }

        g.translate(-x, -y);
        g.setColor(old);
    }

    private void tile(Component c, Graphics g, int x, int y, int width, int height,
            int tileWidth, int tileHeight) {
        if (width <= 0 || height <= 0 || tileWidth <= 0 || tileHeight <= 0) {
            return;
        }
        java.awt.Shape oldClip = g.getClip();
        g.clipRect(x, y, width, height);
        for (int fy = y; fy < y + height; fy = fy + tileHeight) {
            for (int fx = x; fx < x + width; fx = fx + tileWidth) {
                this.tileIcon.paintIcon(c, g, fx, fy);
            }
        }
        g.setClip(oldClip);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        return computeInsets(insets);
    }

    /** The thicknesses, in a new {@link Insets}. */
    public Insets getBorderInsets() {
        return computeInsets(new Insets(0, 0, 0, 0));
    }

    /**
     * Fills {@code insets}, resolving the negatives with the icon's size.
     *
     * <p>A negative thickness is the mark of "nobody told me": the single-icon constructor sets
     * it. Resolving it here and not there is what allows the icon to be changed afterwards.
     */
    private Insets computeInsets(Insets insets) {
        if (this.tileIcon != null) {
            if (this.top == -1 && this.bottom == -1 && this.left == -1 && this.right == -1) {
                int iconWidth = this.tileIcon.getIconWidth();
                int iconHeight = this.tileIcon.getIconHeight();
                insets.top = iconHeight;
                insets.bottom = iconHeight;
                insets.left = iconWidth;
                insets.right = iconWidth;
                return insets;
            }
        }
        insets.top = Math.max(this.top, 0);
        insets.left = Math.max(this.left, 0);
        insets.bottom = Math.max(this.bottom, 0);
        insets.right = Math.max(this.right, 0);
        return insets;
    }

    /** The fill colour, or {@code null} if it tiles with an icon. */
    public Color getMatteColor() {
        return this.color;
    }

    /** The icon it tiles with, or {@code null} if it is of one colour. */
    public Icon getTileIcon() {
        return this.tileIcon;
    }

    /**
     * Opaque only if it is of one colour.
     *
     * <p>An icon may have transparency, and this class has no way of knowing: promising opacity
     * there would be a bet on somebody else's image.
     */
    public boolean isBorderOpaque() {
        return this.color != null;
    }
}
