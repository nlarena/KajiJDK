package javax.swing.plaf.synth;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.Icon;

/**
 * How a region looks: its colours, its typeface, its margins and who draws it.
 *
 * <h2>One style per region, not per component</h2>
 *
 * <p>A scroll bar's thumb has its own style, different from the track's and from the whole bar's.
 * That is what allows writing a look and feel as a table of parts instead of as one class per
 * component.
 *
 * <h2>Why almost everything takes a {@link SynthContext}</h2>
 *
 * <p>Because the answer depends on the state: a pressed button's colour is not the same as a
 * disabled one's. The context carries the component, the region and the state, and without all
 * three there is no possible answer.
 *
 * <h2>The two abstract ones</h2>
 *
 * <p>{@link #getColorForState} and {@link #getFontForState} are the only thing a subclass is
 * forced to write. The rest has a reasonable default answer, and that asymmetry is deliberate: a
 * style has to say what colour it is, and may have no opinion about the rest.
 *
 * <p>{@link #getColor} wraps the first one and adds what does not depend on the subclass: if the
 * component is disabled and there is no colour of its own for that state, the normal state's is
 * used. That is why what is public is not abstract and what is abstract is not public.
 *
 * @since 1.5
 */
public abstract class SynthStyle {

    /**
     * The painter that draws nothing.
     *
     * <p>It has a name and is not anonymous because it goes in a static initializer, and there our
     * javac does not yet generate the synthetic class an anonymous one needs.
     */
    private static final class EmptyPainter extends SynthPainter {
    }

    private static final SynthGraphicsUtils UTILS = new SynthGraphicsUtils();
    private static final SynthPainter PAINTER = new EmptyPainter();

    /** One. */
    public SynthStyle() {
    }

    /**
     * Who does the drawing computations.
     *
     * @param context what is being drawn
     * @return the utilities; never {@code null}
     */
    public SynthGraphicsUtils getGraphicsUtils(SynthContext context) {
        return UTILS;
    }

    /**
     * The colour it gets.
     *
     * @param context what is being drawn and in what state
     * @param type which colour is asked for
     * @return the colour, or {@code null}
     */
    public Color getColor(SynthContext context, ColorType type) {
        return getColorForState(context, type);
    }

    /**
     * The colour it gets in that state.
     *
     * @param context what is being drawn and in what state
     * @param type which colour is asked for
     * @return the colour, or {@code null}
     */
    protected abstract Color getColorForState(SynthContext context, ColorType type);

    /**
     * The typeface it gets.
     *
     * @param context what is being drawn and in what state
     * @return the typeface, or {@code null}
     */
    public Font getFont(SynthContext context) {
        return getFontForState(context);
    }

    /**
     * The typeface it gets in that state.
     *
     * @param context what is being drawn and in what state
     * @return the typeface, or {@code null}
     */
    protected abstract Font getFontForState(SynthContext context);

    /**
     * The region's margins.
     *
     * <p>The object passed in is reused if it is not {@code null}. It is the way of not creating an
     * object per query in something that is queried on every repaint.
     *
     * @param context what is being drawn
     * @param insets where to write them, or {@code null} for a new one
     * @return the margins
     */
    public Insets getInsets(SynthContext context, Insets insets) {
        if (insets == null) {
            return new Insets(0, 0, 0, 0);
        }
        insets.top = 0;
        insets.bottom = 0;
        insets.left = 0;
        insets.right = 0;
        return insets;
    }

    /**
     * Who draws this region.
     *
     * @param context what is being drawn
     * @return the painter; never {@code null}
     */
    public SynthPainter getPainter(SynthContext context) {
        return PAINTER;
    }

    /**
     * Whether the region covers its whole rectangle.
     *
     * <p>Saying yes and not doing it leaves rubbish on the screen, because nobody bothers to clear
     * underneath. That is why the reasonable default is yes: a region that does not cover
     * everything says so.
     *
     * @param context what is being drawn
     * @return true if it covers everything
     */
    public boolean isOpaque(SynthContext context) {
        return true;
    }

    /**
     * Any value of the style, by name.
     *
     * <p>It is the back door: whatever a look and feel wants to keep and does not fit in a colour,
     * a typeface or a margin.
     *
     * @param context what is being drawn
     * @param key the name
     * @return the value, or {@code null}
     */
    public Object get(SynthContext context, Object key) {
        return null;
    }

    /**
     * It applies the style to the component.
     *
     * <p>It sets the colour, the background, the typeface and the opacity. Only what the style has:
     * a null value is not installed, so as not to overwrite what the program set by hand.
     *
     * @param context what is being installed
     */
    public void installDefaults(SynthContext context) {
        final javax.swing.JComponent c = context.getComponent();
        final Color foreground = getColor(context, ColorType.FOREGROUND);
        if (foreground != null) {
            c.setForeground(foreground);
        }
        final Color background = getColor(context, ColorType.BACKGROUND);
        if (background != null) {
            c.setBackground(background);
        }
        final Font f = getFont(context);
        if (f != null) {
            c.setFont(f);
        }
        c.setOpaque(isOpaque(context));
    }

    /**
     * It undoes what it installed.
     *
     * <p>It does nothing by default, and it is not an oversight: what was installed with
     * {@link #installDefaults} are look and feel values, and the next look and feel will overwrite
     * them. A subclass that reserves something more --a listener, a timer-- releases it here.
     *
     * @param context what is being uninstalled
     */
    public void uninstallDefaults(SynthContext context) {
    }

    /**
     * An integer value of the style.
     *
     * @param context what is being drawn
     * @param key the name
     * @param defaultValue what to return if it is not there
     * @return the value
     */
    public int getInt(SynthContext context, Object key, int defaultValue) {
        final Object v = get(context, key);
        return v instanceof Number ? ((Number) v).intValue() : defaultValue;
    }

    /**
     * A truth value of the style.
     *
     * @param context what is being drawn
     * @param key the name
     * @param defaultValue what to return if it is not there
     * @return the value
     */
    public boolean getBoolean(SynthContext context, Object key, boolean defaultValue) {
        final Object v = get(context, key);
        return v instanceof Boolean ? ((Boolean) v).booleanValue() : defaultValue;
    }

    /**
     * An icon of the style.
     *
     * @param context what is being drawn
     * @param key the name
     * @return the icon, or {@code null}
     */
    public Icon getIcon(SynthContext context, Object key) {
        final Object v = get(context, key);
        return v instanceof Icon ? (Icon) v : null;
    }

    /**
     * A text of the style.
     *
     * @param context what is being drawn
     * @param key the name
     * @param defaultValue what to return if it is not there
     * @return the text
     */
    public String getString(SynthContext context, Object key, String defaultValue) {
        final Object v = get(context, key);
        return v instanceof String ? (String) v : defaultValue;
    }
}
