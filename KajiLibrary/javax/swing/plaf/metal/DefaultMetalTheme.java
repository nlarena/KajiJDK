package javax.swing.plaf.metal;

import java.awt.Font;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/**
 * The Steel theme: the Metal of always, blue and grey.
 *
 * <p>It is the shortest class that does something big. It gives the six colours
 * {@link MetalTheme} asks for and six typefaces, and with that a whole look and feel is defined.
 *
 * <p>The six colours are three shades of a violet blue and three greys, all multiples of
 * {@code 0x33}: {@code 102/153/204} and {@code 255}. That is neither chance nor taste -- it is
 * the safe 216-colour palette of 256-colour monitors --. Metal was born to look the same on any
 * machine, and those six values are the ones that survive a screen that cannot show more.
 *
 * <p>The typefaces are Dialog five times at 12 and one at 10. The only thing that tells them
 * apart is the bold: <strong>what is touched goes in bold</strong> --buttons, menus, window
 * title-- and what is read goes plain --the system's and the user's text--. The small 10 one is
 * for the secondary things, such as a menu item's accelerator.
 *
 * <p>The name is {@code "Steel"} and comes out through
 * {@code MetalLookAndFeel.getCurrentTheme().getName()}.
 */
public class DefaultMetalTheme extends MetalTheme {

    private static final ColorUIResource PRIMARY_1 = new ColorUIResource(102, 102, 153);
    private static final ColorUIResource PRIMARY_2 = new ColorUIResource(153, 153, 204);
    private static final ColorUIResource PRIMARY_3 = new ColorUIResource(204, 204, 255);

    private static final ColorUIResource SECONDARY_1 = new ColorUIResource(102, 102, 102);
    private static final ColorUIResource SECONDARY_2 = new ColorUIResource(153, 153, 153);
    private static final ColorUIResource SECONDARY_3 = new ColorUIResource(204, 204, 204);

    private static final FontUIResource CONTROL =
            new FontUIResource("Dialog", Font.BOLD, 12);
    private static final FontUIResource SYSTEM =
            new FontUIResource("Dialog", Font.PLAIN, 12);
    private static final FontUIResource USER =
            new FontUIResource("Dialog", Font.PLAIN, 12);
    private static final FontUIResource MENU =
            new FontUIResource("Dialog", Font.BOLD, 12);
    private static final FontUIResource TITLE =
            new FontUIResource("Dialog", Font.BOLD, 12);
    private static final FontUIResource SMALL =
            new FontUIResource("Dialog", Font.PLAIN, 10);

    public DefaultMetalTheme() {
    }

    public String getName() {
        return "Steel";
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

    /** Dialog bold 12: what is touched. */
    public FontUIResource getControlTextFont() {
        return CONTROL;
    }

    /** Dialog plain 12: what is read. */
    public FontUIResource getSystemTextFont() {
        return SYSTEM;
    }

    public FontUIResource getUserTextFont() {
        return USER;
    }

    public FontUIResource getMenuTextFont() {
        return MENU;
    }

    public FontUIResource getWindowTitleFont() {
        return TITLE;
    }

    /** Dialog plain 10: the secondary things. */
    public FontUIResource getSubTextFont() {
        return SMALL;
    }
}
