package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ProgressBarUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a progress bar.
 *
 * <h2>Two different bars in one class</h2>
 *
 * <p>A <em>determinate</em> bar knows how much is left and fills up bit by bit: it is a rule of
 * three between the model's value and the available width. An <em>indeterminate</em> one knows
 * nothing and the only thing it says is "I am still working": a block that comes and goes. The
 * two share the border, the colours and the text, and there the likeness ends, which is why
 * there are two painting methods.
 *
 * <h2>The block that bounces</h2>
 *
 * <p>The block measures a sixth of the available length -- {@link #getBoxLength}, rounded --
 * and travels the length there and back in {@link #getFrameCount} frames. The number of frames
 * is not a constant: it comes from dividing how long the cycle lasts by how often it is
 * repainted, and in Metal that gives sixty.
 *
 * <p><strong>{@link #getBox} blows up if the bar has not been indeterminate yet.</strong> The
 * internal measurements it needs are computed when the bar <em>enters</em> indeterminate mode,
 * and before that they are null. It is measured: the JDK throws {@code NullPointerException}
 * there, and it is copied -- a subclass that calls {@code getBox} outside
 * {@code paintIndeterminate} has to break the same way in both libraries.
 *
 * <h2>The cells that are no longer used</h2>
 *
 * <p>{@link #getCellLength} and {@link #getCellSpacing} hold 1 and 0, and nobody looks at them.
 * They are from when the bar was drawn as a row of separate little blocks; the basic look and
 * feel draws it filled. They stay because a subclass may go back to that style.
 *
 * <h2>The minimum does not depend on the border</h2>
 *
 * <p>A horizontal bar's minimum width is ten pixels, and that is that: the margins are not
 * added to it. It is odd -- the preferred one does add them -- and it is measured with two
 * different borders.
 *
 * <h2>Baseline</h2>
 *
 * <p>It only has one if the bar shows its text. With no text there is nothing to rest and the
 * answer is -1 with behaviour {@code OTHER}.
 */
public class BasicProgressBarUI extends ProgressBarUI {

    protected JProgressBar progressBar;
    protected ChangeListener changeListener;

    /** The bouncing block's rectangle; null until the bar has been indeterminate. */
    protected Rectangle boxRect;

    private PropertyChangeListener propertyListener;
    private Timer animator;
    private int animationIndex = 0;
    private int numFrames = 0;
    private int cellLength = 1;
    private int cellSpacing = 0;
    private Color selectionForeground;
    private Color selectionBackground;

    /** The area inside the border; null until the first indeterminate mode. */
    private Rectangle componentInnards;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(163, 184, 204);
    private static final ColorUIResource SEL_BACKGROUND = new ColorUIResource(99, 130, 191);
    private static final ColorUIResource SEL_FOREGROUND = new ColorUIResource(238, 238, 238);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);
    private static final DimensionUIResource INNER_H = new DimensionUIResource(146, 12);
    private static final DimensionUIResource INNER_V = new DimensionUIResource(12, 146);

    /**
     * How long a there-and-back lasts, and how often it is repainted; the frames come from that.
     */
    private static final int CYCLE_TIME = 3000;
    private static final int REPAINT_INTERVAL = 50;

    public BasicProgressBarUI() {
    }

    /** A new one per bar: it keeps the component and the animation's state. */
    public static ComponentUI createUI(JComponent x) {
        return new BasicProgressBarUI();
    }

    public void installUI(JComponent c) {
        progressBar = (JProgressBar) c;
        installDefaults();
        installListeners();
        if (progressBar.isIndeterminate()) {
            startIndeterminate();
        }
    }

    public void uninstallUI(JComponent c) {
        if (progressBar.isIndeterminate()) {
            stopIndeterminate();
        }
        uninstallDefaults();
        uninstallListeners();
        progressBar = null;
    }

    /**
     * Colours, typeface, border and opacity; the values are those of {@code ProgressBar.*} in
     * Metal.
     */
    protected void installDefaults() {
        Color background = progressBar.getBackground();
        if (background == null || background instanceof UIResource) {
            progressBar.setBackground(BACKGROUND);
        }
        Color foreground = progressBar.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            progressBar.setForeground(FOREGROUND);
        }
        Font font = progressBar.getFont();
        if (font == null || font instanceof UIResource) {
            progressBar.setFont(FONT);
        }
        Border border = progressBar.getBorder();
        if (border == null || border instanceof UIResource) {
            progressBar.setBorder(new BorderUIResource.LineBorderUIResource(SEL_BACKGROUND, 1));
        }
        LookAndFeel.installProperty(progressBar, "opaque", Boolean.TRUE);
        selectionBackground = SEL_BACKGROUND;
        selectionForeground = SEL_FOREGROUND;
        cellLength = 1;
        cellSpacing = 0;
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        changeListener = new Handler();
        progressBar.addChangeListener(changeListener);
        propertyListener = new Handler();
        progressBar.addPropertyChangeListener(propertyListener);
    }

    protected void uninstallListeners() {
        progressBar.removeChangeListener(changeListener);
        progressBar.removePropertyChangeListener(propertyListener);
        changeListener = null;
        propertyListener = null;
    }

    /** How many frames a there-and-back lasts; zero until the bar has been indeterminate. */
    protected final int getFrameCount() {
        return numFrames;
    }

    /** Which frame the animation is on. */
    protected int getAnimationIndex() {
        return animationIndex;
    }

    /** It sets it and repaints. */
    protected void setAnimationIndex(int newValue) {
        if (animationIndex != newValue) {
            animationIndex = newValue;
            if (progressBar != null) {
                progressBar.repaint();
            }
        }
    }

    /** It goes to the next frame, going back to zero at the end of the cycle. */
    protected void incrementAnimationIndex() {
        int newValue = getAnimationIndex() + 1;
        setAnimationIndex((numFrames > 0 && newValue < numFrames) ? newValue : 0);
    }

    /** It starts the animation's timer. */
    protected void startAnimationTimer() {
        if (animator == null) {
            animator = new Timer(REPAINT_INTERVAL, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    incrementAnimationIndex();
                }
            });
        }
        animator.start();
    }

    protected void stopAnimationTimer() {
        if (animator != null) {
            animator.stop();
        }
        setAnimationIndex(0);
    }

    private void startIndeterminate() {
        numFrames = CYCLE_TIME / REPAINT_INTERVAL;
        componentInnards = new Rectangle();
        boxRect = new Rectangle();
        updateInner();
        startAnimationTimer();
    }

    private void stopIndeterminate() {
        stopAnimationTimer();
    }

    private void updateInner() {
        Insets b = progressBar.getInsets();
        componentInnards.setBounds(b.left, b.top,
                progressBar.getWidth() - (b.left + b.right),
                progressBar.getHeight() - (b.top + b.bottom));
    }

    protected int getCellLength() {
        return cellLength;
    }

    protected void setCellLength(int cellLen) {
        this.cellLength = cellLen;
    }

    protected int getCellSpacing() {
        return cellSpacing;
    }

    protected void setCellSpacing(int cellSpace) {
        this.cellSpacing = cellSpace;
    }

    protected Color getSelectionForeground() {
        return selectionForeground;
    }

    protected Color getSelectionBackground() {
        return selectionBackground;
    }

    /** A sixth of the length, rounded; see the class note. */
    protected int getBoxLength(int availableLength, int otherDimension) {
        return (int) Math.round(availableLength / 6.0);
    }

    /**
     * Where the bouncing block is on this frame.
     *
     * @throws NullPointerException if the bar has not been indeterminate yet; see the class note
     */
    protected Rectangle getBox(Rectangle r) {
        // This call is the one that blows up ahead of time, and it is on purpose:
                // `componentInnards` is null until the bar enters indeterminate mode. See the class
                // note.
        updateInner();
        if (r == null) {
            r = new Rectangle();
        }
        int frames = (numFrames > 0) ? numFrames : 1;
        boolean horizontal = progressBar.getOrientation() == SwingConstants.HORIZONTAL;
        int length = horizontal ? componentInnards.width : componentInnards.height;
        int thick = horizontal ? componentInnards.height : componentInnards.width;
        int block = getBoxLength(length, thick);
        int walk = length - block;
        if (walk < 0) {
            walk = 0;
        }
        // The way there takes half the frames and the way back the other half.
        int half = frames / 2;
        int i = getAnimationIndex();
        int paso;
        if (half == 0) {
            paso = 0;
        } else if (i < half) {
            paso = walk * i / half;
        } else {
            paso = walk * (frames - i) / half;
        }
        if (horizontal) {
            r.x = componentInnards.x + paso;
            r.y = componentInnards.y;
            r.width = block;
            r.height = componentInnards.height;
        } else {
            r.x = componentInnards.x;
            r.y = componentInnards.y + paso;
            r.width = componentInnards.width;
            r.height = block;
        }
        return r;
    }

    /** How much of the bar is full, in pixels. */
    protected int getAmountFull(Insets b, int width, int height) {
        int amountFull = 0;
        BoundedRangeModel model = progressBar.getModel();
        if ((model.getMaximum() - model.getMinimum()) != 0) {
            if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
                amountFull = (int) Math.round(width * progressBar.getPercentComplete());
            } else {
                amountFull = (int) Math.round(height * progressBar.getPercentComplete());
            }
        }
        return amountFull;
    }

    protected Dimension getPreferredInnerHorizontal() {
        return new DimensionUIResource(INNER_H.width, INNER_H.height);
    }

    protected Dimension getPreferredInnerVertical() {
        return new DimensionUIResource(INNER_V.width, INNER_V.height);
    }

    /** The inside plus the margins, enlarged if the text does not fit. */
    public Dimension getPreferredSize(JComponent c) {
        Dimension size;
        Insets border = progressBar.getInsets();
        FontMetrics fontSizer = progressBar.getFontMetrics(progressBar.getFont());
        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            // A copy, and in a bare `Dimension`: what `getPreferredInnerHorizontal` returns
                        // belongs to the look and feel, and a component's preferred size does not.
            size = new Dimension(getPreferredInnerHorizontal());
            if (progressBar.isStringPainted()) {
                int stringHeight = fontSizer.getHeight() + fontSizer.getDescent();
                if (stringHeight > size.height) {
                    size.height = stringHeight;
                }
                String text = progressBar.getString();
                if (text != null) {
                    int stringWidth = fontSizer.stringWidth(text);
                    if (stringWidth > size.width) {
                        size.width = stringWidth;
                    }
                }
            }
        } else {
            size = new Dimension(getPreferredInnerVertical());
            if (progressBar.isStringPainted()) {
                int stringWidth = fontSizer.getHeight() + fontSizer.getDescent();
                if (stringWidth > size.width) {
                    size.width = stringWidth;
                }
                String text = progressBar.getString();
                if (text != null) {
                    int stringHeight = fontSizer.stringWidth(text);
                    if (stringHeight > size.height) {
                        size.height = stringHeight;
                    }
                }
            }
        }
        size.width += border.left + border.right;
        size.height += border.top + border.bottom;
        return size;
    }

    /** Ten pixels long; see the class note. */
    public Dimension getMinimumSize(JComponent c) {
        Dimension pref = getPreferredSize(progressBar);
        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            pref.width = 10;
        } else {
            pref.height = 10;
        }
        return pref;
    }

    /** It stretches lengthwise and not at all widthwise. */
    public Dimension getMaximumSize(JComponent c) {
        Dimension pref = getPreferredSize(progressBar);
        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            pref.width = Short.MAX_VALUE;
        } else {
            pref.height = Short.MAX_VALUE;
        }
        return pref;
    }

    /**
     * Where the text rests, or -1 if there is no text; see the class note.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        if (!progressBar.isStringPainted()) {
            return -1;
        }
        FontMetrics metrics = progressBar.getFontMetrics(progressBar.getFont());
        Insets insets = progressBar.getInsets();
        int y = insets.top;
        int innerHeight = height - insets.top - insets.bottom;
        int ascent = metrics.getAscent();
        return y + (innerHeight - ascent - metrics.getDescent()) / 2 + ascent;
    }

    /**
     * {@code CENTER_OFFSET} with text and {@code OTHER} without it.
     *
     * @throws NullPointerException if the component is null
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        if (progressBar.isStringPainted()) {
            return Component.BaselineResizeBehavior.CENTER_OFFSET;
        }
        return Component.BaselineResizeBehavior.OTHER;
    }

    public void paint(Graphics g, JComponent c) {
        if (progressBar.isIndeterminate()) {
            paintIndeterminate(g, c);
        } else {
            paintDeterminate(g, c);
        }
    }

    /** The filled part, and the text on top if it applies. */
    protected void paintDeterminate(Graphics g, JComponent c) {
        Insets b = progressBar.getInsets();
        int barRectWidth = progressBar.getWidth() - (b.right + b.left);
        int barRectHeight = progressBar.getHeight() - (b.top + b.bottom);
        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return;
        }
        int amountFull = getAmountFull(b, barRectWidth, barRectHeight);
        g.setColor(progressBar.getForeground());
        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            g.fillRect(b.left, b.top, amountFull, barRectHeight);
        } else {
            // The vertical bar fills from the bottom upwards.
            g.fillRect(b.left, b.top + (barRectHeight - amountFull),
                    barRectWidth, amountFull);
        }
        if (progressBar.isStringPainted()) {
            paintString(g, b.left, b.top, barRectWidth, barRectHeight, amountFull, b);
        }
    }

    /** The bouncing block, and the text on top if it applies. */
    protected void paintIndeterminate(Graphics g, JComponent c) {
        Insets b = progressBar.getInsets();
        int barRectWidth = progressBar.getWidth() - (b.right + b.left);
        int barRectHeight = progressBar.getHeight() - (b.top + b.bottom);
        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return;
        }
        boxRect = getBox(boxRect);
        if (boxRect != null) {
            g.setColor(progressBar.getForeground());
            g.fillRect(boxRect.x, boxRect.y, boxRect.width, boxRect.height);
        }
        if (progressBar.isStringPainted()) {
            paintString(g, b.left, b.top, barRectWidth, barRectHeight, 0, b);
        }
    }

    /**
     * The text, with the colour changed over the filled part.
     *
     * <p>The text is painted twice with different clips: once with the usual colour over the
     * background and once with the selection one over the filled part. Without that, the text
     * would be unreadable in half the bar.
     */
    protected void paintString(Graphics g, int x, int y, int width, int height, int amountFull,
            Insets b) {
        String progressString = progressBar.getString();
        if (progressString == null) {
            return;
        }
        g.setFont(progressBar.getFont());
        Point renderLocation = getStringPlacement(g, progressString, x, y, width, height);
        java.awt.Rectangle oldClip = g.getClipBounds();

        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            g.setColor(getSelectionBackground());
            g.drawString(progressString, renderLocation.x, renderLocation.y);
            g.setColor(getSelectionForeground());
            g.clipRect(x, y, amountFull, height);
            g.drawString(progressString, renderLocation.x, renderLocation.y);
        } else {
            g.setColor(getSelectionBackground());
            g.drawString(progressString, renderLocation.x, renderLocation.y);
            g.setColor(getSelectionForeground());
            g.clipRect(x, y + height - amountFull, width, amountFull);
            g.drawString(progressString, renderLocation.x, renderLocation.y);
        }
        if (oldClip != null) {
            g.setClip(oldClip);
        }
    }

    /** Where the text starts: centred in the bar. */
    protected Point getStringPlacement(Graphics g, String progressString, int x, int y,
            int width, int height) {
        FontMetrics fontSizer = progressBar.getFontMetrics(progressBar.getFont());
        int stringWidth = fontSizer.stringWidth(progressString);
        if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
            return new Point(x + Math.round(width / 2 - stringWidth / 2),
                    y + ((height + fontSizer.getAscent() - fontSizer.getLeading()
                            - fontSizer.getDescent()) / 2));
        }
        return new Point(x + ((width - fontSizer.getAscent() + fontSizer.getLeading()
                + fontSizer.getDescent()) / 2),
                y + Math.round(height / 2 - stringWidth / 2));
    }

    /**
     * It repaints when the value changes, and starts or stops the animation when the mode changes.
     */
    private class Handler implements ChangeListener, PropertyChangeListener {

        public void stateChanged(ChangeEvent e) {
            progressBar.repaint();
        }

        public void propertyChange(PropertyChangeEvent e) {
            if ("indeterminate".equals(e.getPropertyName())) {
                if (Boolean.TRUE.equals(e.getNewValue())) {
                    startIndeterminate();
                } else {
                    stopIndeterminate();
                }
                progressBar.repaint();
            }
        }
    }
}
