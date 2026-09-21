package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyListener;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.MenuItemUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a menu item.
 *
 * <h2>A menu item is not a button with another border</h2>
 *
 * <p>It looks like one -- it has a model, text and an icon -- but it has three columns of its
 * own that a button does not have: the tick's on the left, the accelerator's on the right, and
 * the submenu arrow's at the end. Placing those three is almost everything this class does.
 *
 * <h2>The size's arithmetic, and why it ends up odd</h2>
 *
 * <p>The width is built up by adding from left to right: the margins, the tick with its gap,
 * the icon with its own, the text with its own, the accelerator with its own, the arrow with
 * its own, and one last breath. Every gap is the same {@link #defaultTextIconGap}.
 *
 * <p>And afterwards, if the width or the height came out even, one is added to them. That is an
 * oddity of the JDK that <em>has</em> to be copied: the tick icons are drawn centred, and an
 * even width leaves them half a pixel out of place and with the border eaten. It is measured in
 * twelve cases -- empty text, with an icon, with a tick, with three different accelerators, a
 * loose menu and a bar menu -- and the formula gives the exact number in all twelve.
 *
 * <p>Empty text reserves no line height: an item with no text measures whatever its arrow
 * measures.
 *
 * <h2>Neither minimum nor maximum</h2>
 *
 * <p>{@link #getMinimumSize} and {@link #getMaximumSize} return {@code null}. It is not an
 * oversight: a menu item neither stretches nor shrinks by itself, and who lays it out is
 * {@code DefaultMenuLayout}, which hands out the width of the whole popup window. Answering a
 * number would be lying to it.
 *
 * <h2>What it installs, and where it comes from</h2>
 *
 * <p>The values of {@code MenuItem.*} measured in Metal (JDK 25): typeface Dialog bold 12,
 * foreground (51, 51, 51), background (238, 238, 238), selection (163, 184, 204) over
 * (51, 51, 51), disabled (153, 153, 153), accelerator (99, 130, 191) in Dialog 10, accelerator
 * separator "-", gap 4, margin (2, 2, 2, 2), and with no tick -- only tickable items have the
 * tick --.
 *
 * <h2>When there is neither tick nor arrow</h2>
 *
 * <p>A menu that hangs directly from the bar carries neither of the two: it has no state to
 * mark and its submenu opens downwards, not to the side. {@link #useCheckAndArrow} is the one
 * that decides it, and that is where the eight pixels of difference between a bar menu and an
 * inside one come from.
 */
public class BasicMenuItemUI extends MenuItemUI {

    protected JMenuItem menuItem = null;
    protected Color selectionBackground;
    protected Color selectionForeground;
    protected Color disabledForeground;
    protected Color acceleratorForeground;
    protected Color acceleratorSelectionForeground;

    /** What goes between an accelerator's keys: {@code "Ctrl-O"}. */
    protected String acceleratorDelimiter;

    protected int defaultTextIconGap;
    protected Font acceleratorFont;

    protected MouseInputListener mouseInputListener;
    protected MenuDragMouseListener menuDragMouseListener;
    protected MenuKeyListener menuKeyListener;
    protected PropertyChangeListener propertyChangeListener;

    protected Icon arrowIcon = null;
    protected Icon checkIcon = null;

    /**
     * Whether the item painted its border before it was installed; it is restored on removing it.
     */
    protected boolean oldBorderPainted;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource SELECTION = new ColorUIResource(163, 184, 204);
    private static final ColorUIResource DISABLED = new ColorUIResource(153, 153, 153);
    private static final ColorUIResource ACCELERATOR = new ColorUIResource(99, 130, 191);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);
    private static final FontUIResource ACCELERATOR_FONT =
            new FontUIResource("Dialog", Font.PLAIN, 10);

    public BasicMenuItemUI() {
    }

    /** A new one per item: it keeps the component and its listeners. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicMenuItemUI();
    }

    public void installUI(JComponent c) {
        menuItem = (JMenuItem) c;
        installDefaults();
        installComponents(menuItem);
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        menuItem = (JMenuItem) c;
        uninstallDefaults();
        uninstallComponents(menuItem);
        uninstallListeners();
        uninstallKeyboardActions();
        menuItem = null;
    }

    /** The prefix the look and feel's values are looked up with. */
    protected String getPropertyPrefix() {
        return "MenuItem";
    }

    /** Colours, typefaces, icons and margin; see the class note. */
    protected void installDefaults() {
        Color background = menuItem.getBackground();
        if (background == null || background instanceof UIResource) {
            menuItem.setBackground(BACKGROUND);
        }
        Color foreground = menuItem.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            menuItem.setForeground(FOREGROUND);
        }
        Font font = menuItem.getFont();
        if (font == null || font instanceof UIResource) {
            menuItem.setFont(FONT);
        }
        Insets margin = menuItem.getMargin();
        if (margin == null || margin instanceof UIResource) {
            menuItem.setMargin(new InsetsUIResource(2, 2, 2, 2));
        }
        if (menuItem.getBorder() == null || menuItem.getBorder() instanceof UIResource) {
            menuItem.setBorder(new BasicBorders.MarginBorder());
        }

        selectionBackground = SELECTION;
        selectionForeground = FOREGROUND;
        disabledForeground = DISABLED;
        acceleratorForeground = ACCELERATOR;
        acceleratorSelectionForeground = FOREGROUND;
        acceleratorFont = ACCELERATOR_FONT;
        acceleratorDelimiter = "-";
        defaultTextIconGap = 4;

        // The ordinary item carries no tick; the tickable one and the option one set it in their
        // own class.
        checkIcon = null;
        arrowIcon = BasicIconFactory.getMenuItemArrowIcon();

        oldBorderPainted = menuItem.isBorderPainted();
        LookAndFeel.installProperty(menuItem, "borderPainted", Boolean.TRUE);
        LookAndFeel.installProperty(menuItem, "opaque", Boolean.TRUE);
        menuItem.setHorizontalTextPosition(SwingConstants.TRAILING);
        menuItem.setHorizontalAlignment(SwingConstants.LEADING);
    }

    /** It gives the border painting back the way it was; see {@link #oldBorderPainted}. */
    protected void uninstallDefaults() {
        LookAndFeel.installProperty(menuItem, "borderPainted",
                Boolean.valueOf(oldBorderPainted));
        selectionBackground = null;
        selectionForeground = null;
        disabledForeground = null;
        acceleratorForeground = null;
        acceleratorSelectionForeground = null;
        acceleratorFont = null;
        arrowIcon = null;
        checkIcon = null;
    }

    /** It builds the HTML view if the text needs it. */
    protected void installComponents(JMenuItem menuItem) {
        BasicHTML.updateRenderer(menuItem, menuItem.getText());
    }

    protected void uninstallComponents(JMenuItem menuItem) {
        BasicHTML.updateRenderer(menuItem, "");
    }

    protected void installListeners() {
        mouseInputListener = createMouseInputListener(menuItem);
        if (mouseInputListener != null) {
            menuItem.addMouseListener(mouseInputListener);
            menuItem.addMouseMotionListener(mouseInputListener);
        }
        menuDragMouseListener = createMenuDragMouseListener(menuItem);
        if (menuDragMouseListener != null) {
            menuItem.addMenuDragMouseListener(menuDragMouseListener);
        }
        menuKeyListener = createMenuKeyListener(menuItem);
        if (menuKeyListener != null) {
            menuItem.addMenuKeyListener(menuKeyListener);
        }
        propertyChangeListener = createPropertyChangeListener(menuItem);
        if (propertyChangeListener != null) {
            menuItem.addPropertyChangeListener(propertyChangeListener);
        }
    }

    protected void uninstallListeners() {
        if (mouseInputListener != null) {
            menuItem.removeMouseListener(mouseInputListener);
            menuItem.removeMouseMotionListener(mouseInputListener);
        }
        if (menuDragMouseListener != null) {
            menuItem.removeMenuDragMouseListener(menuDragMouseListener);
        }
        if (menuKeyListener != null) {
            menuItem.removeMenuKeyListener(menuKeyListener);
        }
        if (propertyChangeListener != null) {
            menuItem.removePropertyChangeListener(propertyChangeListener);
        }
        mouseInputListener = null;
        menuDragMouseListener = null;
        menuKeyListener = null;
        propertyChangeListener = null;
    }

    /**
     * With no shortcuts of its own: an item's are handled by the {@link JMenuItem}'s own
     * accelerator.
     */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected MouseInputListener createMouseInputListener(JComponent c) {
        return new Handler();
    }

    protected MenuDragMouseListener createMenuDragMouseListener(JComponent c) {
        return new Handler();
    }

    /**
     * None.
     *
     * <p>An ordinary item does not listen to keys: the underlined letter is handled by the menu
     * that contains it, and the accelerator by the {@link JMenuItem} itself. Returning
     * {@code null} leaves the field null, and that is what is seen from the other side. Measured.
     */
    protected MenuKeyListener createMenuKeyListener(JComponent c) {
        return null;
    }

    protected PropertyChangeListener createPropertyChangeListener(JComponent c) {
        return new Handler();
    }

    /** See the class note: it is computed by {@link #getPreferredMenuItemSize}. */
    public Dimension getPreferredSize(JComponent c) {
        return getPreferredMenuItemSize(c, checkIcon, arrowIcon, defaultTextIconGap);
    }

    /** {@code null}; see the class note. */
    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    /** {@code null}; see the class note. */
    public Dimension getMaximumSize(JComponent c) {
        return null;
    }

    /**
     * Whether this item carries a tick column and an arrow one.
     *
     * <p>The menu that hangs from the bar does not; see the class note.
     */
    private boolean useCheckAndArrow() {
        return !(menuItem instanceof JMenu && ((JMenu) menuItem).isTopLevelMenu());
    }

    /** The accelerator's text, already built: {@code "Ctrl-O"}. */
    private String getAcceleratorText() {
        KeyStroke accelerator = menuItem.getAccelerator();
        if (accelerator == null) {
            return "";
        }
        String s = "";
        int modifiers = accelerator.getModifiers();
        if (modifiers > 0) {
            s = KeyEvent.getKeyModifiersText(modifiers) + acceleratorDelimiter;
        }
        int keyCode = accelerator.getKeyCode();
        if (keyCode != 0) {
            s += KeyEvent.getKeyText(keyCode);
        } else {
            s += accelerator.getKeyChar();
        }
        return s;
    }

    /** The whole arithmetic; see the class note. */
    protected Dimension getPreferredMenuItemSize(JComponent c, Icon checkIcon, Icon arrowIcon,
            int defaultTextIconGap) {
        JMenuItem b = (JMenuItem) c;
        Icon icon = b.getIcon();
        String text = b.getText();
        String acceleratorText = getAcceleratorText();
        boolean hasText = text != null && !text.equals("");
        boolean withColumns = useCheckAndArrow();

        FontMetrics fm = b.getFontMetrics(b.getFont());
        FontMetrics fmAccel = b.getFontMetrics(acceleratorFont);

        int textWidth = 0;
        int textHeight = 0;
        if (hasText) {
            javax.swing.text.View v =
                    (javax.swing.text.View) b.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                textWidth = (int) v.getPreferredSpan(javax.swing.text.View.X_AXIS);
                textHeight = (int) v.getPreferredSpan(javax.swing.text.View.Y_AXIS);
            } else {
                textWidth = fm.stringWidth(text);
                textHeight = fm.getHeight();
            }
        }

        Insets insets = b.getInsets();
        int w = insets.left + insets.right;
        int height = textHeight;

        if (withColumns && checkIcon != null) {
            w += checkIcon.getIconWidth() + defaultTextIconGap;
            height = Math.max(height, checkIcon.getIconHeight());
        }
        if (icon != null) {
            w += icon.getIconWidth() + defaultTextIconGap;
            height = Math.max(height, icon.getIconHeight());
        }
        w += textWidth;
        if (hasText) {
            w += defaultTextIconGap;
        }
        if (!acceleratorText.equals("")) {
            w += fmAccel.stringWidth(acceleratorText) + defaultTextIconGap;
            height = Math.max(height, fmAccel.getHeight());
        }
        if (withColumns && arrowIcon != null) {
            w += arrowIcon.getIconWidth() + defaultTextIconGap;
            height = Math.max(height, arrowIcon.getIconHeight());
        }
        // The breath at the end.
        w += defaultTextIconGap;

        int h = height + insets.top + insets.bottom;

        // See the class note: even becomes odd.
        if (w % 2 == 0) {
            w++;
        }
        if (h % 2 == 0) {
            h++;
        }
        return new Dimension(w, h);
    }

    /**
     * It fills the background -- with the selection colour if it is chosen -- and then paints the
     * item.
     */
    public void update(Graphics g, JComponent c) {
        paint(g, c);
    }

    public void paint(Graphics g, JComponent c) {
        paintMenuItem(g, c, checkIcon, arrowIcon, selectionBackground, selectionForeground,
                defaultTextIconGap);
    }

    /**
     * The whole item: background, tick, icon, text, accelerator and arrow.
     *
     * <p>The five pieces are placed with the same arithmetic as
     * {@link #getPreferredMenuItemSize}, in the same order.
     */
    protected void paintMenuItem(Graphics g, JComponent c, Icon checkIcon, Icon arrowIcon,
            Color background, Color foreground, int defaultTextIconGap) {
        JMenuItem b = (JMenuItem) c;
        ButtonModel model = b.getModel();
        int menuWidth = b.getWidth();
        int menuHeight = b.getHeight();
        Insets i = c.getInsets();

        Color oldColor = g.getColor();
        Font oldFont = g.getFont();
        g.setFont(b.getFont());

        paintBackground(g, b, background);

        Rectangle viewRect = new Rectangle(i.left, i.top,
                menuWidth - (i.right + i.left), menuHeight - (i.bottom + i.top));
        boolean withColumns = useCheckAndArrow();
        int x = viewRect.x;

        if (withColumns && checkIcon != null) {
            checkIcon.paintIcon(c, g,
                    x, viewRect.y + (viewRect.height - checkIcon.getIconHeight()) / 2);
            x += checkIcon.getIconWidth() + defaultTextIconGap;
        }
        Icon icon = b.getIcon();
        if (!model.isEnabled()) {
            icon = b.getDisabledIcon();
        } else if (model.isPressed() && model.isArmed()) {
            Icon pressed = b.getPressedIcon();
            if (pressed != null) {
                icon = pressed;
            }
        }
        if (icon != null) {
            icon.paintIcon(c, g, x, viewRect.y + (viewRect.height - icon.getIconHeight()) / 2);
            x += icon.getIconWidth() + defaultTextIconGap;
        }

        String text = b.getText();
        if (text != null && !text.equals("")) {
            FontMetrics fm = b.getFontMetrics(b.getFont());
            Rectangle textRect = new Rectangle(x,
                    viewRect.y + (viewRect.height - fm.getHeight()) / 2,
                    fm.stringWidth(text), fm.getHeight());
            paintText(g, b, textRect, text);
        }

        String acceleratorText = getAcceleratorText();
        if (!acceleratorText.equals("")) {
            FontMetrics fmAccel = b.getFontMetrics(acceleratorFont);
            int width = fmAccel.stringWidth(acceleratorText);
            int ax = viewRect.x + viewRect.width - width;
            if (withColumns && arrowIcon != null) {
                ax -= arrowIcon.getIconWidth() + defaultTextIconGap;
            }
            g.setFont(acceleratorFont);
            g.setColor(model.isArmed() || (b instanceof JMenu && model.isSelected())
                    ? acceleratorSelectionForeground : acceleratorForeground);
            g.drawString(acceleratorText, ax,
                    viewRect.y + (viewRect.height - fmAccel.getHeight()) / 2
                            + fmAccel.getAscent());
        }

        if (withColumns && arrowIcon != null) {
            arrowIcon.paintIcon(c, g,
                    viewRect.x + viewRect.width - arrowIcon.getIconWidth(),
                    viewRect.y + (viewRect.height - arrowIcon.getIconHeight()) / 2);
        }

        g.setColor(oldColor);
        g.setFont(oldFont);
    }

    /**
     * The background: the selection colour if the item is pointed at, and its own if not.
     *
     * <p>A transparent item paints nothing unless it is pointed at: there it does, because the bar
     * that marks where the mouse is has to be seen even if the menu behind is transparent.
     */
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
        ButtonModel model = menuItem.getModel();
        Color old = g.getColor();
        int menuWidth = menuItem.getWidth();
        int menuHeight = menuItem.getHeight();

        if (menuItem.isOpaque()) {
            if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
                g.setColor(bgColor);
            } else {
                g.setColor(menuItem.getBackground());
            }
            g.fillRect(0, 0, menuWidth, menuHeight);
        } else if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
            g.setColor(bgColor);
            g.fillRect(0, 0, menuWidth, menuHeight);
        }
        g.setColor(old);
    }

    /** The text, with the colour that corresponds to the state. */
    protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
        ButtonModel model = menuItem.getModel();
        FontMetrics fm = menuItem.getFontMetrics(menuItem.getFont());

        javax.swing.text.View v =
                (javax.swing.text.View) menuItem.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            v.paint(g, textRect);
            return;
        }
        if (!model.isEnabled()) {
            g.setColor(disabledForeground != null
                    ? disabledForeground : menuItem.getBackground().darker());
        } else if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
            g.setColor(selectionForeground);
        } else {
            g.setColor(menuItem.getForeground());
        }
        g.drawString(text, textRect.x, textRect.y + fm.getAscent());
    }

    /**
     * It presses the item and closes the menu.
     *
     * <p>First it closes and then it fires: the other way round, the action would run with the
     * menu still open, and an action that shows a dialog would leave it hanging on top.
     */
    protected void doClick(MenuSelectionManager msm) {
        if (msm == null) {
            msm = MenuSelectionManager.defaultManager();
        }
        msm.clearSelectedPath();
        menuItem.doClick(0);
    }

    /**
     * The path of menus down to this item.
     *
     * <p>Empty if there is no menu open. If there is, it is the path that was already there plus
     * this item; and if the path ended in a sibling, it is cut back to the common parent before
     * adding it.
     */
    public MenuElement[] getPath() {
        MenuSelectionManager m = MenuSelectionManager.defaultManager();
        MenuElement[] oldPath = m.getSelectedPath();
        MenuElement[] newPath;
        int i = oldPath.length;
        if (i == 0) {
            return new MenuElement[0];
        }
        java.awt.Component parent = menuItem.getParent();
        if (oldPath[i - 1].getComponent() == parent) {
            newPath = new MenuElement[i + 1];
            System.arraycopy(oldPath, 0, newPath, 0, i);
            newPath[i] = menuItem;
        } else {
            int j;
            for (j = oldPath.length - 1; j >= 0; j--) {
                if (oldPath[j].getComponent() == parent) {
                    break;
                }
            }
            newPath = new MenuElement[j + 2];
            System.arraycopy(oldPath, 0, newPath, 0, j + 1);
            newPath[j + 1] = menuItem;
        }
        return newPath;
    }

    /**
     * The one that listens to everything: mouse, dragging over the menu and property changes.
     *
     * <p>A single one instead of three because all three react to the same thing -- where the
     * mouse is -- and separating them would force state to be shared between them.
     */
    private class Handler implements MouseInputListener, MenuDragMouseListener,
            PropertyChangeListener {

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            MenuSelectionManager.defaultManager().setSelectedPath(getPath());
        }

        public void mouseReleased(MouseEvent e) {
            if (!menuItem.isEnabled()) {
                return;
            }
            MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            java.awt.Point p = e.getPoint();
            if (p.x >= 0 && p.x < menuItem.getWidth()
                    && p.y >= 0 && p.y < menuItem.getHeight()) {
                doClick(manager);
            } else {
                manager.processMouseEvent(e);
            }
        }

        public void mouseEntered(MouseEvent e) {
            MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            int modifiers = e.getModifiersEx();
            if ((modifiers & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK
                    | MouseEvent.BUTTON3_DOWN_MASK)) != 0) {
                manager.processMouseEvent(e);
            } else {
                manager.setSelectedPath(getPath());
            }
        }

        public void mouseExited(MouseEvent e) {
            MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            int modifiers = e.getModifiersEx();
            if ((modifiers & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK
                    | MouseEvent.BUTTON3_DOWN_MASK)) != 0) {
                manager.processMouseEvent(e);
            } else {
                MenuElement[] path = manager.getSelectedPath();
                if (path.length > 1) {
                    MenuElement[] newPath = new MenuElement[path.length - 1];
                    System.arraycopy(path, 0, newPath, 0, path.length - 1);
                    manager.setSelectedPath(newPath);
                }
            }
        }

        public void mouseDragged(MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseMoved(MouseEvent e) {
        }

        public void menuDragMouseEntered(MenuDragMouseEvent e) {
            MenuSelectionManager manager = e.getMenuSelectionManager();
            MenuElement[] path = e.getPath();
            manager.setSelectedPath(path);
        }

        public void menuDragMouseDragged(MenuDragMouseEvent e) {
        }

        public void menuDragMouseExited(MenuDragMouseEvent e) {
        }

        public void menuDragMouseReleased(MenuDragMouseEvent e) {
            if (!menuItem.isEnabled()) {
                return;
            }
            MenuSelectionManager manager = e.getMenuSelectionManager();
            java.awt.Point p = e.getPoint();
            if (p.x >= 0 && p.x < menuItem.getWidth()
                    && p.y >= 0 && p.y < menuItem.getHeight()) {
                doClick(manager);
            } else {
                manager.clearSelectedPath();
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if ("labelFor".equals(name) || "text".equals(name)) {
                BasicHTML.updateRenderer(menuItem, menuItem.getText());
            } else if ("accelerator".equals(name) || "horizontalTextPosition".equals(name)
                    || "verticalTextPosition".equals(name)) {
                menuItem.revalidate();
                menuItem.repaint();
            }
        }
    }
}
