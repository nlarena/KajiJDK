package javax.swing.plaf.nimbus;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.Painter;
import javax.swing.UIManager;
import javax.swing.plaf.synth.ColorType;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthPainter;
import javax.swing.plaf.synth.SynthStyle;

/**
 * Nimbus's style: it reads from the table instead of having values of its own.
 *
 * <h2>Where it takes everything from</h2>
 *
 * <p>From {@link UIManager}, with keys built by convention: the background color of a pressed button
 * lives under {@code "Button[Pressed].background"}. Nimbus keeps no copy; it asks. That is what makes
 * a change to a value in the table show up on screen right away.
 *
 * <h2>The three sizes</h2>
 *
 * <p>A component can ask to be large, small or mini by setting a property -- {@link #LARGE_KEY} and
 * the other two -- and Nimbus scales the font by the matching factor. It is what in other looks and
 * feels would have to be done by hand, component by component.
 *
 * <p>The factors are not round numbers because they are not a matter of taste: they come from the
 * proportions of the interface guide Nimbus takes its reference size from.
 *
 * <h2>The three painters</h2>
 *
 * <p>Background, foreground and border are asked for separately and any of them may be missing. That
 * they are {@link Painter} and not a {@link SynthPainter} is the central difference between Nimbus
 * and the rest of synth: a painter is handed the size on every call and draws a shape, instead of
 * stamping an image.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The table lookup is real and works: a color can be put under the matching key and this style
 * returns it. What is missing are Nimbus's concrete painters, which are the ninety private classes
 * of the package, so the three {@code getPainter} methods return whatever is in the table and
 * {@code null} when there is nothing.
 *
 * @since 1.7
 */
public final class NimbusStyle extends SynthStyle {

    /** The property a component asks to be large with. */
    public static final String LARGE_KEY = "large";

    /** The property it asks to be small with. */
    public static final String SMALL_KEY = "small";

    /** The property it asks to be mini with. */
    public static final String MINI_KEY = "mini";

    /** How much the font grows with {@link #LARGE_KEY}. */
    public static final double LARGE_SCALE = 1.15;

    /** How much it shrinks with {@link #SMALL_KEY}. */
    public static final double SMALL_SCALE = 0.857;

    /** How much it shrinks with {@link #MINI_KEY}. */
    public static final double MINI_SCALE = 0.714;

    /**
     * Applies the style to the component.
     *
     * @param ctx what is being installed
     */
    @Override
    public void installDefaults(SynthContext ctx) {
        super.installDefaults(ctx);
    }

    /**
     * The region's margins.
     *
     * @param ctx what is being drawn
     * @param insets where to write them, or {@code null} for a new one
     * @return the margins
     */
    @Override
    public Insets getInsets(SynthContext ctx, Insets insets) {
        final Object v = get(ctx, "contentMargins");
        if (!(v instanceof Insets)) {
            return super.getInsets(ctx, insets);
        }
        final Insets m = (Insets) v;
        if (insets == null) {
            return new Insets(m.top, m.left, m.bottom, m.right);
        }
        insets.top = m.top;
        insets.left = m.left;
        insets.bottom = m.bottom;
        insets.right = m.right;
        return insets;
    }

    /**
     * The color it gets in that state.
     *
     * @param ctx what is being drawn and in which state
     * @param type which color is asked for
     * @return the color, or {@code null}
     */
    @Override
    protected Color getColorForState(SynthContext ctx, ColorType type) {
        final Object v = get(ctx, nameOf(type));
        return v instanceof Color ? (Color) v : null;
    }

    /**
     * The font it gets in that state.
     *
     * <p>If the component asked for a size with one of the three properties, the font comes out
     * scaled by the matching factor.
     *
     * @param ctx what is being drawn and in which state
     * @return the font, or {@code null}
     */
    @Override
    protected Font getFontForState(SynthContext ctx) {
        final Object v = get(ctx, "font");
        if (!(v instanceof Font)) {
            return null;
        }
        final Font f = (Font) v;
        final double scale = scaleOf(ctx);
        return scale == 1.0 ? f : f.deriveFont((float) (f.getSize2D() * scale));
    }

    /**
     * Who draws this region.
     *
     * @param ctx what is being drawn
     * @return the painter
     */
    @Override
    public SynthPainter getPainter(SynthContext ctx) {
        return super.getPainter(ctx);
    }

    /**
     * Whether the region covers its whole rectangle.
     *
     * @param ctx what is being drawn
     * @return whatever the table says, or false
     */
    @Override
    public boolean isOpaque(SynthContext ctx) {
        final Object v = get(ctx, "opaque");
        return v instanceof Boolean ? ((Boolean) v).booleanValue() : false;
    }

    /**
     * Any value of the style.
     *
     * <p>The key is built from the region, the state and the name; see the class note. If the version
     * with the state is not there, the version without it is tried, which is how a value that holds
     * for every state is written.
     *
     * @param ctx what is being drawn
     * @param key the name
     * @return the value, or {@code null}
     */
    @Override
    public Object get(SynthContext ctx, Object key) {
        final String region = ctx.getRegion().getName();
        final String name = String.valueOf(key);
        final Object withState = UIManager.get(region + stateOf(ctx) + "." + name);
        return withState != null ? withState : UIManager.get(region + "." + name);
    }

    /**
     * The background painter.
     *
     * @param ctx what is being drawn
     * @return the painter, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public Painter<Object> getBackgroundPainter(SynthContext ctx) {
        final Object v = get(ctx, "backgroundPainter");
        return v instanceof Painter ? (Painter<Object>) v : null;
    }

    /**
     * The foreground painter.
     *
     * @param ctx what is being drawn
     * @return the painter, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public Painter<Object> getForegroundPainter(SynthContext ctx) {
        final Object v = get(ctx, "foregroundPainter");
        return v instanceof Painter ? (Painter<Object>) v : null;
    }

    /**
     * The border painter.
     *
     * @param ctx what is being drawn
     * @return the painter, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public Painter<Object> getBorderPainter(SynthContext ctx) {
        final Object v = get(ctx, "borderPainter");
        return v instanceof Painter ? (Painter<Object>) v : null;
    }

    /** The name a color type goes by in the table. */
    private static String nameOf(ColorType type) {
        if (type == ColorType.BACKGROUND) {
            return "background";
        }
        if (type == ColorType.FOREGROUND) {
            return "foreground";
        }
        if (type == ColorType.TEXT_BACKGROUND) {
            return "textBackground";
        }
        if (type == ColorType.TEXT_FOREGROUND) {
            return "textForeground";
        }
        if (type == ColorType.FOCUS) {
            return "focus";
        }
        return String.valueOf(type);
    }

    /**
     * The part of the key that names the state, like {@code "[Pressed]"}.
     *
     * <p>Empty for the normal state: in the table, what always holds is written without brackets.
     */
    private static String stateOf(SynthContext ctx) {
        final int s = ctx.getComponentState();
        if ((s & javax.swing.plaf.synth.SynthConstants.DISABLED) != 0) {
            return "[Disabled]";
        }
        if ((s & javax.swing.plaf.synth.SynthConstants.PRESSED) != 0) {
            return "[Pressed]";
        }
        if ((s & javax.swing.plaf.synth.SynthConstants.MOUSE_OVER) != 0) {
            return "[MouseOver]";
        }
        if ((s & javax.swing.plaf.synth.SynthConstants.FOCUSED) != 0) {
            return "[Focused]";
        }
        if ((s & javax.swing.plaf.synth.SynthConstants.SELECTED) != 0) {
            return "[Selected]";
        }
        return "";
    }

    /** What the font has to be multiplied by, according to what the component asked for. */
    private static double scaleOf(SynthContext ctx) {
        final Object size = ctx.getComponent().getClientProperty("JComponent.sizeVariant");
        if (LARGE_KEY.equals(size)) {
            return LARGE_SCALE;
        }
        if (SMALL_KEY.equals(size)) {
            return SMALL_SCALE;
        }
        if (MINI_KEY.equals(size)) {
            return MINI_SCALE;
        }
        return 1.0;
    }
}
