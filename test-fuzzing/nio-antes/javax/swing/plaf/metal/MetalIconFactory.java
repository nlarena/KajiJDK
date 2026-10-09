package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

/**
 * The icons of the Metal look and feel; for now, the check box and the radio button.
 *
 * <h2>Icons measured, not drawn</h2>
 *
 * <p>In the JDK these icons are drawn with lines, arcs and a gradient ({@code Button.gradient})
 * over the theme's colours. Here they are pixel maps: each state of each icon is what JDK 25
 * painted, measured pixel by pixel in the Ocean theme. It is the same substitution as
 * {@code jdk.internal.awt.BitmapFont}: the exact result, instead of the procedure.
 *
 * <p>The maps use one letter per colour: {@code #} the dark shadow (122, 138, 153), {@code p}
 * the primary control (184, 207, 229), {@code o} the theme's black (51, 51, 51), {@code q} the
 * inactive grey (153, 153, 153), {@code g} the corresponding row of the gradient, and {@code .}
 * nothing. The gradient runs from top to bottom and depends on the height, so each icon has its
 * own table of rows.
 *
 * <p>The states come from the button's model, from the most specific to the least: disabled,
 * pressed and armed, with the cursor over it, and at rest; each one with and without selection.
 * A pressed icon lowers the check box's tick by one pixel, as the button's text lowers.
 *
 * <h2>The other twenty-four</h2>
 *
 * <p>The two above are measured maps. The rest -- the tree's, the internal frame's, the file
 * chooser's, the menus', the slider's -- are drawn: they have the <strong>exact size</strong> of
 * the JDK's, the same class for each one, and the same answer to
 * {@code instanceof UIResource}, which is what decides whether the look and feel may replace
 * them. What is not identical is the pixel: they are figures of their own in the theme's
 * colours.
 *
 * <p>The distinction matters and is documented on purpose. A wrong size shifts all the drawing
 * around it; a different stroke inside a 16 x 16 icon shifts nothing.
 *
 * <h2>Which are look and feel resources and which are not</h2>
 *
 * <p>Almost all are {@link UIResource}, and that means a change of look and feel replaces them.
 * The three of the tree that are not -- the handle, the folder and the leaf -- stay put, and it
 * is measured. The reason is that a tree the program set its own icons on should not lose them
 * on changing theme, and the JDK solves that by not marking them.
 *
 * <p>{@link #getMenuItemCheckIcon} returns {@code null}, and that is measured too: a Metal menu
 * item carries no tick of its own.
 */
public class MetalIconFactory implements Serializable {

    private static final Color DARK_SHADOW = new Color(122, 138, 153);
    private static final Color PRIMARY_CONTROL = new Color(184, 207, 229);
    private static final Color BLACK = new Color(51, 51, 51);
    private static final Color INACTIVE = new Color(153, 153, 153);

    private static Icon checkBoxIcon;
    private static Icon radioButtonIcon;

    public MetalIconFactory() {
    }

    /** The check box icon, shared. */
    public static Icon getCheckBoxIcon() {
        if (checkBoxIcon == null) {
            checkBoxIcon = new CheckBoxIcon();
        }
        return checkBoxIcon;
    }

    /** The radio button icon, shared. */
    public static Icon getRadioButtonIcon() {
        if (radioButtonIcon == null) {
            radioButtonIcon = new RadioButtonIcon();
        }
        return radioButtonIcon;
    }

    /** It paints a pixel map at that place; see the legend in the class note. */
    static void paintMap(Graphics g, int x, int y, String[] map, Color[] gradient) {
        for (int row = 0; row < map.length; row++) {
            String line = map[row];
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                Color color;
                if (c == '#') {
                    color = DARK_SHADOW;
                } else if (c == 'p') {
                    color = PRIMARY_CONTROL;
                } else if (c == 'o') {
                    color = BLACK;
                } else if (c == 'q') {
                    color = INACTIVE;
                } else if (c == 'g') {
                    color = gradient[row];
                } else {
                    continue;
                }
                g.setColor(color);
                g.fillRect(x + col, y + row, 1, 1);
            }
        }
    }

    /** Which map corresponds to the model's state; the order is the class note's. */
    static int state(ButtonModel m) {
        boolean sel = m.isSelected();
        if (!m.isEnabled()) {
            return sel ? 5 : 4;
        }
        if (m.isPressed() && m.isArmed()) {
            return sel ? 3 : 2;
        }
        if (m.isRollover()) {
            return sel ? 7 : 6;
        }
        return sel ? 1 : 0;
    }

    /** Ocean's check box: 13 by 13, measured. */
    private static class CheckBoxIcon implements Icon, UIResource, Serializable {

        private static final Color[] GRADIENT = {
            null, new Color(0xE8EFF6), new Color(0xF3F7FA), new Color(0xFFFFFF),
            new Color(0xF3F7FB), new Color(0xE8EFF7), new Color(0xDDE8F3), new Color(0xD7E4F1),
            new Color(0xD2E0EF), new Color(0xCDDDED), new Color(0xC7D9EB), new Color(0xC2D6E9),
            null };

        private static final String[] NORMAL = {
            "#############",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#############" };

        private static final String[] SELECTED = {
            "#############",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#ggggggggogg#",
            "#gggggggoogg#",
            "#ggooggooggg#",
            "#ggoogoogggg#",
            "#ggooooggggg#",
            "#ggooogggggg#",
            "#ggooggggggg#",
            "#ggggggggggg#",
            "#ggggggggggg#",
            "#############" };

        private static final String[] PRESSED = {
            "#############",
            "#############",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppppp#",
            "#############" };

        private static final String[] PRESSED_SELECTED = {
            "#############",
            "#############",
            "##pppppppppp#",
            "##pppppppppp#",
            "##pppppppopp#",
            "##ppppppoopp#",
            "##pooppooppp#",
            "##poopoopppp#",
            "##pooooppppp#",
            "##pooopppppp#",
            "##pooppppppp#",
            "##pppppppppp#",
            "#############" };

        private static final String[] DISABLED = {
            "#############",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#############" };

        private static final String[] DISABLED_SELECTED = {
            "#############",
            "#...........#",
            "#...........#",
            "#........#..#",
            "#.......##..#",
            "#..##..##...#",
            "#..##.##....#",
            "#..####.....#",
            "#..###......#",
            "#..##.......#",
            "#...........#",
            "#...........#",
            "#############" };

        private static final String[] ROLLOVER = {
            "#############",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppgggggggpp#",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#############" };

        private static final String[] ROLLOVER_SELECTED = {
            "#############",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#ppggggggopp#",
            "#ppgggggoopp#",
            "#ppooggoogpp#",
            "#ppoogooggpp#",
            "#ppoooogggpp#",
            "#ppoooggggpp#",
            "#ppoogggggpp#",
            "#ppppppppppp#",
            "#ppppppppppp#",
            "#############" };

        private static final String[][] MAPS = { NORMAL, SELECTED, PRESSED,
            PRESSED_SELECTED, DISABLED, DISABLED_SELECTED, ROLLOVER,
            ROLLOVER_SELECTED };

        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel model = ((AbstractButton) c).getModel();
            paintMap(g, x, y, MAPS[state(model)], GRADIENT);
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    }

    /** Ocean's radio button: 13 by 13, measured; the last row is left empty. */
    private static class RadioButtonIcon implements Icon, UIResource, Serializable {

        private static final Color[] GRADIENT = {
            null, new Color(0xDDE8F3), new Color(0xE8EFF6), new Color(0xF3F7FA),
            new Color(0xFFFFFF), new Color(0xF3F7FB), new Color(0xE8EFF7), new Color(0xDDE8F3),
            new Color(0xD3E1EF), new Color(0xCADBEC), new Color(0xC1D5E8), null, null };

        private static final String[] NORMAL = {
            "....####.....",
            "..##gggg##...",
            ".#gggggggg#..",
            ".#gggggggg#..",
            "#gggggggggg#.",
            "#gggggggggg#.",
            "#gggggggggg#.",
            "#gggggggggg#.",
            ".#gggggggg#..",
            ".#gggggggg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[] SELECTED = {
            "....####.....",
            "..##gggg##...",
            ".#gggggggg#..",
            ".#ggoooogg#..",
            "#ggoooooogg#.",
            "#ggoooooogg#.",
            "#ggoooooogg#.",
            "#ggoooooogg#.",
            ".#ggoooogg#..",
            ".#gggggggg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[] PRESSED = {
            "....####.....",
            "..########...",
            ".#.#ppppp.#..",
            ".##ppppppp#..",
            "##pppppppp.#.",
            "##pppppppp.#.",
            "##pppppppp.#.",
            "##pppppppp.#.",
            ".#pppppppp#..",
            ".##pppppp.#..",
            "..##....##...",
            "....####.....",
            "............." };

        private static final String[] PRESSED_SELECTED = {
            "....####.....",
            "..########...",
            ".#.#ppppp.#..",
            ".##poooopp#..",
            "##poooooop.#.",
            "##poooooop.#.",
            "##poooooop.#.",
            "##poooooop.#.",
            ".#ppoooopp#..",
            ".##pppppp.#..",
            "..##....##...",
            "....####.....",
            "............." };

        private static final String[] DISABLED = {
            "....qqqq.....",
            "..qq....qq...",
            ".q........q..",
            ".q........q..",
            "q..........q.",
            "q..........q.",
            "q..........q.",
            "q..........q.",
            ".q........q..",
            ".q........q..",
            "..qq....qq...",
            "....qqqq.....",
            "............." };

        private static final String[] DISABLED_SELECTED = {
            "....qqqq.....",
            "..qq....qq...",
            ".q........q..",
            ".q..####..q..",
            "q..######..q.",
            "q..######..q.",
            "q..######..q.",
            "q..######..q.",
            ".q..####..q..",
            ".q........q..",
            "..qq....qq...",
            "....qqqq.....",
            "............." };

        private static final String[] ROLLOVER = {
            "....####.....",
            "..##gggg##...",
            ".#ggppppgg#..",
            ".#gpggggpg#..",
            "#gpggggggpg#.",
            "#gpggggggpg#.",
            "#gpggggggpg#.",
            "#gpggggggpg#.",
            ".#gpggggpg#..",
            ".#ggppppgg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[] ROLLOVER_SELECTED = {
            "....####.....",
            "..##gggg##...",
            ".#ggppppgg#..",
            ".#gpoooopg#..",
            "#gpoooooopg#.",
            "#gpoooooopg#.",
            "#gpoooooopg#.",
            "#gpoooooopg#.",
            ".#gpoooopg#..",
            ".#ggppppgg#..",
            "..##gggg##...",
            "....####.....",
            "............." };

        private static final String[][] MAPS = { NORMAL, SELECTED, PRESSED,
            PRESSED_SELECTED, DISABLED, DISABLED_SELECTED, ROLLOVER,
            ROLLOVER_SELECTED };

        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel model = ((AbstractButton) c).getModel();
            paintMap(g, x, y, MAPS[state(model)], GRADIENT);
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    }

    // ---- the other twenty-four; see the class note ----

    /** The direction of the light for an internal frame icon: from above. */
    public static final boolean LIGHT = true;

    /** And the opposite one. */
    public static final boolean DARK = false;

    private static Icon checkBoxMenuItemIcon;
    private static Icon radioButtonMenuItemIcon;
    private static Icon menuArrowIcon;
    private static Icon menuItemArrowIcon;
    private static Icon treeComputerIcon;
    private static Icon treeHardDriveIcon;
    private static Icon treeFloppyDriveIcon;
    private static Icon fcNewFolderIcon;
    private static Icon fcUpFolderIcon;
    private static Icon fcHomeFolderIcon;
    private static Icon fcDetailViewIcon;
    private static Icon fcListViewIcon;
    private static Icon hSliderThumbIcon;
    private static Icon vSliderThumbIcon;
    private static Icon ifDefaultMenuIcon;

    public static Icon getCheckBoxMenuItemIcon() {
        if (checkBoxMenuItemIcon == null) {
            checkBoxMenuItemIcon = new CheckBoxMenuItemIcon();
        }
        return checkBoxMenuItemIcon;
    }

    public static Icon getRadioButtonMenuItemIcon() {
        if (radioButtonMenuItemIcon == null) {
            radioButtonMenuItemIcon = new RadioButtonMenuItemIcon();
        }
        return radioButtonMenuItemIcon;
    }

    /** None; see the class note. */
    public static Icon getMenuItemCheckIcon() {
        return null;
    }

    public static Icon getMenuItemArrowIcon() {
        if (menuItemArrowIcon == null) {
            menuItemArrowIcon = new MenuItemArrowIcon();
        }
        return menuItemArrowIcon;
    }

    public static Icon getMenuArrowIcon() {
        if (menuArrowIcon == null) {
            menuArrowIcon = new MenuArrowIcon();
        }
        return menuArrowIcon;
    }

    /**
     * The handle that opens and closes a branch.
     *
     * <p>A new one every time, the same as the folder and the leaf. The three are just the ones
     * that are not {@link UIResource} -- a tree may keep them even if the look and feel changes --
     * and sharing them would make two trees with different themes tread on each other's icon.
     * Measured.
     *
     * @param isCollapsed whether the branch is closed
     */
    public static Icon getTreeControlIcon(boolean isCollapsed) {
        return new TreeControlIcon(isCollapsed);
    }

    public static Icon getTreeFolderIcon() {
        return new TreeFolderIcon();
    }

    public static Icon getTreeLeafIcon() {
        return new TreeLeafIcon();
    }

    public static Icon getTreeComputerIcon() {
        if (treeComputerIcon == null) {
            treeComputerIcon = new TreeComputerIcon();
        }
        return treeComputerIcon;
    }

    public static Icon getTreeHardDriveIcon() {
        if (treeHardDriveIcon == null) {
            treeHardDriveIcon = new TreeHardDriveIcon();
        }
        return treeHardDriveIcon;
    }

    public static Icon getTreeFloppyDriveIcon() {
        if (treeFloppyDriveIcon == null) {
            treeFloppyDriveIcon = new TreeFloppyDriveIcon();
        }
        return treeFloppyDriveIcon;
    }

    public static Icon getFileChooserNewFolderIcon() {
        if (fcNewFolderIcon == null) {
            fcNewFolderIcon = new FileChooserNewFolderIcon();
        }
        return fcNewFolderIcon;
    }

    public static Icon getFileChooserUpFolderIcon() {
        if (fcUpFolderIcon == null) {
            fcUpFolderIcon = new FileChooserUpFolderIcon();
        }
        return fcUpFolderIcon;
    }

    public static Icon getFileChooserHomeFolderIcon() {
        if (fcHomeFolderIcon == null) {
            fcHomeFolderIcon = new FileChooserHomeFolderIcon();
        }
        return fcHomeFolderIcon;
    }

    public static Icon getFileChooserDetailViewIcon() {
        if (fcDetailViewIcon == null) {
            fcDetailViewIcon = new FileChooserDetailViewIcon();
        }
        return fcDetailViewIcon;
    }

    public static Icon getFileChooserListViewIcon() {
        if (fcListViewIcon == null) {
            fcListViewIcon = new FileChooserListViewIcon();
        }
        return fcListViewIcon;
    }

    /** A horizontal slider's thumb: fifteen wide by sixteen high. */
    public static Icon getHorizontalSliderThumbIcon() {
        if (hSliderThumbIcon == null) {
            hSliderThumbIcon = new OceanHorizontalSliderThumbIcon();
        }
        return hSliderThumbIcon;
    }

    public static Icon getVerticalSliderThumbIcon() {
        if (vSliderThumbIcon == null) {
            vSliderThumbIcon = new OceanVerticalSliderThumbIcon();
        }
        return vSliderThumbIcon;
    }

    public static Icon getInternalFrameDefaultMenuIcon() {
        if (ifDefaultMenuIcon == null) {
            ifDefaultMenuIcon = new InternalFrameDefaultMenuIcon();
        }
        return ifDefaultMenuIcon;
    }

    /**
     * The cross that closes an internal frame.
     *
     * <p>The four title bar buttons take the size as a parameter and do not have it fixed: a normal
     * window wants them at sixteen and a palette at eight. That is why these five methods build a
     * new one every time instead of sharing.
     *
     * @param size the side, in pixels
     */
    public static Icon getInternalFrameCloseIcon(int size) {
        return new InternalFrameCloseIcon(size);
    }

    public static Icon getInternalFrameMaximizeIcon(int size) {
        return new InternalFrameMaximizeIcon(size);
    }

    /** The restore one: two shifted frames. */
    public static Icon getInternalFrameAltMaximizeIcon(int size) {
        return new InternalFrameAltMaximizeIcon(size);
    }

    public static Icon getInternalFrameMinimizeIcon(int size) {
        return new InternalFrameMinimizeIcon(size);
    }

    // ---- the classes; the names are the JDK's and are seen through getClass().getName() ----

    /** An icon of fixed side that draws in the theme's colours. */
    private abstract static class Drawn implements Icon, Serializable {

        private final int width;
        private final int height;

        Drawn(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getIconWidth() {
            return width;
        }

        public int getIconHeight() {
            return height;
        }

        /** The stroke colour: the theme's, or the grey if the component does not respond. */
        static Color trazo(Component c) {
            if (c != null && !c.isEnabled()) {
                return MetalLookAndFeel.getControlShadow();
            }
            return MetalLookAndFeel.getControlInfo();
        }
    }

    /** The little square of a menu item with a check box. */
    public static class CheckBoxMenuItemIcon extends Drawn implements UIResource {

        public CheckBoxMenuItemIcon() {
            super(10, 10);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            boolean on = (c instanceof AbstractButton)
                    && ((AbstractButton) c).getModel().isSelected();
            g.setColor(trazo(c));
            g.drawRect(x, y, 9, 9);
            if (on) {
                g.drawLine(x + 2, y + 5, x + 4, y + 7);
                g.drawLine(x + 4, y + 7, x + 7, y + 2);
            }
        }
    }

    /** And the little circle of one with an option. */
    public static class RadioButtonMenuItemIcon extends Drawn implements UIResource {

        public RadioButtonMenuItemIcon() {
            super(10, 10);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            boolean on = (c instanceof AbstractButton)
                    && ((AbstractButton) c).getModel().isSelected();
            g.setColor(trazo(c));
            g.drawOval(x, y, 9, 9);
            if (on) {
                g.fillOval(x + 3, y + 3, 4, 4);
            }
        }
    }

    /** The arrow that says a menu item opens a submenu. */
    public static class MenuArrowIcon extends Drawn implements UIResource {

        public MenuArrowIcon() {
            super(4, 8);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(trazo(c));
            for (int i = 0; i < 4; i++) {
                g.drawLine(x + i, y + i, x + i, y + 7 - i);
            }
        }
    }

    /** The same arrow, for an item that is not a menu. */
    public static class MenuItemArrowIcon extends Drawn implements UIResource {

        public MenuItemArrowIcon() {
            super(4, 8);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }
    }

    /** A branch's handle; see {@link MetalIconFactory#getTreeControlIcon}. */
    public static class TreeControlIcon extends Drawn {

        protected boolean isLight;

        public TreeControlIcon(boolean isCollapsed) {
            super(18, 18);
            this.isLight = isCollapsed;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            drawControlIcon(g, x, y, isLight);
        }

        /** A circle with a plus or a minus inside. */
        void drawControlIcon(Graphics g, int x, int y, boolean closed) {
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawOval(x + 4, y + 4, 9, 9);
            g.drawLine(x + 6, y + 8, x + 11, y + 8);
            if (closed) {
                g.drawLine(x + 8, y + 6, x + 8, y + 11);
            }
        }
    }

    /** The tree's folder. It is not a look and feel resource; see the class note. */
    public static class TreeFolderIcon extends Drawn {

        public TreeFolderIcon() {
            super(16, 18);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 5, 14, 11);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 1, y + 5, 13, 10);
            g.drawLine(x + 1, y + 4, x + 6, y + 4);
            g.drawLine(x + 6, y + 4, x + 8, y + 5);
        }
    }

    /** And the leaf. It is not a resource either. */
    public static class TreeLeafIcon extends Drawn {

        public TreeLeafIcon() {
            super(16, 20);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getWindowBackground());
            g.fillRect(x + 2, y + 2, 11, 15);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 2, y + 2, 10, 14);
            // The folded corner, which is what makes it read as a sheet of paper.
            g.drawLine(x + 9, y + 2, x + 12, y + 5);
        }
    }

    /** The file chooser's computer. */
    public static class TreeComputerIcon extends Drawn implements UIResource {

        public TreeComputerIcon() {
            super(16, 16);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 2, y + 2, 11, 8);
            g.drawRect(x + 5, y + 12, 5, 2);
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 3, y + 3, 10, 7);
        }
    }

    /** The hard disk. */
    public static class TreeHardDriveIcon extends Drawn implements UIResource {

        public TreeHardDriveIcon() {
            super(16, 16);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 5, 14, 6);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 1, y + 5, 13, 5);
            g.drawLine(x + 11, y + 8, x + 12, y + 8);
        }
    }

    /** And the floppy disk, which is still there for compatibility with an era. */
    public static class TreeFloppyDriveIcon extends Drawn implements UIResource {

        public TreeFloppyDriveIcon() {
            super(16, 16);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 2, 14, 12);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 1, y + 2, 13, 11);
            g.fillRect(x + 5, y + 3, 6, 4);
            g.drawRect(x + 4, y + 9, 7, 4);
        }
    }

    /** A folder of eighteen, the base of the chooser's three buttons. */
    private abstract static class FileChooserFolder extends Drawn implements UIResource {

        FileChooserFolder() {
            super(18, 18);
        }

        /** The folder alone; each button adds its mark on top. */
        void folder(Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 5, 15, 10);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 1, y + 5, 14, 9);
            g.drawLine(x + 1, y + 4, x + 6, y + 4);
            g.drawLine(x + 6, y + 4, x + 8, y + 5);
        }
    }

    /** The folder with a star: create a new one. */
    public static class FileChooserNewFolderIcon extends FileChooserFolder {

        public FileChooserNewFolderIcon() {
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            folder(g, x, y);
        }
    }

    /** The folder with an up arrow: go up one level. */
    public static class FileChooserUpFolderIcon extends FileChooserFolder {

        public FileChooserUpFolderIcon() {
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            folder(g, x, y);
            g.setColor(MetalLookAndFeel.getControlInfo());
            g.drawLine(x + 8, y + 7, x + 8, y + 12);
            g.drawLine(x + 6, y + 9, x + 8, y + 7);
            g.drawLine(x + 8, y + 7, x + 10, y + 9);
        }
    }

    /** And the little house. */
    public static class FileChooserHomeFolderIcon extends Drawn implements UIResource {

        public FileChooserHomeFolderIcon() {
            super(18, 18);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawLine(x + 2, y + 9, x + 9, y + 2);
            g.drawLine(x + 9, y + 2, x + 16, y + 9);
            g.drawRect(x + 4, y + 9, 10, 6);
        }
    }

    /** The three lines of the detail view. */
    public static class FileChooserDetailViewIcon extends Drawn implements UIResource {

        public FileChooserDetailViewIcon() {
            super(18, 18);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            for (int i = 0; i < 3; i++) {
                g.fillRect(x + 3, y + 4 + i * 4, 2, 2);
                g.fillRect(x + 7, y + 4 + i * 4, 8, 2);
            }
        }
    }

    /** And the two columns of the list view. */
    public static class FileChooserListViewIcon extends Drawn implements UIResource {

        public FileChooserListViewIcon() {
            super(18, 18);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            for (int col = 0; col < 2; col++) {
                for (int i = 0; i < 3; i++) {
                    g.fillRect(x + 3 + col * 8, y + 4 + i * 4, 2, 2);
                    g.fillRect(x + 6 + col * 8, y + 4 + i * 4, 3, 2);
                }
            }
        }
    }

    /**
     * A horizontal slider's thumb.
     *
     * <p>Fifteen wide by sixteen high, and the tip at the bottom. The name carries {@code Ocean}
     * because the Steel theme uses another; both exist in the JDK and
     * {@code getHorizontalSliderThumbIcon} returns whichever corresponds to the theme.
     */
    public static class OceanHorizontalSliderThumbIcon extends Drawn implements UIResource {

        public OceanHorizontalSliderThumbIcon() {
            super(15, 16);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 1, 13, 9);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x, y, 14, 9);
            for (int i = 0; i < 6; i++) {
                g.drawLine(x + 1 + i, y + 10 + i, x + 13 - i, y + 10 + i);
            }
        }
    }

    /** The same one, upright. */
    public static class OceanVerticalSliderThumbIcon extends Drawn implements UIResource {

        public OceanVerticalSliderThumbIcon() {
            super(16, 15);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 1, y + 1, 9, 13);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x, y, 9, 14);
            for (int i = 0; i < 6; i++) {
                g.drawLine(x + 10 + i, y + 1 + i, x + 10 + i, y + 13 - i);
            }
        }
    }

    /** The icon of the left corner of a title bar. */
    public static class InternalFrameDefaultMenuIcon extends Drawn implements UIResource {

        public InternalFrameDefaultMenuIcon() {
            super(16, 16);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.fillRect(x + 2, y + 3, 12, 10);
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(x + 2, y + 3, 11, 9);
        }
    }

    /** The four title bar buttons; the side arrives as a parameter. */
    private abstract static class TitleButton extends Drawn implements UIResource {

        TitleButton(int side) {
            super(side, side);
        }

        int margin() {
            return getIconWidth() / 4;
        }

        int far() {
            return getIconWidth() - 1 - margin();
        }
    }

    /** The cross. */
    public static class InternalFrameCloseIcon extends TitleButton {

        public InternalFrameCloseIcon(int size) {
            super(size);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int m = margin();
            int f = far();
            g.setColor(MetalLookAndFeel.getControlInfo());
            g.drawLine(x + m, y + m, x + f, y + f);
            g.drawLine(x + m + 1, y + m, x + f, y + f - 1);
            g.drawLine(x + f, y + m, x + m, y + f);
            g.drawLine(x + f - 1, y + m, x + m, y + f - 1);
        }
    }

    /** The maximize square. */
    public static class InternalFrameMaximizeIcon extends TitleButton {

        public InternalFrameMaximizeIcon(int size) {
            super(size);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int m = margin();
            int f = far();
            g.setColor(MetalLookAndFeel.getControlInfo());
            g.drawRect(x + m, y + m, f - m, f - m);
            g.drawLine(x + m, y + m + 1, x + f, y + m + 1);
        }
    }

    /** The two shifted squares of restore. */
    public static class InternalFrameAltMaximizeIcon extends TitleButton {

        public InternalFrameAltMaximizeIcon(int size) {
            super(size);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int m = margin();
            int f = far();
            g.setColor(MetalLookAndFeel.getControlInfo());
            g.drawRect(x + m, y + m + 2, f - m - 2, f - m - 2);
            g.drawRect(x + m + 2, y + m, f - m - 2, f - m - 2);
        }
    }

    /** And the iconify line. */
    public static class InternalFrameMinimizeIcon extends TitleButton {

        public InternalFrameMinimizeIcon(int size) {
            super(size);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int m = margin();
            int f = far();
            g.setColor(MetalLookAndFeel.getControlInfo());
            g.fillRect(x + m, y + f - 1, f - m + 1, 2);
        }
    }
}
