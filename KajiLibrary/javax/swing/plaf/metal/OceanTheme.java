package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;

/**
 * The Ocean theme: Metal's default since Java 5.
 *
 * <h2>What changed, and why</h2>
 *
 * <p>Steel was designed for 256-colour monitors and it shows: six shades of the safe palette,
 * all multiples of {@code 0x33}. Ocean takes true colour for granted and chooses what looks
 * better and not what survives a poor screen: light blues instead of violet blue, grey
 * {@code 238} instead of {@code 204}, and text {@code (51,51,51)} instead of pure black, which
 * by then was known to tire less.
 *
 * <p>What is interesting is how much of that fits in six colours. Almost everything: Ocean
 * redefines the six and inherits the rest from {@link MetalTheme}. It also redefines five
 * derived ones, and each has its reason:
 *
 * <ul>
 *   <li>{@link #getBlack} becomes {@code (51,51,51)}, and with that all the text shifts at once.
 *   <li>{@link #getDesktopColor} becomes white: Ocean's {@code primary2} is a light blue and a
 *       desktop of that colour would not let anything resting on it be seen.
 *   <li>{@link #getInactiveControlTextColor} and {@link #getMenuDisabledForeground} go back to
 *       grey {@code 153}: deriving them from Ocean's {@code secondary2} would give light blue,
 *       and disabled text has to look disabled, not of another colour.
 *   <li>{@link #getControlTextColor} stays fixed at {@code (51,51,51)}.
 * </ul>
 *
 * <h2>The gradients</h2>
 *
 * <p>Ocean is the first theme that paints buttons with a gradient, and it does it with a list of
 * five things: two fractions and three colours. The fractions say where the cuts are -- the
 * {@code 0.3} is where the first stretch ends and the {@code 0.0} how long the flat stretch in
 * the middle is -- and the three colours are the top one, the middle one and the bottom one. The
 * list is of {@code Object} and in that order because that is how {@code MetalUtils} reads it,
 * and the format became public in fact: a theme of one's own that wants gradients has to build
 * it the same.
 *
 * <h2>What is said and not covered up</h2>
 *
 * <p>Of the 67 entries the JDK's theme adds, this one writes 52. The fifteen missing are
 * <strong>GIF images</strong> the JDK loads from the jar -- the four dialog icons, those of
 * folder and file, those of the tree, the handles --. There is nowhere to get them from and a
 * figure drawn by hand would not be the same image; they are left unset, which is the same thing
 * that happens to a look and feel that is missing a resource. The five title bar icons are there:
 * those the JDK draws too.
 */
public class OceanTheme extends DefaultMetalTheme {

    private static final ColorUIResource PRIMARY_1 = new ColorUIResource(99, 130, 191);
    private static final ColorUIResource PRIMARY_2 = new ColorUIResource(163, 184, 204);
    private static final ColorUIResource PRIMARY_3 = new ColorUIResource(184, 207, 229);

    private static final ColorUIResource SECONDARY_1 = new ColorUIResource(122, 138, 153);
    private static final ColorUIResource SECONDARY_2 = new ColorUIResource(184, 207, 229);
    private static final ColorUIResource SECONDARY_3 = new ColorUIResource(238, 238, 238);

    private static final ColorUIResource BLACK = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource WHITE = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource DIM_GRAY = new ColorUIResource(153, 153, 153);

    public OceanTheme() {
    }

    public String getName() {
        return "Ocean";
    }

    protected ColorUIResource getPrimary1() {
        return PRIMARY_1;
    }

    protected ColorUIResource getPrimary2() {
        return PRIMARY_2;
    }

    protected ColorUIResource getPrimary3() {
        return PRIMARY_3;
    }

    protected ColorUIResource getSecondary1() {
        return SECONDARY_1;
    }

    protected ColorUIResource getSecondary2() {
        return SECONDARY_2;
    }

    protected ColorUIResource getSecondary3() {
        return SECONDARY_3;
    }

    /** Very dark grey, not black; see the class note. */
    protected ColorUIResource getBlack() {
        return BLACK;
    }

    /** White: the theme's light blue would not serve as a desktop background. */
    public ColorUIResource getDesktopColor() {
        return WHITE;
    }

    public ColorUIResource getControlTextColor() {
        return BLACK;
    }

    /** Grey, not light blue: what is disabled has to look disabled. */
    public ColorUIResource getInactiveControlTextColor() {
        return DIM_GRAY;
    }

    public ColorUIResource getMenuDisabledForeground() {
        return DIM_GRAY;
    }

    /** The gradient of almost every control. */
    private static java.util.List<Object> controlGradient() {
        return Arrays.asList(new Object[] {
                Float.valueOf(0.3f), Float.valueOf(0.0f),
                new ColorUIResource(221, 232, 243),
                WHITE,
                new ColorUIResource(184, 207, 229) });
    }

    /** The slider's, which has a flat stretch in the middle. */
    private static java.util.List<Object> sliderGradient() {
        return Arrays.asList(new Object[] {
                Float.valueOf(0.3f), Float.valueOf(0.2f),
                new ColorUIResource(200, 221, 242),
                WHITE,
                new ColorUIResource(184, 207, 229) });
    }

    /** The menu bar's, which goes from white to grey in one go. */
    private static java.util.List<Object> menuGradient() {
        return Arrays.asList(new Object[] {
                Float.valueOf(1.0f), Float.valueOf(0.0f),
                WHITE,
                new ColorUIResource(218, 218, 218),
                new ColorUIResource(218, 218, 218) });
    }

    /** Ocean's fifty-two own values; see the class note. */
    public void addCustomEntriesToTable(UIDefaults table) {
        if (table == null) {
            return;
        }
        ColorUIResource blue = PRIMARY_1;
        ColorUIResource lightBlue = new ColorUIResource(200, 221, 242);
        ColorUIResource dropBackground = new ColorUIResource(210, 233, 255);
        ColorUIResource gray204 = new ColorUIResource(204, 204, 204);
        ColorUIResource gray218 = new ColorUIResource(218, 218, 218);
        Object focusBorder = new BorderUIResource.LineBorderUIResource(blue);

        Object[] pairs = {
            "Button.gradient", controlGradient(),
            "Button.rollover", Boolean.TRUE,
            "Button.rolloverIconType", "ocean",
            "Button.toolBarBorderBackground", DIM_GRAY,
            "Button.disabledToolBarBorderBackground", gray204,

            "CheckBox.gradient", controlGradient(),
            "CheckBox.rollover", Boolean.TRUE,
            "CheckBoxMenuItem.gradient", controlGradient(),
            "RadioButton.gradient", controlGradient(),
            "RadioButton.rollover", Boolean.TRUE,
            "RadioButtonMenuItem.gradient", controlGradient(),
            "ToggleButton.gradient", controlGradient(),
            "ScrollBar.gradient", controlGradient(),

            "InternalFrame.activeTitleGradient", controlGradient(),
            "InternalFrame.closeIcon", new TitleIcon(16, TitleIcon.CLOSE),
            "InternalFrame.iconifyIcon", new TitleIcon(16, TitleIcon.ICONIFY),
            "InternalFrame.maximizeIcon", new TitleIcon(16, TitleIcon.MAXIMIZE),
            "InternalFrame.minimizeIcon", new TitleIcon(16, TitleIcon.RESTORE),
            "InternalFrame.paletteCloseIcon", new TitleIcon(7, TitleIcon.CLOSE),

            "Label.disabledForeground", DIM_GRAY,

            "List.focusCellHighlightBorder", focusBorder,
            "List.dropLineColor", blue,
            "List.dropCellBackground", dropBackground,

            "Menu.opaque", Boolean.FALSE,
            "MenuBar.gradient", menuGradient(),
            "MenuBar.borderColor", gray204,

            "Slider.altTrackColor", new ColorUIResource(210, 226, 239),
            "Slider.gradient", sliderGradient(),
            "Slider.focusGradient", sliderGradient(),

            "SplitPane.oneTouchButtonsOpaque", Boolean.FALSE,
            "SplitPane.dividerFocusColor", lightBlue,

            "TabbedPane.borderHightlightColor", blue,
            "TabbedPane.contentAreaColor", lightBlue,
            "TabbedPane.contentBorderInsets", new Insets(4, 2, 3, 3),
            "TabbedPane.selected", lightBlue,
            "TabbedPane.tabAreaBackground", gray218,
            "TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6),
            "TabbedPane.unselectedBackground", SECONDARY_3,

            "Table.focusCellHighlightBorder", focusBorder,
            "Table.gridColor", SECONDARY_1,
            "Table.dropLineColor", blue,
            "Table.dropLineShortColor", BLACK,
            "Table.dropCellBackground", dropBackground,
            "TableHeader.focusCellBackground", lightBlue,

            "ToolBar.borderColor", gray204,
            "ToolBar.isRollover", Boolean.TRUE,

            "Tree.dropLineColor", blue,
            "Tree.dropCellBackground", dropBackground,
            "Tree.selectionBorderColor", blue,
        };
        table.putDefaults(pairs);
    }

    /**
     * The buttons of an internal frame's title bar.
     *
     * <p>The JDK draws them instead of loading them, and that is why these are there. They are
     * sixteen-pixel squares -- except a palette's close one, which is seven -- and each draws its
     * figure in the theme's text colour.
     */
    private static class TitleIcon implements Icon, UIResource {

        static final int CLOSE = 0;
        static final int ICONIFY = 1;
        static final int MAXIMIZE = 2;
        static final int RESTORE = 3;

        private final int side;
        private final int which;

        TitleIcon(int side, int which) {
            this.side = side;
            this.which = which;
        }

        public int getIconWidth() {
            return side;
        }

        public int getIconHeight() {
            return side;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color before = g.getColor();
            g.setColor(BLACK);
            int m = side / 4;
            int f = side - 1 - m;
            switch (which) {
                case CLOSE:
                    g.drawLine(x + m, y + m, x + f, y + f);
                    g.drawLine(x + m + 1, y + m, x + f, y + f - 1);
                    g.drawLine(x + f, y + m, x + m, y + f);
                    g.drawLine(x + f - 1, y + m, x + m, y + f - 1);
                    break;
                case ICONIFY:
                    // A line at the bottom, which is what is left of an iconified window.
                    g.fillRect(x + m, y + f - 1, f - m + 1, 2);
                    break;
                case MAXIMIZE:
                    g.drawRect(x + m, y + m, f - m, f - m);
                    g.drawLine(x + m, y + m + 1, x + f, y + m + 1);
                    break;
                case RESTORE:
                    // Two shifted frames: the window goes back to its previous size.
                    g.drawRect(x + m, y + m + 2, f - m - 2, f - m - 2);
                    g.drawRect(x + m + 2, y + m, f - m - 2, f - m - 2);
                    break;
                default:
                    break;
            }
            g.setColor(before);
        }
    }
}
