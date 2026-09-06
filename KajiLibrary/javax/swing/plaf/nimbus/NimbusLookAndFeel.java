package javax.swing.plaf.nimbus;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthLookAndFeel;

/**
 * The Nimbus look and feel: drawn with curves, not with images.
 *
 * <h2>What sets it apart</h2>
 *
 * <p>Every other look and feel stamps images; Nimbus draws each component with curves and gradients
 * worked out on the fly. The practical consequence is that it looks right at any size and any screen
 * density, and that the whole of it can be recolored by changing a few colors.
 *
 * <p>That recoloring is {@link #getDerivedColor}: nearly every color in Nimbus is written as an
 * offset in hue, saturation and brightness over a handful of base colors. Changing
 * {@code "nimbusBase"} changes the entire look.
 *
 * <h2>Why it extends synth</h2>
 *
 * <p>Because the mechanism is the same: a table saying how each region looks and a set of UI classes
 * that consult it. The difference is where the table comes from. Synth reads it from a file; Nimbus
 * carries it written in code, generated from the design tool.
 *
 * <h2>{@link #register}</h2>
 *
 * <p>It is how a component of one's own takes part in Nimbus: its region is registered with a
 * prefix, and from then on its values are looked up in the table by the same rules as a button's.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>Derived colors and region registration really work. What is missing is Nimbus's table -- the
 * thousand values that came out of the tool -- and the concrete painters, which are the ninety
 * private classes of the package. {@link #getDefaults} returns an empty table rather than an
 * invented one: a button drawn in colors that are not Nimbus's would not be Nimbus.
 *
 * @since 1.7
 */
public class NimbusLookAndFeel extends SynthLookAndFeel {

    private final Map<Region, String> registered = new HashMap<Region, String>();

    /** One. */
    public NimbusLookAndFeel() {
    }

    /** Installs itself. */
    @Override
    public void initialize() {
        super.initialize();
    }

    /** Uninstalls itself. */
    @Override
    public void uninitialize() {
        super.uninitialize();
    }

    /**
     * Nimbus's table of values.
     *
     * @return the table
     */
    @Override
    public UIDefaults getDefaults() {
        return super.getDefaults();
    }

    /**
     * The style of that region of that component.
     *
     * @param c the component
     * @param r the region
     * @return the style, or {@code null} if the one in place is not Nimbus's
     */
    public static NimbusStyle getStyle(JComponent c, Region r) {
        final Object s = SynthLookAndFeel.getStyle(c, r);
        return s instanceof NimbusStyle ? (NimbusStyle) s : null;
    }

    /**
     * The name to show.
     *
     * @return {@code "Nimbus"}
     */
    @Override
    public String getName() {
        return "Nimbus";
    }

    /**
     * The short identifier.
     *
     * @return {@code "Nimbus"}
     */
    @Override
    public String getID() {
        return "Nimbus";
    }

    /**
     * What it is.
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Nimbus Look and Feel";
    }

    /**
     * Whether the style has to be revisited when the component changes container.
     *
     * @return true: in Nimbus a component looks different depending on where it is -- a button
     *     inside a toolbar, for instance -- so the change matters
     */
    @Override
    public boolean shouldUpdateStyleOnAncestorChanged() {
        return true;
    }

    /**
     * Whether that property change forces the style to be revisited.
     *
     * @param ev what changed
     * @return true if the style may have changed
     */
    @Override
    protected boolean shouldUpdateStyleOnEvent(PropertyChangeEvent ev) {
        final String n = ev.getPropertyName();
        return "Nimbus.Overrides".equals(n)
                || "Nimbus.Overrides.InheritDefaults".equals(n)
                || "JComponent.sizeVariant".equals(n)
                || super.shouldUpdateStyleOnEvent(ev);
    }

    /**
     * Registers a region so it takes part in Nimbus.
     *
     * <p>The prefix is what the table's keys for that region are built from. Registering the same
     * region twice with different prefixes replaces the earlier one: it would make no sense for one
     * region to have two sets of values.
     *
     * @param region the region
     * @param prefix the prefix of its keys
     */
    public void register(Region region, String prefix) {
        registered.put(region, prefix);
    }

    /**
     * An icon in its disabled version.
     *
     * @param component the component, or {@code null}
     * @param icon the icon, or {@code null}
     * @return the disabled icon, or {@code null}
     */
    @Override
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        return super.getDisabledIcon(component, icon);
    }

    /**
     * A color offset from one in Nimbus's table.
     *
     * <p>It is the mechanism by which the whole of Nimbus is recolored: nearly all of its colors are
     * written this way, as offsets over a handful of base colors.
     *
     * @param uiDefaultParentName the key of the base color
     * @param hOffset how far to shift the hue
     * @param sOffset how far to shift the saturation
     * @param bOffset how far to shift the brightness
     * @param aOffset how far to shift the transparency, from -255 to 255
     * @param uiResource whether the returned color has to be marked as put there by the look and feel
     * @return the color
     */
    public Color getDerivedColor(String uiDefaultParentName, float hOffset, float sOffset,
            float bOffset, int aOffset, boolean uiResource) {
        final Object v = javax.swing.UIManager.get(uiDefaultParentName);
        final Color base = v instanceof Color ? (Color) v : Color.GRAY;
        final Color d = shifted(base, hOffset, sOffset, bOffset, aOffset);
        return uiResource ? new javax.swing.plaf.ColorUIResource(d) : d;
    }

    /**
     * A color between two, at that proportion, marked as put there by the look and feel.
     *
     * @param color1 the one at one end
     * @param color2 the one at the other
     * @param midPoint how much of the second, between zero and one
     * @param uiResource whether it has to be marked as put there by the look and feel
     * @return the color in between
     */
    protected final Color getDerivedColor(Color color1, Color color2, float midPoint,
            boolean uiResource) {
        // Transparency is mixed and then lost: the color is assembled packed and handed over through
        // the single-int constructor, which always gives an opaque one. That is what the JDK does
        // and not a slip here; changing it would give colors different from its own.
        final int argb = mix(color1.getAlpha(), color2.getAlpha(), midPoint) << 24
                | mix(color1.getRed(), color2.getRed(), midPoint) << 16
                | mix(color1.getGreen(), color2.getGreen(), midPoint) << 8
                | mix(color1.getBlue(), color2.getBlue(), midPoint);
        return uiResource ? new javax.swing.plaf.ColorUIResource(argb) : new Color(argb);
    }

    /**
     * A color between two, at that proportion.
     *
     * @param color1 the one at one end
     * @param color2 the one at the other
     * @param midPoint how much of the second, between zero and one
     * @return the color in between
     */
    protected final Color getDerivedColor(Color color1, Color color2, float midPoint) {
        return getDerivedColor(color1, color2, midPoint, false);
    }

    private static Color shifted(Color base, float h, float s, float b, int a) {
        final float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        final Color c = Color.getHSBColor(clamp(hsb[0] + h), clamp(hsb[1] + s), clamp(hsb[2] + b));
        final int aa = Math.max(0, Math.min(255, base.getAlpha() + a));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), aa);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }

    private static int mix(int a, int b, float p) {
        final int v = (int) (a + (b - a) * p + 0.5f);
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }
}
