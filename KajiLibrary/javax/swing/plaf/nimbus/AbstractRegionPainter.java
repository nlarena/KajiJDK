package javax.swing.plaf.nimbus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

import javax.swing.JComponent;
import javax.swing.Painter;

/**
 * The base of Nimbus's painters: it draws a region in relative coordinates.
 *
 * <h2>The problem it solves</h2>
 *
 * <p>A Nimbus button is not an image: it is a shape described with curves and gradients. That shape
 * has to look the same at twenty pixels and at two hundred, and its rounded corners have to keep
 * their radius instead of stretching.
 *
 * <p>Hence {@link #decodeX} and {@link #decodeY}: the shape is written on a grid of three bands per
 * axis -- nine cells in all -- and those methods translate it to the real size. The edge bands are
 * the margins and do not stretch; the middle one absorbs the whole difference. That is what makes a
 * rounded border keep the same radius when the component grows.
 *
 * <h2>Derived colors</h2>
 *
 * <p>{@link #decodeColor(String, float, float, float, int)} does not return a color: it returns
 * <strong>one offset</strong> from a base color in the table. That is how the whole of Nimbus is
 * recolored by changing a handful of colors: everything else is written as offsets in hue,
 * saturation and brightness over those few.
 *
 * <p>Without it, recoloring Nimbus would mean touching the thousand values that came out of the
 * design tool.
 *
 * <h2>{@link #paint} is final</h2>
 *
 * <p>What a subclass writes is {@link #doPaint}. {@code paint} keeps what must not vary: setting up
 * antialiasing, working out the scale and -- if the context asks for it -- storing the result in the
 * cache. Leaving it overridable would mean every painter had to remember all of that.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The arithmetic is real: the nine-cell grid, the derived colors and the gradients are really
 * worked out. What it cannot do is draw, because {@link #doPaint} is written by each concrete
 * painter and this library has none -- they are the ninety private classes of the package.
 *
 * @since 1.7
 */
public abstract class AbstractRegionPainter implements Painter<JComponent> {

    /** The component being painted right now, for {@link #getComponentColor}. */
    private JComponent current;

    /** The real size of the component being painted; the {@code decode} methods need it. */
    private int currentWidth;
    private int currentHeight;

    /** One. */
    protected AbstractRegionPainter() {
    }

    /**
     * Draws the region.
     *
     * @param g where to draw
     * @param c the component
     * @param w the width
     * @param h the height
     */
    public final void paint(Graphics2D g, JComponent c, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final PaintContext ctx = getPaintContext();
        if (ctx == null) {
            return;
        }
        current = c;
        currentWidth = w;
        currentHeight = h;
        try {
            configureGraphics(g);
            doPaint(g, c, w, h, getExtendedCacheKeys(c));
        } finally {
            current = null;
            currentWidth = 0;
            currentHeight = 0;
        }
    }

    /**
     * What, besides the size, tells this drawing apart, for the cache.
     *
     * <p>A painter that always draws the same thing returns {@code null}. One that looks at a color
     * of the component returns that color: if it did not, two components of different colors would
     * share the stored image and the second would come out in the first one's color.
     *
     * @param c the component
     * @return what tells it apart, or {@code null}
     */
    protected Object[] getExtendedCacheKeys(JComponent c) {
        return null;
    }

    /**
     * How this region is drawn: its margins, its reference size and whether it can be cached.
     *
     * @return the context; {@code null} means nothing is drawn
     */
    protected abstract PaintContext getPaintContext();

    /**
     * Sets up the graphics context before drawing.
     *
     * <p>It turns on antialiasing, which is what keeps Nimbus's curves from looking stepped. A
     * subclass may change it.
     *
     * @param g the graphics context
     */
    protected void configureGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     * The drawing proper.
     *
     * @param g where to draw
     * @param c the component
     * @param width the width
     * @param height the height
     * @param extendedCacheKeys what {@link #getExtendedCacheKeys} returned
     */
    protected abstract void doPaint(Graphics2D g, JComponent c, int width, int height,
            Object[] extendedCacheKeys);

    /**
     * Translates a horizontal grid coordinate to the real size.
     *
     * <p>The grid runs from zero to three: the band from zero to one is the left margin, the one
     * from one to two the centre, and the one from two to three the right margin. The margins
     * measure the same whatever the component's size; the centre absorbs the difference.
     *
     * @param x the coordinate on the grid
     * @return the coordinate in pixels
     */
    protected final float decodeX(float x) {
        final Insets m = margins();
        return decodeOn(x, m.left, currentWidth - m.left - m.right, m.right);
    }

    /**
     * Translates a vertical grid coordinate to the real size.
     *
     * @param y the coordinate on the grid
     * @return the coordinate in pixels
     */
    protected final float decodeY(float y) {
        final Insets m = margins();
        return decodeOn(y, m.top, currentHeight - m.top - m.bottom, m.bottom);
    }

    /**
     * Like {@link #decodeX} but shifting the result by a few pixels.
     *
     * <p>The shift is applied <strong>after</strong> translating, so it does not stretch with the
     * component. It is what a line's thickness is done with: a one-pixel border has to stay one
     * pixel on a large button.
     *
     * @param x the coordinate on the grid
     * @param dx how many pixels to shift it by
     * @return the coordinate in pixels
     */
    protected final float decodeAnchorX(float x, float dx) {
        return decodeX(x) + dx;
    }

    /**
     * Like {@link #decodeY} but shifting the result by a few pixels.
     *
     * @param y the coordinate on the grid
     * @param dy how many pixels to shift it by
     * @return the coordinate in pixels
     */
    protected final float decodeAnchorY(float y, float dy) {
        return decodeY(y) + dy;
    }

    /**
     * A color offset from one in the table.
     *
     * <p>The first three offsets are hue, saturation and brightness, each between minus one and one;
     * the fourth is transparency, between minus two hundred and fifty-five and two hundred and
     * fifty-five. It is what allows the whole of Nimbus to be recolored by changing a few base
     * colors.
     *
     * @param key the key of the base color in the table
     * @param hOffset how far to shift the hue
     * @param sOffset how far to shift the saturation
     * @param bOffset how far to shift the brightness
     * @param aOffset how far to shift the transparency
     * @return the color, or the base one if the key is not there
     */
    protected final Color decodeColor(String key, float hOffset, float sOffset, float bOffset,
            int aOffset) {
        final Object v = javax.swing.UIManager.get(key);
        final Color base = v instanceof Color ? (Color) v : Color.GRAY;
        return shifted(base, hOffset, sOffset, bOffset, aOffset);
    }

    /**
     * A color between two, at that proportion.
     *
     * @param color1 the one at one end
     * @param color2 the one at the other
     * @param midPoint how much of the second, between zero and one
     * @return the color in between
     */
    protected final Color decodeColor(Color color1, Color color2, float midPoint) {
        return new Color(
                mix(color1.getRed(), color2.getRed(), midPoint),
                mix(color1.getGreen(), color2.getGreen(), midPoint),
                mix(color1.getBlue(), color2.getBlue(), midPoint),
                mix(color1.getAlpha(), color2.getAlpha(), midPoint));
    }

    /**
     * A linear gradient between two points.
     *
     * <p>If the two points coincide no gradient is possible; the second is shifted by a
     * hundred-thousandth, which is what the JDK does. It is ugly and it beats throwing an exception
     * in the middle of a repaint.
     *
     * @param x1 from
     * @param y1 from
     * @param x2 to
     * @param y2 to
     * @param midpoints at what proportion each color sits, in increasing order
     * @param colors the colors
     * @return the gradient
     */
    protected final LinearGradientPaint decodeGradient(float x1, float y1, float x2, float y2,
            float[] midpoints, Color[] colors) {
        float fx2 = x2;
        float fy2 = y2;
        if (x1 == fx2 && y1 == fy2) {
            fy2 += 0.00001f;
        }
        return new LinearGradientPaint(new Point2D.Float(x1, y1), new Point2D.Float(fx2, fy2),
                midpoints, colors);
    }

    /**
     * A radial gradient out of a centre.
     *
     * @param x the centre
     * @param y the centre
     * @param r the radius
     * @param midpoints at what proportion each color sits, in increasing order
     * @param colors the colors
     * @return the gradient
     */
    protected final RadialGradientPaint decodeRadialGradient(float x, float y, float r,
            float[] midpoints, Color[] colors) {
        final float radius = r == 0f ? 0.00001f : r;
        return new RadialGradientPaint(new Point2D.Float(x, y), radius, midpoints, colors,
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    /**
     * A color of the component itself, offset.
     *
     * <p>It is what lets a button the program gave a background color be drawn in that color and not
     * in the table's. If the component does not have that color, or has one put there by the look and
     * feel rather than by the program, the matching one from the table is used.
     *
     * @param c the component
     * @param property which color is asked for: {@code "background"} or {@code "foreground"}
     * @param defaultColor what to use if the component does not have it
     * @param saturationOffset how far to shift the saturation
     * @param brightnessOffset how far to shift the brightness
     * @param alphaOffset how far to shift the transparency
     * @return the color
     */
    protected final Color getComponentColor(JComponent c, String property, Color defaultColor,
            float saturationOffset, float brightnessOffset, int alphaOffset) {
        Color base = defaultColor;
        final JComponent comp = c == null ? current : c;
        if (comp != null) {
            if ("background".equals(property)) {
                base = comp.getBackground();
            } else if ("foreground".equals(property)) {
                base = comp.getForeground();
            }
        }
        if (base == null) {
            base = defaultColor;
        }
        return base == null ? null : shifted(base, 0f, saturationOffset, brightnessOffset,
                alphaOffset);
    }

    /**
     * Draws the region.
     *
     * <p>This is {@link Painter}'s version, with the type not narrowed. Anything that is not a
     * {@link JComponent} is not drawn: this painter looks at the component to decide its colors, and
     * without a component there is nothing to look at.
     *
     * @param g where to draw
     * @param object the component
     * @param width the width
     * @param height the height
     */
    public void paint(Graphics2D g, Object object, int width, int height) {
        if (object instanceof JComponent) {
            paint(g, (JComponent) object, width, height);
        }
    }

    /** The offset in hue, saturation, brightness and transparency. */
    private static Color shifted(Color base, float h, float s, float b, int a) {
        final float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        final float hh = clamp(hsb[0] + h);
        final float ss = clamp(hsb[1] + s);
        final float bb = clamp(hsb[2] + b);
        final int aa = Math.max(0, Math.min(255, base.getAlpha() + a));
        final Color c = Color.getHSBColor(hh, ss, bb);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), aa);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }

    private static int mix(int a, int b, float p) {
        final int v = Math.round(a + (b - a) * p);
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    /** The margins that do not stretch, or all zeros when there is no context. */
    private Insets margins() {
        final PaintContext ctx = getPaintContext();
        final Insets m = ctx == null ? null : ctx.margins();
        return m == null ? NO_MARGIN : m;
    }

    private static final Insets NO_MARGIN = new Insets(0, 0, 0, 0);

    /**
     * The grid coordinate translated to the real size.
     *
     * <p>The grid has three bands and runs from zero to three. The first and the third are the
     * margins and <strong>do not stretch</strong>: they measure the same on a small button as on a
     * large one. The middle one absorbs all the rest, which is what makes a rounded corner keep the
     * same radius when the component grows.
     *
     * @throws IllegalArgumentException if the coordinate falls outside the grid
     */
    private static float decodeOn(float v, int first, int middle, int third) {
        if (v >= 0f && v <= 1f) {
            return v * first;
        }
        if (v > 1f && v < 2f) {
            return (v - 1f) * middle + first;
        }
        if (v >= 2f && v <= 3f) {
            return (v - 2f) * third + first + middle;
        }
        throw new IllegalArgumentException("outside the grid: " + v);
    }

    /**
     * How a region is drawn: margins, reference size and whether the result can be cached.
     *
     * <h2>The reference size</h2>
     *
     * <p>It is the size the shape was drawn for. The {@code decode} methods translate from that grid
     * to the real size, and without it there would be nothing to translate from.
     *
     * <h2>The cache</h2>
     *
     * <p>Drawing a shape with gradients and antialiasing is expensive, and a component is repainted
     * many times without changing. Storing the result is worth it, and when it is worth it depends on
     * the shape: that is why the mode is part of the context and not a global decision.
     *
     * @since 1.7
     */
    public static class PaintContext {

        private final Insets insets;
        private final Dimension canvasSize;
        private final boolean inverted;
        private final CacheMode cacheMode;
        private final double maxH;
        private final double maxV;

        /**
         * A context that caches nothing.
         *
         * @param insets the margins that do not stretch
         * @param canvasSize the size the shape was drawn for
         * @param inverted whether the grid is read the other way round
         */
        public PaintContext(Insets insets, Dimension canvasSize, boolean inverted) {
            this(insets, canvasSize, inverted, null, 1, 1);
        }

        /**
         * A full context.
         *
         * @param insets the margins that do not stretch
         * @param canvasSize the size the shape was drawn for
         * @param inverted whether the grid is read the other way round
         * @param cacheMode how to store the result, or {@code null} not to store it
         * @param maxH how far what is stored may be scaled horizontally
         * @param maxV how far what is stored may be scaled vertically
         */
        public PaintContext(Insets insets, Dimension canvasSize, boolean inverted,
                CacheMode cacheMode, double maxH, double maxV) {
            this.insets = insets;
            this.canvasSize = canvasSize;
            this.inverted = inverted;
            this.cacheMode = cacheMode == null ? CacheMode.NO_CACHING : cacheMode;
            this.maxH = maxH;
            this.maxV = maxV;
        }

        /** The margins that do not stretch; for the painter wrapping it. */
        Insets margins() {
            return insets;
        }

        /**
         * How the result of drawing is cached.
         *
         * @since 1.7
         */
        public static enum CacheMode {

            /** Not cached. */
            NO_CACHING,

            /** One image is stored for each size that turns up. */
            FIXED_SIZES,

            /**
             * A single image is stored and stretched by cells.
             *
             * <p>It is what lets a button of any width be drawn from a single image without the
             * corners deforming: the four corner cells are copied as they are and only the middle
             * ones stretch.
             */
            NINE_SQUARE_SCALE,
        }
    }
}
