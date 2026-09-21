package javax.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;

import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.StrokeBorder;
import javax.swing.border.TitledBorder;

/**
 * It makes borders, and shares those that have no state.
 *
 * <h2>What it exists for if the constructors are public</h2>
 *
 * <p>For the sharing. A border does not keep which component it belongs to -- it is passed the
 * component on each drawing --, so two buttons with the same border may use the <em>same
 * object</em>. Those that carry no parameter are built once and always returned, and on a
 * screen with two hundred components that is two hundred objects that are not created.
 *
 * <p>Those that do carry parameters are created each time, because there is nothing to share.
 * Nothing forces one to go through here: the public constructors are still there and do the
 * same, only without sharing.
 *
 * <h2>A shared border cannot be touched</h2>
 *
 * <p>It is the other side and it is worth keeping in mind: the one
 * {@link #createEtchedBorder} returns is being used by others. This library's borders are
 * immutable, so there is no way of going wrong; the care is for whoever writes one of their
 * own.
 */
public class BorderFactory {

    private BorderFactory() {
    }

    static final Border sharedRaisedBevel = new BevelBorder(BevelBorder.RAISED);
    static final Border sharedLoweredBevel = new BevelBorder(BevelBorder.LOWERED);
    static final Border sharedEtchedBorder = new EtchedBorder();
    static final Border emptyBorder = new EmptyBorder(0, 0, 0, 0);

    /** A one-pixel line of that colour. */
    public static Border createLineBorder(Color color) {
        return new LineBorder(color, 1);
    }

    /** A line of that thickness. */
    public static Border createLineBorder(Color color, int thickness) {
        return new LineBorder(color, thickness);
    }

    /** A line, with the corners rounded or not. */
    public static Border createLineBorder(Color color, int thickness, boolean rounded) {
        return new LineBorder(color, thickness, rounded);
    }

    /** The relief that sticks out; shared. */
    public static Border createRaisedBevelBorder() {
        return createSharedBevel(BevelBorder.RAISED);
    }

    /** The relief that sinks; shared. */
    public static Border createLoweredBevelBorder() {
        return createSharedBevel(BevelBorder.LOWERED);
    }

    /**
     * The relief of that type.
     *
     * @throws IllegalArgumentException if the type is neither RAISED nor LOWERED.
     */
    public static Border createBevelBorder(int type) {
        return createSharedBevel(type);
    }

    /** The relief with those two colours. */
    public static Border createBevelBorder(int type, Color highlight, Color shadow) {
        return new BevelBorder(type, highlight, shadow);
    }

    /** The relief with the four colours set by hand. */
    public static Border createBevelBorder(int type, Color highlightOuter, Color highlightInner,
            Color shadowOuter, Color shadowInner) {
        return new BevelBorder(type, highlightOuter, highlightInner, shadowOuter, shadowInner);
    }

    /** The shared one if the type is one of the two; if not, a new one. */
    static Border createSharedBevel(int type) {
        if (type == BevelBorder.RAISED) {
            return sharedRaisedBevel;
        } else if (type == BevelBorder.LOWERED) {
            return sharedLoweredBevel;
        }
        return null;
    }

    /** The soft relief that sticks out. */
    public static Border createRaisedSoftBevelBorder() {
        return new SoftBevelBorder(BevelBorder.RAISED);
    }

    /** The soft relief that sinks. */
    public static Border createLoweredSoftBevelBorder() {
        return new SoftBevelBorder(BevelBorder.LOWERED);
    }

    /** The soft relief of that type; null if the type is neither of the two. */
    public static Border createSoftBevelBorder(int type) {
        if (type == BevelBorder.RAISED) {
            return createRaisedSoftBevelBorder();
        } else if (type == BevelBorder.LOWERED) {
            return createLoweredSoftBevelBorder();
        }
        return null;
    }

    public static Border createSoftBevelBorder(int type, Color highlight, Color shadow) {
        return new SoftBevelBorder(type, highlight, shadow);
    }

    public static Border createSoftBevelBorder(int type, Color highlightOuter,
            Color highlightInner, Color shadowOuter, Color shadowInner) {
        return new SoftBevelBorder(type, highlightOuter, highlightInner, shadowOuter,
                shadowInner);
    }

    /** The sunken groove; shared. */
    public static Border createEtchedBorder() {
        return sharedEtchedBorder;
    }

    /** The groove with those two colours. */
    public static Border createEtchedBorder(Color highlight, Color shadow) {
        return new EtchedBorder(highlight, shadow);
    }

    /** The groove of that type; the sunken one is the shared one. */
    public static Border createEtchedBorder(int type) {
        if (type == EtchedBorder.LOWERED) {
            return sharedEtchedBorder;
        }
        return new EtchedBorder(type);
    }

    public static Border createEtchedBorder(int type, Color highlight, Color shadow) {
        return new EtchedBorder(type, highlight, shadow);
    }

    /** A title, with no border around it. */
    public static TitledBorder createTitledBorder(String title) {
        return new TitledBorder(title);
    }

    /** That border with room for a title. */
    public static TitledBorder createTitledBorder(Border border) {
        return new TitledBorder(border);
    }

    /** That border with that title. */
    public static TitledBorder createTitledBorder(Border border, String title) {
        return new TitledBorder(border, title);
    }

    /** With the title set on that side. */
    public static TitledBorder createTitledBorder(Border border, String title,
            int titleJustification, int titlePosition) {
        return new TitledBorder(border, title, titleJustification, titlePosition);
    }

    /** And with that typeface. */
    public static TitledBorder createTitledBorder(Border border, String title,
            int titleJustification, int titlePosition, Font titleFont) {
        return new TitledBorder(border, title, titleJustification, titlePosition, titleFont);
    }

    /** And with that colour. */
    public static TitledBorder createTitledBorder(Border border, String title,
            int titleJustification, int titlePosition, Font titleFont, Color titleColor) {
        return new TitledBorder(border, title, titleJustification, titlePosition, titleFont,
                titleColor);
    }

    /**
     * A border that is not seen and takes up nothing; shared.
     *
     * <p>It serves to take a component's border away without leaving it null: null means "the one
     * the look and feel sets", and this means "none".
     */
    public static Border createEmptyBorder() {
        return emptyBorder;
    }

    /**
     * An invisible border that takes up that room all the same; it is how a margin is set with no
     * layout.
     */
    public static Border createEmptyBorder(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    /** Two empty borders, one inside the other. */
    public static CompoundBorder createCompoundBorder() {
        return new CompoundBorder();
    }

    /** One inside the other; the outer one is drawn first. */
    public static CompoundBorder createCompoundBorder(Border outsideBorder,
            Border insideBorder) {
        return new CompoundBorder(outsideBorder, insideBorder);
    }

    /** A frame of that colour with those four thicknesses. */
    public static MatteBorder createMatteBorder(int top, int left, int bottom, int right,
            Color color) {
        return new MatteBorder(top, left, bottom, right, color);
    }

    /** A frame made by repeating that icon. */
    public static MatteBorder createMatteBorder(int top, int left, int bottom, int right,
            Icon tileIcon) {
        return new MatteBorder(top, left, bottom, right, tileIcon);
    }

    /**
     * A border drawn with that stroke, in the component's colour.
     *
     * @throws NullPointerException if the stroke is null
     */
    public static Border createStrokeBorder(BasicStroke stroke) {
        return new StrokeBorder(stroke);
    }

    /**
     * A border drawn with that stroke and that paint.
     *
     * @throws NullPointerException if the stroke is null
     */
    public static Border createStrokeBorder(BasicStroke stroke, Paint paint) {
        return new StrokeBorder(stroke, paint);
    }

    /**
     * A line of dashes of that paint, with the usual measurements.
     *
     * @throws NullPointerException if the paint is null
     */
    public static Border createDashedBorder(Paint paint) {
        return createDashedBorder(paint, 1.0f, 1.0f, 1.0f, false);
    }

    /**
     * Dashes of that length and with that gap.
     *
     * @throws NullPointerException if the paint is null
     * @throws IllegalArgumentException if some measurement is not positive
     */
    public static Border createDashedBorder(Paint paint, float length, float spacing) {
        return createDashedBorder(paint, 1.0f, length, spacing, false);
    }

    /**
     * The complete dashed border.
     *
     * <p>With {@code rounded} at true the dashes carry rounded ends and corners; the length and
     * the gap are measured in multiples of the thickness, not in pixels, so that a thicker border
     * carries proportionally longer dashes.
     *
     * @throws NullPointerException if the paint is null
     * @throws IllegalArgumentException if some measurement is not positive
     */
    public static Border createDashedBorder(Paint paint, float thickness, float length,
            float spacing, boolean rounded) {
        boolean shared = !rounded && thickness == 1.0f && length == 1.0f && spacing == 1.0f;
        if (shared && paint == null) {
            // With no paint and with the usual measurements there is nothing that tells this border
                        // from another one the same, so it could be shared. It is not shared: the
                        // JDK builds a new one, and returning the same object would change an
                        // identity comparison somebody may be making.
            return new StrokeBorder(trazo(thickness, length, spacing, rounded), null);
        }
        return new StrokeBorder(trazo(thickness, length, spacing, rounded), paint);
    }

    /** The dotted stroke that corresponds to those measurements. */
    private static BasicStroke trazo(float thickness, float length, float spacing,
            boolean rounded) {
        int cap = rounded ? BasicStroke.CAP_ROUND : BasicStroke.CAP_SQUARE;
        int join = rounded ? BasicStroke.JOIN_ROUND : BasicStroke.JOIN_MITER;
        float[] array = {thickness * length, thickness * spacing};
        return new BasicStroke(thickness, cap, join, thickness * 2.0f, array, 0.0f);
    }
}
