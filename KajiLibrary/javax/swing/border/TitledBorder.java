package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * A border with a title written over the line.
 *
 * <h2>The only border in this package that has content</h2>
 *
 * <p>The rest are geometry: they reserve space and paint lines. This one carries
 * <strong>text</strong>, and that drags in everything text drags in -- a font, a colour, metrics
 * that depend on the system, and a decision about where to put it. Hence it has thirteen
 * position constants where the others have none.
 *
 * <h2>The two placement dimensions, which are independent</h2>
 *
 * <p>{@link #setTitlePosition} says at what height it goes --above the border, over it, below
 * it-- and {@link #setTitleJustification} at which side. They are separate knobs because any
 * combination is valid.
 *
 * <p>{@link #LEADING} and {@link #TRAILING} are not synonyms of {@link #LEFT} and
 * {@link #RIGHT}: they are resolved according to the component's orientation, so in a language
 * read from right to left they point the other way. It is the reason all five exist and not
 * three.
 *
 * <h2>It wraps another border</h2>
 *
 * <p>The frame it draws is not its own: it delegates to the {@link Border} it is given, and all
 * it does is reserve room for the title and write it. A {@code TitledBorder} with no inner
 * border is just the text.
 */
public class TitledBorder extends AbstractBorder {

    private static final long serialVersionUID = 8012999415147721601L;

    protected String title;
    protected Border border;
    protected int titlePosition;
    protected int titleJustification;
    protected Font titleFont;
    protected Color titleColor;

    /** The position whoever draws decides; in practice, {@link #TOP}. */
    public static final int DEFAULT_POSITION = 0;
    /** Above the top line. */
    public static final int ABOVE_TOP = 1;
    /** Over the top line, splitting it. */
    public static final int TOP = 2;
    /** Below the top line. */
    public static final int BELOW_TOP = 3;
    /** Above the bottom line. */
    public static final int ABOVE_BOTTOM = 4;
    /** Over the bottom line, splitting it. */
    public static final int BOTTOM = 5;
    /** Below the bottom line. */
    public static final int BELOW_BOTTOM = 6;

    /** The justification whoever draws decides; in practice, {@link #LEADING}. */
    public static final int DEFAULT_JUSTIFICATION = 0;
    /** Flush left, whatever the orientation. */
    public static final int LEFT = 1;
    /** Centred. */
    public static final int CENTER = 2;
    /** Flush right, whatever the orientation. */
    public static final int RIGHT = 3;
    /** At the start according to the component's orientation. */
    public static final int LEADING = 4;
    /** At the end according to the component's orientation. */
    public static final int TRAILING = 5;

    /** What is left free between the title and the box's edge. */
    protected static final int EDGE_SPACING = 2;
    /** What is left free above and below the text. */
    protected static final int TEXT_SPACING = 2;
    /** What is left free at the sides of the text. */
    protected static final int TEXT_INSET_H = 5;

    /** Only the title, with no border. */
    public TitledBorder(String title) {
        this(null, title, LEADING, DEFAULT_POSITION, null, null);
    }

    /** A border with an empty title. */
    public TitledBorder(Border border) {
        this(border, "", LEADING, DEFAULT_POSITION, null, null);
    }

    /** A border with its title. */
    public TitledBorder(Border border, String title) {
        this(border, title, LEADING, DEFAULT_POSITION, null, null);
    }

    /** The same, choosing where the title goes. */
    public TitledBorder(Border border, String title, int titleJustification, int titlePosition) {
        this(border, title, titleJustification, titlePosition, null, null);
    }

    /** The same, with a font. */
    public TitledBorder(Border border, String title, int titleJustification, int titlePosition,
            Font titleFont) {
        this(border, title, titleJustification, titlePosition, titleFont, null);
    }

    /** With everything explicit. */
    public TitledBorder(Border border, String title, int titleJustification, int titlePosition,
            Font titleFont, Color titleColor) {
        this.title = title;
        this.border = border;
        this.titleFont = titleFont;
        this.titleColor = titleColor;
        setTitleJustification(titleJustification);
        setTitlePosition(titlePosition);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Border inner = getBorder();
        String text = getTitle();
        if (text == null || text.isEmpty()) {
            // With no title there is nothing to reserve nor to write: it is the inner border as it
            // is.
            if (inner != null) {
                inner.paintBorder(c, g, x, y, width, height);
            }
            return;
        }

        Font font = getFont(c);
        FontMetrics fm = c.getFontMetrics(font);
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int position = resolvedPosition();
        Insets borderInsets = inner.getBorderInsets(c);

        // The title's box: it has the line's height and rests where the position says.
        int boxX = x + horizontalPosition(c, width, textWidth, borderInsets);
        int boxY = y + boxTop(c, position, height, textHeight);

        // The inner border goes `EDGE_SPACING` in on each side, and on the title's side it starts
        // where the box leaves it: at its middle if the text rides over the line (`TOP`, `BOTTOM`),
        // below or above it if the text goes apart. Measured against the JDK in `TOP`; the rest
        // follows the same rule.
        int bx = x + EDGE_SPACING;
        int by = y + EDGE_SPACING;
        int bw = width - EDGE_SPACING - EDGE_SPACING;
        int bh = height - EDGE_SPACING - EDGE_SPACING;
        if (position == TOP) {
            by = y + textHeight / 2;
            bh = height - textHeight / 2 - EDGE_SPACING;
        } else if (position == ABOVE_TOP) {
            by = y + textHeight;
            bh = height - textHeight - EDGE_SPACING;
        } else if (position == BOTTOM) {
            bh = height - textHeight / 2 - EDGE_SPACING;
        } else if (position == BELOW_BOTTOM) {
            bh = height - textHeight - EDGE_SPACING;
        }

        // The JDK does not paint the background behind the title: it CLIPS the title's box out of
        // the border, so the line is interrupted and whatever is underneath goes on showing. The
        // border is painted three times with three clips that together are "everything but the
        // box", and each one over a copy of the context so as not to drag the clip to whoever comes
        // next.
        int gapX = boxX - TEXT_SPACING;
        int gapW = textWidth + TEXT_SPACING + TEXT_SPACING;
        paintClippedBorder(inner, c, g, bx, by, bw, bh,
                x, y, gapX - x, height);
        paintClippedBorder(inner, c, g, bx, by, bw, bh,
                gapX + gapW, y, x + width - gapX - gapW, height);
        paintClippedBorder(inner, c, g, bx, by, bw, bh,
                gapX, y, gapW, boxY - y);
        paintClippedBorder(inner, c, g, bx, by, bw, bh,
                gapX, boxY + textHeight, gapW, y + height - boxY - textHeight);

        Color oldColor = g.getColor();
        Font oldFont = g.getFont();
        g.setFont(font);
        // The default colour is NOT the component's foreground; see getTitleColor.
        g.setColor(getTitleColor());
        g.drawString(text, boxX, boxY + fm.getAscent());
        g.setFont(oldFontOrCurrent(oldFont, font));
        g.setColor(oldColor);
    }

    /** Paints the inner border with the given clip, over a copy of the context. */
    private void paintClippedBorder(Border inner, Component c, Graphics g,
            int bx, int by, int bw, int bh, int cx, int cy, int cw, int ch) {
        if (cw <= 0 || ch <= 0) {
            return;
        }
        Graphics copy = g.create();
        copy.clipRect(cx, cy, cw, ch);
        inner.paintBorder(c, copy, bx, by, bw, bh);
        copy.dispose();
    }

    private Font oldFontOrCurrent(Font old, Font used) {
        return old != null ? old : used;
    }

    private int resolvedPosition() {
        int p = getTitlePosition();
        return p == DEFAULT_POSITION ? TOP : p;
    }

    private int horizontalPosition(Component c, int width, int textWidth, Insets borderInsets) {
        int j = getTitleJustification();
        if (j == DEFAULT_JUSTIFICATION) {
            j = LEADING;
        }
        // `LEADING`/`TRAILING` are resolved here and not in the setter: the orientation belongs to
        // the component and may change after the border is built.
        if (j == LEADING || j == TRAILING) {
            boolean izqADer = isLeftToRight(c);
            if (j == LEADING) {
                j = izqADer ? LEFT : RIGHT;
            } else {
                j = izqADer ? RIGHT : LEFT;
            }
        }
        // Measured against the JDK: the text starts past the inner border, the margin and the
        // horizontal inset, and against the right it is the same mirrored.
        if (j == CENTER) {
            return (width - textWidth) / 2;
        }
        if (j == RIGHT) {
            return width - borderInsets.right - EDGE_SPACING - TEXT_INSET_H - textWidth;
        }
        return borderInsets.left + EDGE_SPACING + TEXT_INSET_H;
    }

    /**
     * Where the title's box starts, measured from the top edge of the area.
     *
     * <p>The six cases come from measuring {@code getBaseline} in the JDK, position by position,
     * and not from reasoning about the geometry: the title's box has the line's height, and the
     * only thing that changes is what it rests against. At the top it rests on the area itself
     * ({@link #TOP}, {@link #ABOVE_TOP}) or below the border and its margin ({@link #BELOW_TOP});
     * at the bottom, against the bottom of the area ({@link #BOTTOM}, {@link #BELOW_BOTTOM}) or
     * above the border and its margin ({@link #ABOVE_BOTTOM}).
     */
    private int boxTop(Component c, int position, int height, int textHeight) {
        Insets borderInsets = getBorder().getBorderInsets(c);
        if (position == BELOW_TOP) {
            return borderInsets.top + EDGE_SPACING;
        }
        if (position == ABOVE_BOTTOM) {
            return height - textHeight - borderInsets.bottom - EDGE_SPACING;
        }
        if (position == BOTTOM || position == BELOW_BOTTOM) {
            return height - textHeight;
        }
        return 0;
    }

    /** The title's baseline: the top of its box plus the ascent. */
    private int baseline(Component c, int position, int height, int textHeight, int ascent) {
        return boxTop(c, position, height, textHeight) + ascent;
    }

    /**
     * How much it reserves, with the JDK's arithmetic measured in its seven positions.
     *
     * <p>It is not the one you would write from memory, and that is why it is said out loud: with
     * no title it reserves <em>only</em> the inner border, with no margin at all. With a title,
     * each side adds {@code EDGE_SPACING + TEXT_SPACING} --hence a one-pixel border of 5 per
     * side-- and the title's side takes the whole line height, except in {@link #TOP} and
     * {@link #BOTTOM}, where the text rides over the line and the border only has to grow up to
     * that height minus the margin. A {@code TitledBorder} inside another does not double the
     * margin.
     */
    public Insets getBorderInsets(Component c, Insets insets) {
        Border inner = getBorder();
        Insets i = inner.getBorderInsets(c);
        insets.top = i.top;
        insets.left = i.left;
        insets.right = i.right;
        insets.bottom = i.bottom;

        String text = getTitle();
        if (text == null || text.isEmpty()) {
            return insets;
        }
        int margin = (inner instanceof TitledBorder) ? 0 : EDGE_SPACING;
        int lineHeight = c.getFontMetrics(getFont(c)).getHeight();
        int position = resolvedPosition();
        if (position == ABOVE_TOP) {
            insets.top = insets.top + lineHeight - margin;
        } else if (position == TOP) {
            if (insets.top < lineHeight) {
                insets.top = lineHeight - margin;
            }
        } else if (position == BELOW_TOP) {
            insets.top = insets.top + lineHeight;
        } else if (position == ABOVE_BOTTOM) {
            insets.bottom = insets.bottom + lineHeight;
        } else if (position == BOTTOM) {
            if (insets.bottom < lineHeight) {
                insets.bottom = lineHeight - margin;
            }
        } else if (position == BELOW_BOTTOM) {
            insets.bottom = insets.bottom + lineHeight - margin;
        }
        insets.top = insets.top + margin + TEXT_SPACING;
        insets.left = insets.left + margin + TEXT_SPACING;
        insets.right = insets.right + margin + TEXT_SPACING;
        insets.bottom = insets.bottom + margin + TEXT_SPACING;
        return insets;
    }

    /**
     * Opaque only if the inner border is.
     *
     * <p>The title covers nothing on its own: the only thing that can promise opacity is the frame
     * this border wraps.
     */
    public boolean isBorderOpaque() {
        Border inner = getBorder();
        return inner != null && inner.isBorderOpaque();
    }

    /** The title. */
    public String getTitle() {
        return this.title;
    }

    /**
     * The border it wraps.
     *
     * <p>Never {@code null}, even if it was built with no border: the JDK substitutes there the
     * look and feel's default border --{@code UIManager.getBorder("TitledBorder.border")}--, and
     * measuring it confirms that a {@code TitledBorder} with no border reserves the same as one
     * with a one-pixel line. This library has no {@code UIManager}; the substitute is a one-pixel
     * grey line, shared, which is what that default value is in practice.
     */
    public Border getBorder() {
        if (this.border != null) {
            return this.border;
        }
        return DEFAULT_BORDER;
    }

    /** What stands in for {@code TitledBorder.border} without a {@code UIManager}. */
    private static final Border DEFAULT_BORDER = new LineBorder(Color.gray, 1);

    /** At what height the title goes. */
    public int getTitlePosition() {
        return this.titlePosition;
    }

    /** At which side the title goes. */
    public int getTitleJustification() {
        return this.titleJustification;
    }

    /** The title's font, or {@code null} if it follows the component's. */
    public Font getTitleFont() {
        return this.titleFont;
    }

    /**
     * The title's colour.
     *
     * <p>Never {@code null}: with none set, the JDK returns
     * {@code UIManager.getColor("TitledBorder.titleColor")}, which in its default look and feel
     * is a dark grey, (51, 51, 51) -- not the component's foreground colour, which is what one
     * would assume and what this class did before it was measured. Without {@code UIManager}, that
     * grey is the constant below, and it is what makes a title with no explicit colour come out
     * identical in both VMs.
     */
    public Color getTitleColor() {
        if (this.titleColor != null) {
            return this.titleColor;
        }
        return DEFAULT_COLOR;
    }

    /**
     * What stands in for {@code TitledBorder.titleColor} without a {@code UIManager}: Metal's
     * grey.
     */
    private static final Color DEFAULT_COLOR = new Color(51, 51, 51);

    /** Changes the title. */
    public void setTitle(String title) {
        this.title = title;
    }

    /** Changes the border it wraps. */
    public void setBorder(Border border) {
        this.border = border;
    }

    /**
     * Changes the title's height.
     *
     * @throws IllegalArgumentException if it is not one of the seven position constants
     */
    public void setTitlePosition(int titlePosition) {
        if (titlePosition < DEFAULT_POSITION || titlePosition > BELOW_BOTTOM) {
            throw new IllegalArgumentException(String.valueOf(titlePosition)
                    + " is not a valid title position");
        }
        this.titlePosition = titlePosition;
    }

    /**
     * Changes the title's side.
     *
     * @throws IllegalArgumentException if it is not one of the six justification constants
     */
    public void setTitleJustification(int titleJustification) {
        if (titleJustification < DEFAULT_JUSTIFICATION || titleJustification > TRAILING) {
            throw new IllegalArgumentException(String.valueOf(titleJustification)
                    + " is not a valid title justification");
        }
        this.titleJustification = titleJustification;
    }

    /** Changes the title's font; {@code null} to follow the component's. */
    public void setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
    }

    /** Changes the title's colour; {@code null} to follow the component's. */
    public void setTitleColor(Color titleColor) {
        this.titleColor = titleColor;
    }

    /**
     * The minimum size for the title to fit.
     *
     * <p>It is what keeps a panel from shrinking until it cuts its own title: an ordinary border
     * has nothing to say about the component's size, this one does.
     */
    public Dimension getMinimumSize(Component c) {
        Insets i = getBorderInsets(c, new Insets(0, 0, 0, 0));
        Dimension d = new Dimension(i.right + i.left, i.top + i.bottom);
        String text = getTitle();
        if (text == null || text.isEmpty()) {
            return d;
        }
        // Measured in the JDK: the width is the insets plus the text, in the seven positions --
        // without the extra `TEXT_INSET_H` one would expect, because that already travels inside
        // the insets.
        FontMetrics fm = c.getFontMetrics(getFont(c));
        d.width = d.width + fm.stringWidth(text);
        return d;
    }

    /**
     * The title's baseline.
     *
     * @throws NullPointerException if {@code c} is {@code null}
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(Component c, int width, int height) {
        if (c == null) {
            throw new NullPointerException("The component cannot be null");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height cannot be negative");
        }
        String text = getTitle();
        if (text == null || text.isEmpty()) {
            return -1;
        }
        FontMetrics fm = c.getFontMetrics(getFont(c));
        return baseline(c, resolvedPosition(), height, fm.getHeight(), fm.getAscent());
    }

    /**
     * How the baseline moves when the size changes.
     *
     * <p>It depends on where the title is: flush at the top it does not move, flush at the bottom
     * it moves with the bottom edge. The type goes with the binary name because of finding #101 --
     * see {@link AbstractBorder#getBaselineResizeBehavior}.
     */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior(Component c) {
        super.getBaselineResizeBehavior(c);
        int position = resolvedPosition();
        if (position == ABOVE_TOP || position == TOP || position == BELOW_TOP) {
            return Component$BaselineResizeBehavior.CONSTANT_ASCENT;
        }
        if (position == ABOVE_BOTTOM || position == BOTTOM || position == BELOW_BOTTOM) {
            return Component$BaselineResizeBehavior.CONSTANT_DESCENT;
        }
        return Component$BaselineResizeBehavior.OTHER;
    }

    /**
     * The title's font, falling back to the component's and then to a default one.
     *
     * <p>A difference from the JDK worth knowing: without {@link #setTitleFont}, the JDK takes
     * {@code TitledBorder.font} from {@code UIManager}, which in all of its looks and feels is a
     * <strong>bold</strong>. This VM draws every font with a single face, Dialog 12 regular --see
     * {@code jdk.internal.awt.BitmapFont}--, so a title with no explicit font comes out regular
     * here and bold there, and the difference is one of face, not of placement: with the same flat
     * font at both ends the whole border matches pixel for pixel. It is the usual font
     * substitution, said in the place where it shows.
     */
    protected Font getFont(Component c) {
        if (this.titleFont != null) {
            return this.titleFont;
        }
        if (c != null) {
            Font f = c.getFont();
            if (f != null) {
                return f;
            }
        }
        return new Font("Dialog", Font.PLAIN, 12);
    }
}
