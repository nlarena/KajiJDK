package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DesktopIconUI;
import javax.swing.plaf.InternalFrameUI;

/**
 * A window inside another.
 *
 * <h2>It is not a system window</h2>
 *
 * <p>It is a {@link JComponent} that is drawn with a frame, a title and buttons. It lives
 * inside a {@link JDesktopPane}; the operating system does not know it exists. Hence
 * {@link #getWarningString} returns null -- there is no real window to mark -- and a hundred of
 * these can be had without spending a hundred system windows.
 *
 * <h2>The changes of state may be vetoed</h2>
 *
 * <p>{@link #setClosed}, {@link #setIcon}, {@link #setMaximum} and {@link #setSelected} throw
 * {@link PropertyVetoException}. Before changing they tell the veto listeners, and anybody may
 * refuse. It is the mechanism with which an editor keeps a frame with unsaved changes from
 * being closed, without having to intercept the close button.
 *
 * <p>The state is changed <em>after</em> the veto has passed and <em>before</em> giving notice
 * of the change, so whoever listens to the change already sees the new value.
 *
 * <h2>Who moves the frame</h2>
 *
 * <p>Almost nobody calls {@code setBounds} on an internal frame: the look and feel asks the
 * desktop's {@link DesktopManager}, and that one decides. See {@link DesktopManager}'s note.
 *
 * <h2>The children go to the content</h2>
 *
 * <p>As in {@link JDialog}, inside there is a {@link JRootPane} and {@code add} redirects to
 * its content pane.
 */
public class JInternalFrame extends JComponent implements Accessible, WindowConstants,
        RootPaneContainer {

    private static final String uiClassID = "InternalFrameUI";

    /** The root pane; see the class note. */
    protected JRootPane rootPane;

    /** Whether adding redirects to the content. */
    protected boolean rootPaneCheckingEnabled = false;

    /** Whether it has a close button. */
    protected boolean closable;

    /** Whether it has already closed. */
    protected boolean isClosed;

    /** Whether it has a maximize button. */
    protected boolean maximizable;

    /** Whether it is maximized. */
    protected boolean isMaximum;

    /** Whether it has a minimize button. */
    protected boolean iconable;

    /** Whether it is turned into an icon. */
    protected boolean isIcon;

    /** Whether it can be resized. */
    protected boolean resizable;

    /** Whether it is the desktop's active frame. */
    protected boolean isSelected;

    /** The title's icon. */
    protected Icon frameIcon;

    /** The title. */
    protected String title;

    /** The icon that replaces it when it is minimized. */
    protected JDesktopIcon desktopIcon;

    public static final String CONTENT_PANE_PROPERTY = "contentPane";
    public static final String MENU_BAR_PROPERTY = "JMenuBar";
    public static final String TITLE_PROPERTY = "title";
    public static final String LAYERED_PANE_PROPERTY = "layeredPane";
    public static final String ROOT_PANE_PROPERTY = "rootPane";
    public static final String GLASS_PANE_PROPERTY = "glassPane";
    public static final String FRAME_ICON_PROPERTY = "frameIcon";
    public static final String IS_SELECTED_PROPERTY = "selected";
    public static final String IS_CLOSED_PROPERTY = "closed";
    public static final String IS_MAXIMUM_PROPERTY = "maximum";
    public static final String IS_ICON_PROPERTY = "icon";

    private int defaultCloseOperation = DISPOSE_ON_CLOSE;
    private Rectangle normalBounds = null;
    private Component lastFocusOwner;
    private Cursor lastCursor;
    private boolean opened = false;

    /** A frame with no title, which neither closes nor grows nor shrinks nor resizes. */
    public JInternalFrame() {
        this("", false, false, false, false);
    }

    /** With that title. */
    public JInternalFrame(String title) {
        this(title, false, false, false, false);
    }

    /** With that title, and resizable or not. */
    public JInternalFrame(String title, boolean resizable) {
        this(title, resizable, false, false, false);
    }

    public JInternalFrame(String title, boolean resizable, boolean closable) {
        this(title, resizable, closable, false, false);
    }

    public JInternalFrame(String title, boolean resizable, boolean closable,
            boolean maximizable) {
        this(title, resizable, closable, maximizable, false);
    }

    /** With that title and those four capabilities. */
    public JInternalFrame(String title, boolean resizable, boolean closable,
            boolean maximizable, boolean iconifiable) {
        setRootPane(createRootPane());
        setLayout(new java.awt.BorderLayout());
        this.title = title;
        this.resizable = resizable;
        this.closable = closable;
        this.maximizable = maximizable;
        this.iconable = iconifiable;
        isClosed = false;
        isSelected = false;
        isIcon = false;
        isMaximum = false;
        setVisible(false);
        setRootPaneCheckingEnabled(true);
        desktopIcon = new JDesktopIcon(this);
        updateUI();
    }

    protected JRootPane createRootPane() {
        return new JRootPane();
    }

    public InternalFrameUI getUI() {
        return (InternalFrameUI) ui;
    }

    public void setUI(InternalFrameUI ui) {
        boolean checkingEnabled = isRootPaneCheckingEnabled();
        try {
            setRootPaneCheckingEnabled(false);
            super.setUI(ui);
        } finally {
            setRootPaneCheckingEnabled(checkingEnabled);
        }
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }

    protected void setRootPaneCheckingEnabled(boolean enabled) {
        rootPaneCheckingEnabled = enabled;
    }

    /**
     * It adds to the content, not to the frame.
     *
     * @throws Error if the root pane is added with the redirection switched on.
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().add(comp, constraints, index);
        } else {
            super.addImpl(comp, constraints, index);
        }
    }

    /** It removes from the content, unless it is the root pane. */
    public void remove(Component comp) {
        int oldCount = getComponentCount();
        super.remove(comp);
        if (oldCount == getComponentCount()) {
            getContentPane().remove(comp);
        }
    }

    /** It gives the layout to the content, not to the frame. */
    public void setLayout(LayoutManager manager) {
        if (isRootPaneCheckingEnabled()) {
            getContentPane().setLayout(manager);
        } else {
            super.setLayout(manager);
        }
    }

    /**
     * @deprecated Use {@link #getJMenuBar}.
     */
    @Deprecated
    public JMenuBar getMenuBar() {
        return getRootPane().getMenuBar();
    }

    public JMenuBar getJMenuBar() {
        return getRootPane().getJMenuBar();
    }

    /**
     * @deprecated Use {@link #setJMenuBar}.
     */
    @Deprecated
    public void setMenuBar(JMenuBar m) {
        JMenuBar oldValue = getMenuBar();
        getRootPane().setJMenuBar(m);
        firePropertyChange(MENU_BAR_PROPERTY, oldValue, m);
    }

    public void setJMenuBar(JMenuBar m) {
        JMenuBar oldValue = getMenuBar();
        getRootPane().setJMenuBar(m);
        firePropertyChange(MENU_BAR_PROPERTY, oldValue, m);
    }

    public Container getContentPane() {
        return getRootPane().getContentPane();
    }

    /**
     * @throws java.awt.IllegalComponentStateException if it is null.
     */
    public void setContentPane(Container c) {
        Container oldValue = getContentPane();
        getRootPane().setContentPane(c);
        firePropertyChange(CONTENT_PANE_PROPERTY, oldValue, c);
    }

    public JLayeredPane getLayeredPane() {
        return getRootPane().getLayeredPane();
    }

    public void setLayeredPane(JLayeredPane layered) {
        JLayeredPane oldValue = getLayeredPane();
        getRootPane().setLayeredPane(layered);
        firePropertyChange(LAYERED_PANE_PROPERTY, oldValue, layered);
    }

    public Component getGlassPane() {
        return getRootPane().getGlassPane();
    }

    public void setGlassPane(Component glass) {
        Component oldValue = getGlassPane();
        getRootPane().setGlassPane(glass);
        firePropertyChange(GLASS_PANE_PROPERTY, oldValue, glass);
    }

    public JRootPane getRootPane() {
        return rootPane;
    }

    protected void setRootPane(JRootPane root) {
        if (rootPane != null) {
            remove(rootPane);
        }
        JRootPane oldValue = getRootPane();
        rootPane = root;
        if (rootPane != null) {
            boolean checkingEnabled = isRootPaneCheckingEnabled();
            try {
                // Switched off while the root pane is added: otherwise, it would redirect to
                // itself.
                setRootPaneCheckingEnabled(false);
                add(rootPane, java.awt.BorderLayout.CENTER);
            } finally {
                setRootPaneCheckingEnabled(checkingEnabled);
            }
        }
        firePropertyChange(ROOT_PANE_PROPERTY, oldValue, root);
    }

    public void setClosable(boolean b) {
        boolean oldValue = closable;
        closable = b;
        firePropertyChange("closable", oldValue, b);
    }

    public boolean isClosable() {
        return closable;
    }

    public boolean isClosed() {
        return isClosed;
    }

    /**
     * It closes or reopens the frame.
     *
     * <p>Closing hides it and destroys it. Reopening a closed one does not show it again by
     * itself: it has to be added to the desktop again.
     *
     * @throws PropertyVetoException if some listener objects.
     */
    public void setClosed(boolean b) throws PropertyVetoException {
        if (isClosed == b) {
            return;
        }
        Boolean oldValue = isClosed ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        if (b) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        }
        fireVetoableChange(IS_CLOSED_PROPERTY, oldValue, newValue);
        isClosed = b;
        if (isClosed) {
            setVisible(false);
        }
        firePropertyChange(IS_CLOSED_PROPERTY, oldValue, newValue);
        if (isClosed) {
            dispose();
        } else if (!opened) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_OPENED);
            opened = true;
        }
    }

    public void setResizable(boolean b) {
        boolean oldValue = resizable;
        resizable = b;
        firePropertyChange("resizable", oldValue, b);
    }

    public boolean isResizable() {
        // A maximized frame takes up everything: letting it resize would give it nowhere to grow.
        return isMaximum ? false : resizable;
    }

    public void setIconifiable(boolean b) {
        boolean oldValue = iconable;
        iconable = b;
        firePropertyChange("iconable", oldValue, b);
    }

    public boolean isIconifiable() {
        return iconable;
    }

    public boolean isIcon() {
        return isIcon;
    }

    /**
     * It turns the frame into an icon, or gives it back.
     *
     * @throws PropertyVetoException if some listener objects.
     */
    public void setIcon(boolean b) throws PropertyVetoException {
        if (isIcon == b) {
            return;
        }
        Boolean oldValue = isIcon ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        fireVetoableChange(IS_ICON_PROPERTY, oldValue, newValue);
        isIcon = b;
        firePropertyChange(IS_ICON_PROPERTY, oldValue, newValue);
        if (b) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_ICONIFIED);
        } else {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED);
        }
    }

    public void setMaximizable(boolean b) {
        boolean oldValue = maximizable;
        maximizable = b;
        firePropertyChange("maximizable", oldValue, b);
    }

    public boolean isMaximizable() {
        return maximizable;
    }

    public boolean isMaximum() {
        return isMaximum;
    }

    /**
     * It enlarges the frame to the whole desktop, or gives it back its size.
     *
     * @throws PropertyVetoException if some listener objects.
     */
    public void setMaximum(boolean b) throws PropertyVetoException {
        if (isMaximum == b) {
            return;
        }
        Boolean oldValue = isMaximum ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        fireVetoableChange(IS_MAXIMUM_PROPERTY, oldValue, newValue);
        // The state is changed before giving notice: whoever listens already sees the new value.
        isMaximum = b;
        firePropertyChange(IS_MAXIMUM_PROPERTY, oldValue, newValue);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String oldValue = this.title;
        this.title = title;
        firePropertyChange(TITLE_PROPERTY, oldValue, title);
    }

    /**
     * It activates or deactivates the frame.
     *
     * <p>In order to be activated it has to be visible on the screen -- or its icon, if it is
     * minimized. Activating a frame that is not seen would leave the desktop with an invisible
     * active frame and none of the visible ones lit. Deactivating, on the other hand, is always
     * possible.
     *
     * @throws PropertyVetoException if some listener objects.
     */
    public void setSelected(boolean selected) throws PropertyVetoException {
        if (selected && isSelected) {
            // It is already active, but the focus may be outside: it is brought back inside.
            restoreSubcomponentFocus();
            return;
        }
        if (isSelected == selected) {
            return;
        }
        if (selected) {
            boolean showing = isIcon ? desktopIcon.isShowing() : isShowing();
            if (!showing) {
                return;
            }
        }
        Boolean oldValue = isSelected ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = selected ? Boolean.TRUE : Boolean.FALSE;
        fireVetoableChange(IS_SELECTED_PROPERTY, oldValue, newValue);
        if (selected) {
            restoreSubcomponentFocus();
        }
        isSelected = selected;
        firePropertyChange(IS_SELECTED_PROPERTY, oldValue, newValue);
        if (isSelected) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_ACTIVATED);
        } else {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED);
        }
        repaint();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setFrameIcon(Icon icon) {
        Icon oldIcon = frameIcon;
        frameIcon = icon;
        firePropertyChange(FRAME_ICON_PROPERTY, oldIcon, icon);
    }

    public Icon getFrameIcon() {
        return frameIcon;
    }

    /**
     * It puts it in front of the others.
     *
     * <p>If it is turned into an icon it moves the icon, not the frame: it is what is seen.
     */
    public void moveToFront() {
        if (isIcon()) {
            if (getDesktopIcon().getParent() instanceof JLayeredPane) {
                JLayeredPane p = (JLayeredPane) getDesktopIcon().getParent();
                p.moveToFront(getDesktopIcon());
            }
        } else if (getParent() instanceof JLayeredPane) {
            JLayeredPane p = (JLayeredPane) getParent();
            p.moveToFront(this);
        }
    }

    /** It sends it behind the others. */
    public void moveToBack() {
        if (isIcon()) {
            if (getDesktopIcon().getParent() instanceof JLayeredPane) {
                JLayeredPane p = (JLayeredPane) getDesktopIcon().getParent();
                p.moveToBack(getDesktopIcon());
            }
        } else if (getParent() instanceof JLayeredPane) {
            JLayeredPane p = (JLayeredPane) getParent();
            p.moveToBack(this);
        }
    }

    /**
     * The cursor that was there before the look and feel changed it in order to resize.
     *
     * <p>On passing over an edge the look and feel sets a double arrow; when one leaves it has to
     * give back the previous one, and this is the one that keeps which it was.
     */
    public Cursor getLastCursor() {
        return lastCursor;
    }

    public void setCursor(Cursor cursor) {
        lastCursor = cursor;
        super.setCursor(cursor);
    }

    /** The desktop layer it is in. */
    public void setLayer(Integer layer) {
        if (getParent() != null && getParent() instanceof JLayeredPane) {
            JLayeredPane p = (JLayeredPane) getParent();
            p.setLayer(this, layer.intValue(), p.getPosition(this));
        } else {
            JLayeredPane.putLayer(this, layer.intValue());
            if (getParent() != null) {
                getParent().repaint();
            }
        }
    }

    /** The layer, given as an integer. */
    public void setLayer(int layer) {
        this.setLayer(Integer.valueOf(layer));
    }

    public int getLayer() {
        return JLayeredPane.getLayer(this);
    }

    /**
     * The desktop that contains it.
     *
     * <p>If it is turned into an icon the frame has no parent; then it is looked up from the
     * icon, which is the one that is set in the desktop.
     */
    public JDesktopPane getDesktopPane() {
        Container p = getParent();
        while (p != null && !(p instanceof JDesktopPane)) {
            p = p.getParent();
        }
        if (p == null) {
            p = getDesktopIcon().getParent();
            while (p != null && !(p instanceof JDesktopPane)) {
                p = p.getParent();
            }
        }
        return (JDesktopPane) p;
    }

    public void setDesktopIcon(JDesktopIcon d) {
        JDesktopIcon oldValue = getDesktopIcon();
        desktopIcon = d;
        firePropertyChange("desktopIcon", oldValue, d);
    }

    public JDesktopIcon getDesktopIcon() {
        return desktopIcon;
    }

    /**
     * The rectangle it took up before maximizing.
     *
     * <p>If there is none kept it returns the current one, which is the right one while it is not
     * maximized.
     */
    public Rectangle getNormalBounds() {
        if (normalBounds != null) {
            return normalBounds;
        }
        return getBounds();
    }

    public void setNormalBounds(Rectangle r) {
        normalBounds = r;
    }

    /** Who has the focus inside, or null if the frame is not active. */
    public Component getFocusOwner() {
        if (isSelected()) {
            return lastFocusOwner;
        }
        return null;
    }

    /** Who would have the focus if the frame were activated. */
    public Component getMostRecentFocusOwner() {
        if (isSelected()) {
            return getFocusOwner();
        }
        if (lastFocusOwner != null) {
            return lastFocusOwner;
        }
        return getContentPane();
    }

    /** It gives the focus back to whoever had it before. */
    public void restoreSubcomponentFocus() {
        lastFocusOwner = getMostRecentFocusOwner();
        if (lastFocusOwner != null) {
            lastFocusOwner.requestFocus();
        }
    }

    /**
     * @deprecated It is called by itself; use {@code setBounds}.
     */
    @Deprecated
    public void reshape(int x, int y, int width, int height) {
        super.reshape(x, y, width, height);
        validate();
        repaint();
    }

    public void addInternalFrameListener(InternalFrameListener l) {
        listenerList.add(InternalFrameListener.class, l);
    }

    public void removeInternalFrameListener(InternalFrameListener l) {
        listenerList.remove(InternalFrameListener.class, l);
    }

    public InternalFrameListener[] getInternalFrameListeners() {
        return listenerList.getListeners(InternalFrameListener.class);
    }

    /**
     * It gives notice of an internal frame event.
     *
     * <p>The identifier decides which method of the listener is called.
     */
    protected void fireInternalFrameEvent(int id) {
        Object[] listeners = listenerList.getListenerList();
        InternalFrameEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == InternalFrameListener.class) {
                if (e == null) {
                    e = new InternalFrameEvent(this, id);
                }
                InternalFrameListener l = (InternalFrameListener) listeners[i + 1];
                distribute(l, e, id);
            }
        }
    }

    /**
     * It gives the event to the method that applies.
     *
     * <p>It is a chain of comparisons and not a {@code switch} because the identifiers are
     * constants of another class.
     */
    private void distribute(InternalFrameListener l, InternalFrameEvent e, int id) {
        if (id == InternalFrameEvent.INTERNAL_FRAME_OPENED) {
            l.internalFrameOpened(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_CLOSING) {
            l.internalFrameClosing(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_CLOSED) {
            l.internalFrameClosed(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_ICONIFIED) {
            l.internalFrameIconified(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED) {
            l.internalFrameDeiconified(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_ACTIVATED) {
            l.internalFrameActivated(e);
        } else if (id == InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED) {
            l.internalFrameDeactivated(e);
        }
    }

    /**
     * What happens when the user presses the close button.
     *
     * <p>It gives notice that it is closing and afterwards does what
     * {@link #setDefaultCloseOperation} says. Giving notice first is what gives the listener the
     * chance to change the operation before it is carried out.
     */
    public void doDefaultCloseAction() {
        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        if (defaultCloseOperation == DO_NOTHING_ON_CLOSE) {
            return;
        }
        if (defaultCloseOperation == HIDE_ON_CLOSE) {
            setVisible(false);
            if (isSelected()) {
                try {
                    setSelected(false);
                } catch (PropertyVetoException pve) {
                    // Vetoing the deactivation does not prevent hiding: it has already hidden.
                }
            }
            return;
        }
        if (defaultCloseOperation == DISPOSE_ON_CLOSE) {
            try {
                fireVetoableChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
                isClosed = true;
                setVisible(false);
                firePropertyChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
                dispose();
            } catch (PropertyVetoException pve) {
                // Somebody objected: the frame is left as it was.
            }
        }
    }

    /**
     * What to do on closing.
     *
     * <p>Unlike {@link JDialog}, by default it destroys: a hidden internal frame would go on
     * taking up room in the desktop.
     */
    public void setDefaultCloseOperation(int operation) {
        this.defaultCloseOperation = operation;
    }

    public int getDefaultCloseOperation() {
        return defaultCloseOperation;
    }

    /** It shrinks it to the size its children ask for. */
    public void pack() {
        try {
            if (isIcon()) {
                setIcon(false);
            } else if (isMaximum()) {
                setMaximum(false);
            }
        } catch (PropertyVetoException e) {
            // If it cannot be deiconified or unmaximized there is no point in measuring: it is left
            // as it is.
            return;
        }
        setSize(getPreferredSize());
        validate();
    }

    /**
     * @deprecated Use {@code setVisible(true)}.
     */
    @Deprecated
    public void show() {
        if (isVisible()) {
            return;
        }
        if (!opened) {
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_OPENED);
            opened = true;
        }
        // The icon is born hidden; it is switched on now so that it appears on minimizing.
        getDesktopIcon().setVisible(true);
        toFront();
        super.show();
        try {
            setSelected(true);
        } catch (PropertyVetoException pve) {
            // Its not being able to be activated does not prevent showing it.
        }
    }

    /**
     * @deprecated Use {@code setVisible(false)}.
     */
    @Deprecated
    public void hide() {
        if (isIcon()) {
            getDesktopIcon().setVisible(false);
        }
        super.hide();
    }

    /** It takes it off the desktop and gives notice that it closed. */
    public void dispose() {
        if (isVisible()) {
            setVisible(false);
        }
        if (isSelected()) {
            try {
                setSelected(false);
            } catch (PropertyVetoException pve) {
                // It is destroyed all the same.
            }
        }
        if (!isClosed) {
            firePropertyChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
            isClosed = true;
        }
        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSED);
    }

    public void toFront() {
        moveToFront();
    }

    public void toBack() {
        moveToBack();
    }

    /**
     * It does nothing: an internal frame is always the root of its focus cycle.
     *
     * <p>Tabbing inside must not take the focus to the desktop.
     */
    public final void setFocusCycleRoot(boolean value) {
    }

    /** Always true; see {@link #setFocusCycleRoot}. */
    public final boolean isFocusCycleRoot() {
        return true;
    }

    /** Always null: there is no cycle above. */
    public final Container getFocusCycleRootAncestor() {
        return null;
    }

    /** Always null: it is not a system window, there is nothing to warn about. */
    public final String getWarningString() {
        return null;
    }

    protected String paramString() {
        return super.paramString();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * The icon that replaces a minimized internal frame.
     *
     * <p>It is a separate component and not a way of drawing the frame: while the frame is
     * minimized it is the icon that is set in the desktop, and the frame has no parent. Hence
     * {@link JInternalFrame#getDesktopPane} consults it.
     */
    public static class JDesktopIcon extends JComponent implements Accessible {

        private JInternalFrame internalFrame;

        /** That frame's icon. */
        public JDesktopIcon(JInternalFrame f) {
            setVisible(false);
            setInternalFrame(f);
            updateUI();
        }

        public DesktopIconUI getUI() {
            return (DesktopIconUI) ui;
        }

        public void setUI(DesktopIconUI ui) {
            super.setUI(ui);
        }

        public JInternalFrame getInternalFrame() {
            return internalFrame;
        }

        public void setInternalFrame(JInternalFrame f) {
            internalFrame = f;
        }

        /** The desktop, asking the frame for it. */
        public JDesktopPane getDesktopPane() {
            if (getInternalFrame() != null) {
                return getInternalFrame().getDesktopPane();
            }
            return null;
        }

        public void updateUI() {
        }

        public String getUIClassID() {
            return "DesktopIconUI";
        }

        public AccessibleContext getAccessibleContext() {
            return accessibleContext;
        }
    }
}
