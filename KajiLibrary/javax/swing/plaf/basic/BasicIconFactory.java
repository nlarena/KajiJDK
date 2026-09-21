package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

/**
 * The icons the basic look and feel gives to check boxes, radio buttons and menus.
 *
 * <h2>Almost all of them are empty, and it is on purpose</h2>
 *
 * <p>Five of the eight draw nothing: they only take up room. The basic look and feel does not
 * have a drawing of its own for a check box nor for a menu arrow -- Metal, Windows and GTK have
 * one, and each its own --, but the <em>size</em> does have to be there, because the text's
 * indent and the alignment of a column of items come from it. A 13 x 13 icon that paints nothing
 * leaves the gap where the real look and feel is going to put its own.
 *
 * <p>The two that do draw are the menu ones with state: the tick of the tickable item and the
 * dot of the option item, and only when the item is chosen.
 *
 * <h2>The sizes, measured</h2>
 *
 * <p>Check box and radio button 13 x 13; menu tick and dot 9 x 9 and 6 x 6; the tick's gap
 * 9 x 9; the two arrows 4 x 8; an internal frame's empty icon 14 x 16. All measured in JDK 25.
 *
 * <h2>One object per type</h2>
 *
 * <p>Each method always returns the same instance. A stateless icon can be shared between every
 * component on the screen, and they are several hundred.
 */
public class BasicIconFactory implements Serializable {

    private static Icon frameIcon;
    private static Icon checkBoxIcon;
    private static Icon radioButtonIcon;
    private static Icon checkBoxMenuItemIcon;
    private static Icon radioButtonMenuItemIcon;
    private static Icon menuItemCheckIcon;
    private static Icon menuItemArrowIcon;
    private static Icon menuArrowIcon;

    public BasicIconFactory() {
    }

    /** A check box's: 13 x 13 and empty. */
    public static Icon getCheckBoxIcon() {
        if (checkBoxIcon == null) {
            checkBoxIcon = new CheckBoxIcon();
        }
        return checkBoxIcon;
    }

    /** An option radio button's: 13 x 13 and empty. */
    public static Icon getRadioButtonIcon() {
        if (radioButtonIcon == null) {
            radioButtonIcon = new RadioButtonIcon();
        }
        return radioButtonIcon;
    }

    /** A tickable menu item's tick: 9 x 9, it draws only if it is ticked. */
    public static Icon getCheckBoxMenuItemIcon() {
        if (checkBoxMenuItemIcon == null) {
            checkBoxMenuItemIcon = new CheckBoxMenuItemIcon();
        }
        return checkBoxMenuItemIcon;
    }

    /** An option menu item's dot: 6 x 6, it draws only if it is chosen. */
    public static Icon getRadioButtonMenuItemIcon() {
        if (radioButtonMenuItemIcon == null) {
            radioButtonMenuItemIcon = new RadioButtonMenuItemIcon();
        }
        return radioButtonMenuItemIcon;
    }

    /** The gap where an item's tick goes: 9 x 9 and empty. */
    public static Icon getMenuItemCheckIcon() {
        if (menuItemCheckIcon == null) {
            menuItemCheckIcon = new MenuItemCheckIcon();
        }
        return menuItemCheckIcon;
    }

    /** The gap of an item's submenu arrow: 4 x 8 and empty. */
    public static Icon getMenuItemArrowIcon() {
        if (menuItemArrowIcon == null) {
            menuItemArrowIcon = new MenuItemArrowIcon();
        }
        return menuItemArrowIcon;
    }

    /** The arrow that says a menu has a submenu: 4 x 8 and empty. */
    public static Icon getMenuArrowIcon() {
        if (menuArrowIcon == null) {
            menuArrowIcon = new MenuArrowIcon();
        }
        return menuArrowIcon;
    }

    /** The icon of an internal frame with no icon of its own: 14 x 16 and empty. */
    public static Icon createEmptyFrameIcon() {
        if (frameIcon == null) {
            frameIcon = new EmptyFrameIcon();
        }
        return frameIcon;
    }

    // These classes' names are the JDK's even though they are private: they show through
        // getClass() and the differential test compares them. See JTable's note on the same thing.

    private static class CheckBoxIcon implements Icon, Serializable {

        static final int csize = 13;

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return csize;
        }

        public int getIconHeight() {
            return csize;
        }
    }

    private static class RadioButtonIcon implements Icon, UIResource, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    }

    private static class CheckBoxMenuItemIcon implements Icon, UIResource, Serializable {

        /** The tick: two strokes, a short one going down and a long one going up. */
        public void drawCheck(Component c, Graphics g, int x, int y) {
            int w = getIconWidth();
            int h = getIconHeight();
            g.drawLine(x + 1, y + h / 2, x + w / 3, y + h - 2);
            g.drawLine(x + w / 3, y + h - 2, x + w - 2, y + 1);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            if (model.isSelected()) {
                drawCheck(c, g, x, y);
            }
        }

        public int getIconWidth() {
            return 9;
        }

        public int getIconHeight() {
            return 9;
        }
    }

    private static class RadioButtonMenuItemIcon implements Icon, UIResource, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            if (b.isSelected()) {
                g.fillOval(x, y, getIconWidth() - 1, getIconHeight() - 1);
            }
        }

        public int getIconWidth() {
            return 6;
        }

        public int getIconHeight() {
            return 6;
        }
    }

    private static class MenuItemCheckIcon implements Icon, UIResource, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 9;
        }

        public int getIconHeight() {
            return 9;
        }
    }

    private static class MenuItemArrowIcon implements Icon, UIResource, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 4;
        }

        public int getIconHeight() {
            return 8;
        }
    }

    private static class MenuArrowIcon implements Icon, UIResource, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 4;
        }

        public int getIconHeight() {
            return 8;
        }
    }

    private static class EmptyFrameIcon implements Icon, Serializable {

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 14;
        }

        public int getIconHeight() {
            return 16;
        }
    }
}
