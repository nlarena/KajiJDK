package java.awt;

import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * A window with a border, a title bar and, if given one, a menu bar.
 *
 * <p>It is the main window of a desktop application. What it adds over {@link Window} is everything
 * the desktop draws around the content: the frame, the title, the minimise and close buttons.
 *
 * <p>The **extended state** is a bit mask and not a value, and that matters: a window can be
 * maximised horizontally and not vertically, or minimised **and** maximised at once — when it is
 * restored it comes back maximised. A single value could not express that.
 *
 * <p>The insets of {@link Container#getInsets} are, in a frame, the thickness of that decoration.
 * Here they are zero: with no desktop there is no frame taking up room.
 */
public class Frame extends Window implements MenuContainer {

    private static final long serialVersionUID = 2673458971256075116L;

    /** Neither minimized nor maximized. */
    public static final int NORMAL = 0;

    /** Minimized. */
    public static final int ICONIFIED = 1;

    /** Maximised across. */
    public static final int MAXIMIZED_HORIZ = 2;

    /** Maximised down. */
    public static final int MAXIMIZED_VERT = 4;

    /** Maximised in both directions. */
    public static final int MAXIMIZED_BOTH = MAXIMIZED_VERT | MAXIMIZED_HORIZ;

    /**
     * The usual cursor.
     *
     * @deprecated it is from the 1.0 model. Use {@link Cursor#DEFAULT_CURSOR}.
     */
    @Deprecated
    public static final int DEFAULT_CURSOR = Cursor.DEFAULT_CURSOR;

    /**
     * The crosshair.
     *
     * @deprecated use {@link Cursor#CROSSHAIR_CURSOR}.
     */
    @Deprecated
    public static final int CROSSHAIR_CURSOR = Cursor.CROSSHAIR_CURSOR;

    /**
     * The text bar.
     *
     * @deprecated use {@link Cursor#TEXT_CURSOR}.
     */
    @Deprecated
    public static final int TEXT_CURSOR = Cursor.TEXT_CURSOR;

    /**
     * The waiting clock.
     *
     * @deprecated use {@link Cursor#WAIT_CURSOR}.
     */
    @Deprecated
    public static final int WAIT_CURSOR = Cursor.WAIT_CURSOR;

    /**
     * Resizing from the bottom left edge.
     *
     * @deprecated use {@link Cursor#SW_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int SW_RESIZE_CURSOR = Cursor.SW_RESIZE_CURSOR;

    /**
     * From the bottom right edge.
     *
     * @deprecated use {@link Cursor#SE_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int SE_RESIZE_CURSOR = Cursor.SE_RESIZE_CURSOR;

    /**
     * From the top left edge.
     *
     * @deprecated use {@link Cursor#NW_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int NW_RESIZE_CURSOR = Cursor.NW_RESIZE_CURSOR;

    /**
     * From the top right edge.
     *
     * @deprecated use {@link Cursor#NE_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int NE_RESIZE_CURSOR = Cursor.NE_RESIZE_CURSOR;

    /**
     * From the top edge.
     *
     * @deprecated use {@link Cursor#N_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int N_RESIZE_CURSOR = Cursor.N_RESIZE_CURSOR;

    /**
     * From the bottom edge.
     *
     * @deprecated use {@link Cursor#S_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int S_RESIZE_CURSOR = Cursor.S_RESIZE_CURSOR;

    /**
     * From the left edge.
     *
     * @deprecated use {@link Cursor#W_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int W_RESIZE_CURSOR = Cursor.W_RESIZE_CURSOR;

    /**
     * From the right edge.
     *
     * @deprecated use {@link Cursor#E_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int E_RESIZE_CURSOR = Cursor.E_RESIZE_CURSOR;

    /**
     * The hand.
     *
     * @deprecated use {@link Cursor#HAND_CURSOR}.
     */
    @Deprecated
    public static final int HAND_CURSOR = Cursor.HAND_CURSOR;

    /**
     * The moving cross.
     *
     * @deprecated use {@link Cursor#MOVE_CURSOR}.
     */
    @Deprecated
    public static final int MOVE_CURSOR = Cursor.MOVE_CURSOR;

    private static final List<Frame> allFrames = new ArrayList<Frame>();

    private String title = "Untitled";
    private MenuBar menuBar;
    private boolean resizable = true;
    private boolean undecorated;
    private int state = NORMAL;
    private Rectangle maximizedBounds;

    /**
     * A frame with no title.
     *
     * @throws HeadlessException if there is no screen
     */
    public Frame() throws HeadlessException {
        this("");
    }

    /**
     * With that graphics configuration.
     *
     * @throws IllegalArgumentException if the configuration is not a screen one
     */
    public Frame(GraphicsConfiguration gc) {
        this("", gc);
    }

    /**
     * With that title.
     *
     * @throws HeadlessException if there is no screen
     */
    public Frame(String title) throws HeadlessException {
        this(title, null);
    }

    /**
     * With a title and a graphics configuration.
     *
     * @throws IllegalArgumentException if the configuration is not a screen one
     */
    public Frame(String title, GraphicsConfiguration gc) {
        super(null, gc);
        this.title = title;
        synchronized (allFrames) {
            allFrames.add(this);
        }
    }

    /** Notifies that it can be shown, and tells the menu bar. */
    public void addNotify() {
        synchronized (this.getTreeLock()) {
            if (this.menuBar != null) {
                this.menuBar.addNotify();
            }
            super.addNotify();
        }
    }

    /** Notifies that it can no longer be shown. */
    public void removeNotify() {
        synchronized (this.getTreeLock()) {
            if (this.menuBar != null) {
                this.menuBar.removeNotify();
            }
            super.removeNotify();
        }
    }

    /** The text of the title bar. */
    public String getTitle() {
        return this.title;
    }

    /** Changes its title; a `null` is taken as empty. */
    public void setTitle(String title) {
        String old = this.title;
        synchronized (this) {
            this.title = title == null ? "" : title;
        }
        this.firePropertyChange("title", old, this.title);
    }

    /**
     * The window's icon.
     *
     * @return the first of {@link Window#getIconImages}, or `null` if there is none
     */
    public Image getIconImage() {
        java.util.List<Image> l = this.getIconImages();
        if (l.isEmpty()) {
            return null;
        }
        return l.get(0);
    }

    /** Gives it an icon. */
    public void setIconImage(Image image) {
        super.setIconImage(image);
    }

    /**
     * The menu bar.
     *
     * @return the bar, or `null` if it has none
     */
    public MenuBar getMenuBar() {
        return this.menuBar;
    }

    /**
     * Gives it a menu bar.
     *
     * <p>It changes the room available for the content, so it invalidates the frame.
     */
    public void setMenuBar(MenuBar mb) {
        synchronized (this.getTreeLock()) {
            if (this.menuBar == mb) {
                return;
            }
            if (this.menuBar != null) {
                this.menuBar.setParent(null);
            }
            this.menuBar = mb;
            if (mb != null) {
                if (mb.getParent() != null) {
                    ((MenuContainer) mb.getParent()).remove(mb);
                }
                mb.setParent(this);
            }
            this.invalidate();
        }
    }

    /** Whether the user can change its size. */
    public boolean isResizable() {
        return this.resizable;
    }

    /** Declares whether the user can change its size. */
    public void setResizable(boolean resizable) {
        boolean old;
        synchronized (this) {
            old = this.resizable;
            this.resizable = resizable;
        }
        this.firePropertyChange("resizable", old, resizable);
    }

    /**
     * Minimises or restores.
     *
     * @deprecated it can only express minimised and normal. Use {@link #setExtendedState}.
     */
    @Deprecated
    public synchronized void setState(int state) {
        int fresh = this.state;
        if (state == ICONIFIED) {
            fresh = fresh | ICONIFIED;
        } else {
            fresh = fresh & ~ICONIFIED;
        }
        this.setExtendedState(fresh);
    }

    /**
     * Whether it is minimised.
     *
     * @deprecated it does not see the maximisation states. Use {@link #getExtendedState}.
     */
    @Deprecated
    public synchronized int getState() {
        return (this.state & ICONIFIED) != 0 ? ICONIFIED : NORMAL;
    }

    /**
     * Changes the state of the window.
     *
     * <p>It is a mask: {@link #ICONIFIED} can be combined with the maximisation ones, and that
     * means it will come back maximised when it is restored.
     */
    public void setExtendedState(int state) {
        synchronized (this) {
            this.state = state;
        }
    }

    /** The state, as a bit mask. */
    public int getExtendedState() {
        return this.state;
    }

    /**
     * How far it maximises.
     *
     * @param bounds the rectangle, or `null` for the whole screen
     */
    public synchronized void setMaximizedBounds(Rectangle bounds) {
        this.maximizedBounds = bounds;
    }

    /**
     * How far it maximises.
     *
     * @return the rectangle, or `null` if it is the whole screen
     */
    public Rectangle getMaximizedBounds() {
        return this.maximizedBounds;
    }

    /**
     * Takes its decoration away.
     *
     * @throws IllegalComponentStateException if the window can already be shown: the decoration is
     *     put on by the desktop when it creates it, and afterwards it is too late
     */
    public void setUndecorated(boolean undecorated) {
        synchronized (this.getTreeLock()) {
            if (this.isDisplayable()) {
                throw new IllegalComponentStateException(
                        "The frame is displayable.");
            }
            this.undecorated = undecorated;
        }
    }

    /** Whether it has no decoration. */
    public boolean isUndecorated() {
        return this.undecorated;
    }

    /**
     * Changes its opacity.
     *
     * @throws IllegalComponentStateException if the window is decorated: the desktop cannot make
     *     translucent a frame that it draws itself
     */
    public void setOpacity(float opacity) {
        synchronized (this.getTreeLock()) {
            if (opacity < 1.0f && !this.isUndecorated()) {
                throw new IllegalComponentStateException("The frame is decorated");
            }
            super.setOpacity(opacity);
        }
    }

    /**
     * Clips its shape.
     *
     * @throws IllegalComponentStateException if the window is decorated, for the same reason
     */
    public void setShape(Shape shape) {
        synchronized (this.getTreeLock()) {
            if (shape != null && !this.isUndecorated()) {
                throw new IllegalComponentStateException("The frame is decorated");
            }
            super.setShape(shape);
        }
    }

    /**
     * Changes its background.
     *
     * @throws IllegalComponentStateException if transparency is asked for on a decorated window
     */
    public void setBackground(Color bgColor) {
        synchronized (this.getTreeLock()) {
            if (bgColor != null && bgColor.getAlpha() < 255 && !this.isUndecorated()) {
                throw new IllegalComponentStateException("The frame is decorated");
            }
            super.setBackground(bgColor);
        }
    }

    /** Takes its menu bar away if that is what is passed in. */
    public void remove(MenuComponent m) {
        if (m == this.menuBar) {
            this.setMenuBar(null);
        } else {
            super.remove(m);
        }
    }

    /**
     * Gives it a cursor by number.
     *
     * @deprecated it is from the 1.0 model. Use {@link Component#setCursor(Cursor)}.
     */
    @Deprecated
    public void setCursor(int cursorType) {
        if (cursorType < DEFAULT_CURSOR || cursorType > MOVE_CURSOR) {
            throw new IllegalArgumentException("illegal cursor type");
        }
        this.setCursor(Cursor.getPredefinedCursor(cursorType));
    }

    /**
     * Which cursor it has, by number.
     *
     * @deprecated it is from the 1.0 model. Use {@link Component#getCursor}.
     */
    @Deprecated
    public int getCursorType() {
        return this.getCursor().getType();
    }

    /** Every frame of this application. */
    public static Frame[] getFrames() {
        synchronized (allFrames) {
            return allFrames.toArray(new Frame[allFrames.size()]);
        }
    }

    protected String paramString() {
        String s = super.paramString();
        if (this.title != null) {
            s = s + ",title=" + this.title;
        }
        if (this.resizable) {
            s = s + ",resizable";
        }
        return s;
    }

    /** The accessibility information of this frame. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTFrame();
        }
        return this.accessibleContext;
    }

    /** The accessibility of a frame. */
    protected class AccessibleAWTFrame extends AccessibleAWTWindow {

        /** For the subclasses. */
        protected AccessibleAWTFrame() {
        }

        /** It is a frame. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.FRAME;
        }

        /** The ones of a window, plus more if it can be resized and if it is minimised. */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (Frame.this.isResizable()) {
                s.add(AccessibleState.RESIZABLE);
            }
            if ((Frame.this.getExtendedState() & ICONIFIED) != 0) {
                s.add(AccessibleState.ICONIFIED);
            }
            return s;
        }
    }
}
