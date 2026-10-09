package javax.swing.plaf.metal;

import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/**
 * Metal's colours and typefaces.
 *
 * <h2>Eight colours and nothing else</h2>
 *
 * <p>A theme defines eight colours -- three primary, three secondary, white and black -- and from
 * there comes <em>everything</em> else. This class's forty-odd public methods keep nothing: they
 * are names for combinations of those eight.
 *
 * <p>The division between primary and secondary is what makes a theme writable in twenty lines.
 * The <strong>secondary</strong> ones are the greys of the metal sheet: a button's background,
 * its shadow, its dark shadow. The <strong>primary</strong> ones are the colour with which that
 * theme marks what is chosen or has the focus -- blue in Steel, light blue in Ocean --. Within
 * each triple, 1 is the darkest and 3 the lightest, always, and on that depends that a button
 * drawn with {@code getControlDarkShadow} and {@code getControlHighlight} comes out with relief
 * and not sunken.
 *
 * <p>That is why the eight are {@code protected} and the rest are not: whoever writes a theme of
 * their own gives the eight, and inherits for free that the rest of the look and feel stays
 * coherent. Redefining a derived one is possible and is what {@link OceanTheme} does in five
 * cases where the derivation did not give what it wanted.
 *
 * <h2>Black is not black</h2>
 *
 * <p>{@link #getBlack} returns black in Steel and {@code (51,51,51)} in Ocean. It is the same
 * place in the structure -- the colour of the text and of the details -- and that is why it is
 * called that; that it is literally black is the theme's business, not the class's.
 *
 * <h2>What is said</h2>
 *
 * <p>{@link #addCustomEntriesToTable} adds nothing. A theme that only changes the eight colours
 * does not need to add entries; the one that wants gradients or icons of its own redefines the
 * method, and that is what {@link OceanTheme} does.
 */
public abstract class MetalTheme {

    private static final ColorUIResource WHITE = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource BLACK = new ColorUIResource(0, 0, 0);

    public MetalTheme() {
    }

    /**
     * What it is called; it is what comes out through {@code
     * MetalLookAndFeel.getCurrentTheme().getName()}.
     */
    public abstract String getName();

    // ---- the eight ----

    /** The darkest primary. */
    protected abstract ColorUIResource getPrimary1();

    /** The middle primary. */
    protected abstract ColorUIResource getPrimary2();

    /** The lightest primary. */
    protected abstract ColorUIResource getPrimary3();

    /** The darkest secondary. */
    protected abstract ColorUIResource getSecondary1();

    /** The middle secondary. */
    protected abstract ColorUIResource getSecondary2();

    /** The lightest secondary; it is the background of almost everything. */
    protected abstract ColorUIResource getSecondary3();

    /** The white; no JDK theme changes it. */
    protected ColorUIResource getWhite() {
        return WHITE;
    }

    /** The black, which is not always black; see the class note. */
    protected ColorUIResource getBlack() {
        return BLACK;
    }

    // ---- the typefaces ----

    public abstract FontUIResource getControlTextFont();

    public abstract FontUIResource getSystemTextFont();

    public abstract FontUIResource getUserTextFont();

    public abstract FontUIResource getMenuTextFont();

    public abstract FontUIResource getWindowTitleFont();

    public abstract FontUIResource getSubTextFont();

    // ---- the derived ones: the controls ----

    /** The background of a button, a panel, a bar. */
    public ColorUIResource getControl() {
        return getSecondary3();
    }

    public ColorUIResource getControlShadow() {
        return getSecondary2();
    }

    public ColorUIResource getControlDarkShadow() {
        return getSecondary1();
    }

    public ColorUIResource getControlHighlight() {
        return getWhite();
    }

    /** The colour of the lines a control draws: the arrow, the tick, the dot. */
    public ColorUIResource getControlInfo() {
        return getBlack();
    }

    public ColorUIResource getControlDisabled() {
        return getSecondary2();
    }

    // ---- the derived ones: what is chosen and what has the focus ----

    public ColorUIResource getPrimaryControl() {
        return getPrimary3();
    }

    public ColorUIResource getPrimaryControlShadow() {
        return getPrimary2();
    }

    public ColorUIResource getPrimaryControlDarkShadow() {
        return getPrimary1();
    }

    public ColorUIResource getPrimaryControlHighlight() {
        return getWhite();
    }

    public ColorUIResource getPrimaryControlInfo() {
        return getBlack();
    }

    // ---- the derived ones: the texts ----

    public ColorUIResource getSystemTextColor() {
        return getBlack();
    }

    public ColorUIResource getControlTextColor() {
        return getControlInfo();
    }

    public ColorUIResource getUserTextColor() {
        return getBlack();
    }

    public ColorUIResource getInactiveSystemTextColor() {
        return getSecondary2();
    }

    public ColorUIResource getInactiveControlTextColor() {
        return getControlDisabled();
    }

    public ColorUIResource getHighlightedTextColor() {
        return getControlTextColor();
    }

    public ColorUIResource getTextHighlightColor() {
        return getPrimaryControl();
    }

    // ---- the derived ones: the background ----

    public ColorUIResource getWindowBackground() {
        return getWhite();
    }

    public ColorUIResource getDesktopColor() {
        return getPrimary2();
    }

    public ColorUIResource getFocusColor() {
        return getPrimary2();
    }

    // ---- the derived ones: the menus ----

    public ColorUIResource getMenuBackground() {
        return getSecondary3();
    }

    public ColorUIResource getMenuForeground() {
        return getBlack();
    }

    public ColorUIResource getMenuSelectedBackground() {
        return getPrimary2();
    }

    public ColorUIResource getMenuSelectedForeground() {
        return getBlack();
    }

    public ColorUIResource getMenuDisabledForeground() {
        return getSecondary2();
    }

    public ColorUIResource getAcceleratorForeground() {
        return getPrimary1();
    }

    public ColorUIResource getAcceleratorSelectedForeground() {
        return getBlack();
    }

    // ---- the derived ones: separators and titles ----

    /** The separator is drawn with two lines, one light and one dark, and from there the relief. */
    public ColorUIResource getSeparatorBackground() {
        return getWhite();
    }

    public ColorUIResource getSeparatorForeground() {
        return getPrimary1();
    }

    public ColorUIResource getWindowTitleBackground() {
        return getPrimary3();
    }

    public ColorUIResource getWindowTitleForeground() {
        return getBlack();
    }

    public ColorUIResource getWindowTitleInactiveBackground() {
        return getSecondary3();
    }

    public ColorUIResource getWindowTitleInactiveForeground() {
        return getBlack();
    }

    /** Nothing; see the class note. */
    public void addCustomEntriesToTable(UIDefaults table) {
    }
}
