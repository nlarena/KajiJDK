package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;

/**
 * An internal frame's title bar.
 *
 * <h2>A component, not a part of the drawing</h2>
 *
 * <p>The bar is a real {@link JComponent}, with children: the system menu on the left and the
 * three buttons on the right. It could be a rectangle painted by the frame's look and feel, and
 * it is not for a concrete reason: the buttons have to receive clicks, and the system menu has
 * to be able to drop down. A drawing does neither of the two things.
 *
 * <h2>The title is cut, not shrunk</h2>
 *
 * <p>{@link #getTitle} returns the whole text if it fits and, if not, the longest prefix that
 * fits followed by three dots. A null title gives the empty string, not {@code "null"}. It is
 * the only reasonable thing: shrinking the letters would make two frames on the same desktop
 * have titles of different sizes.
 *
 * <h2>Six actions and seven items</h2>
 *
 * <p>The actions are restore, move, resize, minimize, maximize and close. The system menu has
 * those six plus a separator before close -- seven elements --, and {@link #enableActions}
 * switches each one on and off according to what the frame allows: a frame that cannot be
 * closed has the close item switched off, not absent.
 *
 * <h2>What is left said</h2>
 *
 * <p>The buttons' four icons -- maximize, restore, minimize, close -- come from the look and
 * feel's table, which this library does not have. The buttons are there and work; what there is
 * not is the drawing inside, and that is why the bar measures less in width than the JDK's.
 *
 * <p>Moving and resizing with the keyboard need an event loop: the actions exist and do
 * nothing.
 */
public class BasicInternalFrameTitlePane extends JComponent {

    /** The six commands' names; they are compared by name in {@code actionPerformed}. */
    protected static final String CLOSE_CMD = "Close";

    /** Ver {@link #CLOSE_CMD}. */
    protected static final String ICONIFY_CMD = "Minimize";

    /** Ver {@link #CLOSE_CMD}. */
    protected static final String RESTORE_CMD = "Restore";

    /** Ver {@link #CLOSE_CMD}. */
    protected static final String MAXIMIZE_CMD = "Maximize";

    /** Ver {@link #CLOSE_CMD}. */
    protected static final String MOVE_CMD = "Move";

    /** Ver {@link #CLOSE_CMD}. */
    protected static final String SIZE_CMD = "Size";

    protected JInternalFrame frame;

    protected Color selectedTitleColor;
    protected Color selectedTextColor;
    protected Color notSelectedTitleColor;
    protected Color notSelectedTextColor;

    protected Icon maxIcon;
    protected Icon minIcon;
    protected Icon iconIcon;
    protected Icon closeIcon;

    protected PropertyChangeListener propertyChangeListener;

    protected JMenuBar menuBar;
    protected JMenu windowMenu;

    protected JButton iconButton;
    protected JButton maxButton;
    protected JButton closeButton;

    protected Action closeAction;
    protected Action maximizeAction;
    protected Action iconifyAction;
    protected Action restoreAction;
    protected Action moveAction;
    protected Action sizeAction;

    private static final ColorUIResource SELECTED_TITLE = new ColorUIResource(184, 207, 229);
    private static final ColorUIResource TEXT = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource UNSELECTED_TITLE = new ColorUIResource(238, 238, 238);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);

    /** For that frame; it builds actions, buttons and system menu. */
    public BasicInternalFrameTitlePane(JInternalFrame f) {
        this.frame = f;
        installTitlePane();
    }

    /** The order matters: the actions first, because the buttons use them. */
    protected void installTitlePane() {
        installDefaults();
        installListeners();
        createActions();
        enableActions();
        createButtons();
        setLayout(createLayout());
        assembleSystemMenu();
        addSubComponents();
    }

    /** Colours and typeface; the values are those of {@code InternalFrame.*} in Metal. */
    protected void installDefaults() {
        selectedTitleColor = SELECTED_TITLE;
        selectedTextColor = TEXT;
        notSelectedTitleColor = UNSELECTED_TITLE;
        notSelectedTextColor = TEXT;
        Font font = getFont();
        if (font == null || font instanceof UIResource) {
            setFont(FONT);
        }
        setOpaque(false);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        if (propertyChangeListener == null) {
            propertyChangeListener = createPropertyChangeListener();
        }
        frame.addPropertyChangeListener(propertyChangeListener);
    }

    protected void uninstallListeners() {
        frame.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    /** The six; see the class note. */
    protected void createActions() {
        maximizeAction = new FrameAction(this, MAXIMIZE_CMD);
        iconifyAction = new FrameAction(this, ICONIFY_CMD);
        closeAction = new FrameAction(this, CLOSE_CMD);
        restoreAction = new FrameAction(this, RESTORE_CMD);
        moveAction = new FrameAction(this, MOVE_CMD);
        sizeAction = new FrameAction(this, SIZE_CMD);
    }

    /**
     * It switches each action on and off according to what the frame allows; see the class note.
     */
    protected void enableActions() {
        restoreAction.setEnabled(frame.isMaximum() || frame.isIcon());
        maximizeAction.setEnabled((frame.isMaximizable() && !frame.isMaximum() && !frame.isIcon())
                || (frame.isMaximizable() && frame.isIcon()));
        iconifyAction.setEnabled(frame.isIconifiable() && !frame.isIcon());
        closeAction.setEnabled(frame.isClosable());
        sizeAction.setEnabled(false);
        moveAction.setEnabled(false);
    }

    /** The system one; the one that drops down with the icon on the left. */
    protected JMenu createSystemMenu() {
        JMenu menu = new JMenu("    ");
        menu.setName("InternalFrameTitlePane.menuButton");
        return menu;
    }

    protected JMenuBar createSystemMenuBar() {
        menuBar = new SystemMenuBar(this);
        menuBar.setBorderPainted(false);
        return menuBar;
    }

    /** It builds the menu and hangs the items from it. */
    protected void assembleSystemMenu() {
        menuBar = createSystemMenuBar();
        windowMenu = createSystemMenu();
        menuBar.add(windowMenu);
        addSystemMenuItems(windowMenu);
        enableActions();
    }

    /** The seven elements; see the class note. */
    protected void addSystemMenuItems(JMenu systemMenu) {
        JMenuItem mi = systemMenu.add(restoreAction);
        mi.setMnemonic('R');
        mi = systemMenu.add(moveAction);
        mi.setMnemonic('M');
        mi = systemMenu.add(sizeAction);
        mi.setMnemonic('S');
        mi = systemMenu.add(iconifyAction);
        mi.setMnemonic('n');
        mi = systemMenu.add(maximizeAction);
        mi.setMnemonic('x');
        systemMenu.add(new javax.swing.JSeparator());
        mi = systemMenu.add(closeAction);
        mi.setMnemonic('C');
    }

    /** It drops down the system menu. */
    protected void showSystemMenu() {
        if (windowMenu != null) {
            windowMenu.doClick();
        }
    }

    /** The three buttons on the right. */
    protected void createButtons() {
        iconButton = new JButton();
        iconButton.setName("InternalFrameTitlePane.iconifyButton");
        iconButton.setFocusPainted(false);
        iconButton.setOpaque(false);
        iconButton.addActionListener(new ActionTrigger(iconifyAction));

        maxButton = new JButton();
        maxButton.setName("InternalFrameTitlePane.maximizeButton");
        maxButton.setFocusPainted(false);
        maxButton.setOpaque(false);
        maxButton.addActionListener(new ActionTrigger(maximizeAction));

        closeButton = new JButton();
        closeButton.setName("InternalFrameTitlePane.closeButton");
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(false);
        closeButton.addActionListener(new ActionTrigger(closeAction));

        setButtonIcons();
    }

    /**
     * It gives the buttons the icon that corresponds to the state.
     *
     * <p>The maximize one changes for the restore one when the frame is already maximized, and the
     * minimize one for the restore one when it is turned into an icon. With no icons -- see the
     * class note -- there is nothing to set, and the buttons are left empty.
     */
    protected void setButtonIcons() {
        if (frame.isIcon()) {
            if (iconButton != null) {
                iconButton.setIcon(minIcon);
            }
            if (maxButton != null) {
                maxButton.setIcon(maxIcon);
            }
        } else if (frame.isMaximum()) {
            if (iconButton != null) {
                iconButton.setIcon(iconIcon);
            }
            if (maxButton != null) {
                maxButton.setIcon(minIcon);
            }
        } else {
            if (iconButton != null) {
                iconButton.setIcon(iconIcon);
            }
            if (maxButton != null) {
                maxButton.setIcon(maxIcon);
            }
        }
        if (closeButton != null) {
            closeButton.setIcon(closeIcon);
        }
    }

    /** It hangs the menu and the buttons. */
    protected void addSubComponents() {
        add(menuBar);
        add(iconButton);
        add(maxButton);
        add(closeButton);
    }

    protected LayoutManager createLayout() {
        return new Handler(this);
    }

    /** The bar's background: the colour that corresponds to whether the frame is chosen. */
    protected void paintTitleBackground(Graphics g) {
        Color color = frame.isSelected() ? selectedTitleColor : notSelectedTitleColor;
        g.setColor(color);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** The background and the title. */
    public void paintComponent(Graphics g) {
        paintTitleBackground(g);
        String title = frame.getTitle();
        if (title == null) {
            return;
        }
        Font f = getFont();
        g.setFont(f);
        FontMetrics fm = getFontMetrics(f);
        g.setColor(frame.isSelected() ? selectedTextColor : notSelectedTextColor);
        int baseline = (getHeight() + fm.getAscent() - fm.getLeading() - fm.getDescent()) / 2;
        int titleX = (menuBar == null) ? 2 : menuBar.getX() + menuBar.getWidth() + 2;
        int width = availableTitleWidth();
        g.drawString(getTitle(title, fm, width), titleX, baseline);
    }

    private int availableTitleWidth() {
        int used = 0;
        if (menuBar != null) {
            used += menuBar.getWidth();
        }
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof JButton) {
                used += c.getWidth();
            }
        }
        return Math.max(0, getWidth() - used - 4);
    }

    /** The whole title if it fits, and if not cut with dots; see the class note. */
    protected String getTitle(String text, FontMetrics fm, int availTextWidth) {
        if (text == null || text.equals("")) {
            return "";
        }
        int textWidth = fm.stringWidth(text);
        if (textWidth <= availTextWidth) {
            return text;
        }
        String clipString = "...";
        int totalWidth = fm.stringWidth(clipString);
        int nChars;
        for (nChars = 0; nChars < text.length(); nChars++) {
            totalWidth += fm.charWidth(text.charAt(nChars));
            if (totalWidth > availTextWidth) {
                break;
            }
        }
        return text.substring(0, nChars) + clipString;
    }

    /**
     * It sends the frame the notice that it is closing.
     *
     * <p>It goes as an event and not as a direct call because the program may veto it: whoever
     * listens to {@code internalFrameClosing} has the chance to ask "do you want to save?"
     * first.
     */
    protected void postClosingEvent(JInternalFrame frame) {
        InternalFrameEvent e = new InternalFrameEvent(frame,
                InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        try {
            java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
        } catch (Exception ex) {
            // With no event queue -- headless -- the notice is lost; closing goes on working.
        }
    }

    /**
     * The menu bar that contains the system menu.
     *
     * <p>It is a class of its own and not a bare {@code JMenuBar} because it has to paint no
     * border and not take the focus: in a title bar, the system menu is an icon, not a bar.
     */
    static class SystemMenuBar extends JMenuBar {

        private final BasicInternalFrameTitlePane bar;

        SystemMenuBar(BasicInternalFrameTitlePane bar) {
            this.bar = bar;
        }

        public boolean isFocusTraversable() {
            return false;
        }

        public void requestFocus() {
        }

        public boolean isOpaque() {
            return true;
        }
    }

    /** Each of the six commands; the name says which. */
    private static class FrameAction extends AbstractAction {

        private final BasicInternalFrameTitlePane bar;
        private final String command;

        FrameAction(BasicInternalFrameTitlePane bar, String command) {
            super(command);
            this.bar = bar;
            this.command = command;
        }

        public void actionPerformed(ActionEvent e) {
            JInternalFrame frame = bar.frame;
            try {
                if (CLOSE_CMD.equals(command)) {
                    if (frame.isClosable()) {
                        bar.postClosingEvent(frame);
                        frame.doDefaultCloseAction();
                    }
                } else if (ICONIFY_CMD.equals(command)) {
                    if (frame.isIconifiable() && !frame.isIcon()) {
                        frame.setIcon(true);
                    }
                } else if (MAXIMIZE_CMD.equals(command)) {
                    if (frame.isMaximizable()) {
                        if (frame.isIcon()) {
                            frame.setIcon(false);
                        }
                        if (!frame.isMaximum()) {
                            frame.setMaximum(true);
                        }
                    }
                } else if (RESTORE_CMD.equals(command)) {
                    if (frame.isIcon()) {
                        frame.setIcon(false);
                    } else if (frame.isMaximum()) {
                        frame.setMaximum(false);
                    }
                }
                // Move and resize: see the class note.
            } catch (PropertyVetoException ex) {
                // Somebody said no. It is a valid answer, not an error.
            }
        }
    }

    /** The bridge between a button and its action. */
    private static class ActionTrigger implements java.awt.event.ActionListener {

        private final Action action;

        ActionTrigger(Action action) {
            this.action = action;
        }

        public void actionPerformed(ActionEvent e) {
            if (action.isEnabled()) {
                action.actionPerformed(e);
            }
        }
    }

    /**
     * The layout and the property listener.
     *
     * <p>The menu on the left, the three buttons on the right in reverse order, and the title
     * keeps what is left over. There is no ready-made layout that does that: the title is not a
     * component, it is what is painted in the gap.
     */
    private static class Handler implements LayoutManager, PropertyChangeListener {

        private final BasicInternalFrameTitlePane bar;

        Handler(BasicInternalFrameTitlePane bar) {
            this.bar = bar;
        }

        public void addLayoutComponent(String name, Component c) {
        }

        public void removeLayoutComponent(Component c) {
        }

        public Dimension preferredLayoutSize(Container c) {
            return minimumLayoutSize(c);
        }

        public Dimension minimumLayoutSize(Container c) {
            int height = 0;
            int width = 0;
            if (bar.menuBar != null) {
                Dimension d = bar.menuBar.getPreferredSize();
                width += d.width;
                height = Math.max(height, d.height);
            }
            JButton[] buttons = {bar.iconButton, bar.maxButton, bar.closeButton};
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i] != null) {
                    Dimension d = buttons[i].getPreferredSize();
                    width += d.width;
                    height = Math.max(height, d.height);
                }
            }
            FontMetrics fm = bar.getFontMetrics(bar.getFont());
            height = Math.max(height, fm.getHeight());
            // The title's gap: whatever the text needs, with a reasonable cap.
            String t = bar.frame.getTitle();
            if (t != null) {
                width += Math.min(fm.stringWidth(t), 100);
            }
            Insets in = bar.getInsets();
            return new Dimension(width + in.left + in.right, height + in.top + in.bottom);
        }

        public void layoutContainer(Container c) {
            Insets in = bar.getInsets();
            int w = bar.getWidth() - in.left - in.right;
            int h = bar.getHeight() - in.top - in.bottom;
            int x = in.left;
            if (bar.menuBar != null) {
                int mw = bar.menuBar.getPreferredSize().width;
                bar.menuBar.setBounds(x, in.top, mw, h);
                x += mw;
            }
            int right = in.left + w;
            JButton[] buttons = {bar.closeButton, bar.maxButton, bar.iconButton};
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i] == null) {
                    continue;
                }
                int bw = buttons[i].getPreferredSize().width;
                right -= bw;
                buttons[i].setBounds(right, in.top, bw, h);
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if (JInternalFrame.IS_SELECTED_PROPERTY.equals(prop)
                    || JInternalFrame.IS_MAXIMUM_PROPERTY.equals(prop)
                    || JInternalFrame.IS_ICON_PROPERTY.equals(prop)
                    || JInternalFrame.IS_CLOSED_PROPERTY.equals(prop)
                    || JInternalFrame.TITLE_PROPERTY.equals(prop)) {
                bar.enableActions();
                bar.setButtonIcons();
                bar.revalidate();
                bar.repaint();
            }
        }
    }
}
