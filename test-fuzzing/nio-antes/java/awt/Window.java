package java.awt;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.awt.im.InputContext;
import java.awt.image.BufferStrategy;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * A window with no border and no title bar: the topmost container of the tree.
 *
 * <p>It is the root of everything that gets shown. A {@link Frame} is a window with decoration and
 * a {@link Dialog} is a subordinate window; this one, as it is, is the one good for a popup menu or
 * a splash screen — whatever has to appear with no frame.
 *
 * <p>The **ownership** of one window over another is what ties their fates: a window with an owner
 * is minimised, closed and brought to the front with it. It is what keeps a dialog from being left
 * orphaned and floating when the window that opened it closes.
 *
 * <p>It is a focus cycle root irrevocably —{@link #setFocusCycleRoot} does nothing— because the
 * focus cannot leave a window with the tab key: there is nowhere to go.
 *
 * <p><strong>None of this appears on a screen.</strong> This library ships no windowing system.
 * Unlike the headless mode of the real JDK, which **refuses to build** a window, here it is built:
 * refusing would leave the whole component tree dead, and this way the hierarchy, the layout, the
 * listeners and the event dispatching can be used and tested. What does not happen is the
 * appearing, and the methods that depend on it say so one by one.
 */
public class Window extends Container implements Accessible {

    private static final long serialVersionUID = 4497834738069338734L;

    /** What a window is used for; the desktop decorates it according to this. */
    public static enum Type {

        /** An ordinary window. */
        NORMAL,

        /** A tool palette: smaller title bar, does not show in the taskbar. */
        UTILITY,

        /** A popup menu or a tooltip: no decoration and short-lived. */
        POPUP
    }

    private static final List<Window> allWindows = new ArrayList<Window>();

    private final Window owner;
    private final List<Window> ownedWindows = new ArrayList<Window>();
    private List<Image> icons = new ArrayList<Image>();
    private Dialog.ModalExclusionType modalExclusionType =
            Dialog.ModalExclusionType.NO_EXCLUDE;
    private Type type = Type.NORMAL;
    private boolean alwaysOnTop;
    private boolean focusableWindowState = true;
    private boolean autoRequestFocus = true;
    private boolean locationByPlatform;
    private float opacity = 1.0f;
    private Shape shape;
    private final GraphicsConfiguration graphicsConfig;

    private transient WindowListener windowListener;
    private transient WindowStateListener windowStateListener;
    private transient WindowFocusListener windowFocusListener;

    /** With the owner window, which may be `null`. */
    private Window(Window owner, GraphicsConfiguration gc, boolean internalMarker) {
        this.owner = owner;
        this.graphicsConfig = gc;
        this.setFocusableWindowStateInternal();
        if (owner != null) {
            synchronized (owner.ownedWindows) {
                owner.ownedWindows.add(this);
            }
        }
        synchronized (allWindows) {
            allWindows.add(this);
        }
    }

    /** Leaves the window as a focus cycle root, which is the only thing it can be. */
    private void setFocusableWindowStateInternal() {
        super.setFocusCycleRoot(true);
    }

    /**
     * A window that belongs to that frame.
     *
     * @throws HeadlessException if there is no screen
     */
    public Window(Frame owner) {
        this(owner, owner == null ? null : owner.getGraphicsConfiguration(), true);
    }

    /**
     * A window that belongs to that window.
     *
     * @throws HeadlessException if there is no screen
     */
    public Window(Window owner) {
        this(owner, owner == null ? null : owner.getGraphicsConfiguration(), true);
    }

    /**
     * Like the previous one, with the given graphics configuration.
     *
     * @throws IllegalArgumentException if the configuration is not a screen one
     */
    public Window(Window owner, GraphicsConfiguration gc) {
        this(owner, gc, true);
    }

    /** The images the desktop uses as an icon, in several sizes. */
    public List<Image> getIconImages() {
        return new ArrayList<Image>(this.icons);
    }

    /**
     * Gives it icons.
     *
     * <p>**Several sizes** are given and the desktop picks: a small one for the taskbar, a big one
     * for the window switcher. Giving only one forces scaling and looks bad.
     */
    public synchronized void setIconImages(List<? extends Image> icons) {
        List<Image> fresh = new ArrayList<Image>();
        if (icons != null) {
            java.util.Iterator<? extends Image> it = icons.iterator();
            while (it.hasNext()) {
                Image i = it.next();
                if (i != null) {
                    fresh.add(i);
                }
            }
        }
        this.icons = fresh;
    }

    /** Gives it a single icon. */
    public void setIconImage(Image image) {
        List<Image> one = new ArrayList<Image>();
        if (image != null) {
            one.add(image);
        }
        this.setIconImages(one);
    }

    /** Notifies that it can be shown. */
    public void addNotify() {
        super.addNotify();
    }

    /** Notifies that it can no longer be shown. */
    public void removeNotify() {
        super.removeNotify();
    }

    /**
     * Fits the window to the size its children need.
     *
     * <p>It is what saves working out by hand how much an interface measures: the tree is built,
     * this is called, and the window ends up the size of its content.
     */
    public void pack() {
        Dimension d = this.getPreferredSize();
        Insets m = this.getInsets();
        this.setSize(d.width + m.left + m.right, d.height + m.top + m.bottom);
        this.validate();
    }

    /** Sets its minimum size. */
    public void setMinimumSize(Dimension minimumSize) {
        super.setMinimumSize(minimumSize);
    }

    /** Resizes it. */
    public void setSize(Dimension d) {
        super.setSize(d);
    }

    /** Resizes it. */
    public void setSize(int width, int height) {
        super.setSize(width, height);
    }

    /**
     * Moves it.
     *
     * <p>Moving a window by hand switches {@link #setLocationByPlatform} off: whoever says where it
     * goes no longer wants the desktop to place it.
     */
    public void setLocation(int x, int y) {
        this.locationByPlatform = false;
        super.setLocation(x, y);
    }

    /** Moves it. */
    public void setLocation(Point p) {
        this.setLocation(p.x, p.y);
    }

    /**
     * Moves it and resizes it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setBounds(int, int, int, int)}.
     */
    @Deprecated
    public void reshape(int x, int y, int width, int height) {
        this.locationByPlatform = false;
        super.reshape(x, y, width, height);
    }

    /** Moves it and resizes it. */
    public void setBounds(int x, int y, int width, int height) {
        this.reshape(x, y, width, height);
    }

    /** Moves it and resizes it. */
    public void setBounds(Rectangle r) {
        this.setBounds(r.x, r.y, r.width, r.height);
    }

    /**
     * Shows it or hides it.
     *
     * <p>Showing it for the first time fires {@code WINDOW_OPENED}, and only the first time: it is
     * the notice that the window was born, not that it became visible.
     */
    public void setVisible(boolean b) {
        if (b) {
            this.show();
        } else {
            this.hide();
        }
    }

    private boolean everShown;

    /**
     * Shows it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setVisible}.
     */
    @Deprecated
    public void show() {
        boolean first = !this.everShown;
        super.show();
        if (first) {
            this.everShown = true;
            this.fireWindowEvent(WindowEvent.WINDOW_OPENED);
        }
    }

    /**
     * Hides it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setVisible}.
     */
    @Deprecated
    public void hide() {
        synchronized (this.ownedWindows) {
            for (int i = 0; i < this.ownedWindows.size(); i++) {
                this.ownedWindows.get(i).hide();
            }
        }
        super.hide();
    }

    /**
     * Releases the resources of the window and of the ones that belong to it.
     *
     * <p>A disposed window can be shown again: {@code dispose} releases the system resources, it
     * does not destroy the object. That is the difference from closing.
     */
    public void dispose() {
        synchronized (this.ownedWindows) {
            for (int i = this.ownedWindows.size() - 1; i >= 0; i--) {
                this.ownedWindows.get(i).dispose();
            }
        }
        this.hide();
        this.removeNotify();
        this.fireWindowEvent(WindowEvent.WINDOW_CLOSED);
    }

    /** Fires a window event if anyone asked for them. */
    private void fireWindowEvent(int id) {
        if (this.windowListener != null || this.windowStateListener != null
                || this.windowFocusListener != null
                || (this.eventMask & AWTEvent.WINDOW_EVENT_MASK) != 0) {
            this.processEvent(new WindowEvent(this, id));
        }
    }

    /** Puts it in front of the others; with no desktop, there is no order to change. */
    public void toFront() {
    }

    /** Sends it to the back. */
    public void toBack() {
    }

    /** The platform's toolkit. */
    public Toolkit getToolkit() {
        return Toolkit.getDefaultToolkit();
    }

    /**
     * The warning the system draws over a window of untrusted code.
     *
     * @return `null`: this window is not of untrusted code
     */
    public final String getWarningString() {
        return null;
    }

    /** The locale; the machine's if it has none of its own, because a window has no parent. */
    public Locale getLocale() {
        Locale l = null;
        try {
            l = super.getLocale();
        } catch (IllegalComponentStateException e) {
            // A window has no parent to inherit it from: it falls back to the machine's.
            l = null;
        }
        if (l != null) {
            return l;
        }
        return Locale.getDefault();
    }

    /** The write state of this window. */
    public InputContext getInputContext() {
        return InputContext.getInstance();
    }

    /** Gives it a cursor. */
    public void setCursor(Cursor cursor) {
        super.setCursor(cursor);
    }

    /** The window it belongs to, or `null` if it belongs to none. */
    public Window getOwner() {
        return this.owner;
    }

    /** The windows that belong to it. */
    public Window[] getOwnedWindows() {
        synchronized (this.ownedWindows) {
            return this.ownedWindows.toArray(new Window[this.ownedWindows.size()]);
        }
    }

    /** Every window of this application. */
    public static Window[] getWindows() {
        synchronized (allWindows) {
            return allWindows.toArray(new Window[allWindows.size()]);
        }
    }

    /** The ones that belong to no other. */
    public static Window[] getOwnerlessWindows() {
        synchronized (allWindows) {
            List<Window> out = new ArrayList<Window>();
            for (int i = 0; i < allWindows.size(); i++) {
                if (allWindows.get(i).getOwner() == null) {
                    out.add(allWindows.get(i));
                }
            }
            return out.toArray(new Window[out.size()]);
        }
    }

    /**
     * Declares that this window is not blocked by modal dialogs.
     *
     * @throws NullPointerException if the type is `null`
     */
    public void setModalExclusionType(Dialog.ModalExclusionType exclusionType) {
        if (exclusionType == null) {
            this.modalExclusionType = Dialog.ModalExclusionType.NO_EXCLUDE;
        } else {
            this.modalExclusionType = exclusionType;
        }
    }

    /** Which modal dialogs it is excluded from. */
    public Dialog.ModalExclusionType getModalExclusionType() {
        return this.modalExclusionType;
    }

    /** Adds a window listener; a `null` is ignored. */
    public synchronized void addWindowListener(WindowListener l) {
        if (l == null) {
            return;
        }
        this.windowListener = AWTEventMulticaster.add(this.windowListener, l);
        this.enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    }

    /** Adds a state listener; a `null` is ignored. */
    public synchronized void addWindowStateListener(WindowStateListener l) {
        if (l == null) {
            return;
        }
        this.windowStateListener = AWTEventMulticaster.add(this.windowStateListener, l);
        this.enableEvents(AWTEvent.WINDOW_STATE_EVENT_MASK);
    }

    /** Adds a window focus listener; a `null` is ignored. */
    public synchronized void addWindowFocusListener(WindowFocusListener l) {
        if (l == null) {
            return;
        }
        this.windowFocusListener = AWTEventMulticaster.add(this.windowFocusListener, l);
        this.enableEvents(AWTEvent.WINDOW_FOCUS_EVENT_MASK);
    }

    /** Removes that listener. */
    public synchronized void removeWindowListener(WindowListener l) {
        if (l == null) {
            return;
        }
        this.windowListener = AWTEventMulticaster.remove(this.windowListener, l);
    }

    /** Removes that listener. */
    public synchronized void removeWindowStateListener(WindowStateListener l) {
        if (l == null) {
            return;
        }
        this.windowStateListener = AWTEventMulticaster.remove(this.windowStateListener, l);
    }

    /** Removes that listener. */
    public synchronized void removeWindowFocusListener(WindowFocusListener l) {
        if (l == null) {
            return;
        }
        this.windowFocusListener = AWTEventMulticaster.remove(this.windowFocusListener, l);
    }

    /** The window listeners. */
    public synchronized WindowListener[] getWindowListeners() {
        return AWTEventMulticaster.getListeners(this.windowListener, WindowListener.class);
    }

    /** The window focus listeners. */
    public synchronized WindowFocusListener[] getWindowFocusListeners() {
        return AWTEventMulticaster.getListeners(this.windowFocusListener,
                WindowFocusListener.class);
    }

    /** The state listeners. */
    public synchronized WindowStateListener[] getWindowStateListeners() {
        return AWTEventMulticaster.getListeners(this.windowStateListener,
                WindowStateListener.class);
    }

    /**
     * The listeners of that class.
     *
     * <p>The {@code T extends EventListener} bound is what keeps the question well posed: a class
     * that is not a listener one cannot be passed without raw types.
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == WindowListener.class) {
            return AWTEventMulticaster.getListeners(this.windowListener, listenerType);
        }
        if (listenerType == WindowStateListener.class) {
            return AWTEventMulticaster.getListeners(this.windowStateListener, listenerType);
        }
        if (listenerType == WindowFocusListener.class) {
            return AWTEventMulticaster.getListeners(this.windowFocusListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    /**
     * Sorts the event out.
     *
     * <p>The three kinds of window event share the {@link WindowEvent} class and are told apart by
     * the identifier; that is why they have to be dispatched here and not by type.
     */
    protected void processEvent(AWTEvent e) {
        if (e instanceof WindowEvent) {
            int id = e.getID();
            if (id == WindowEvent.WINDOW_GAINED_FOCUS || id == WindowEvent.WINDOW_LOST_FOCUS) {
                this.processWindowFocusEvent((WindowEvent) e);
            } else if (id == WindowEvent.WINDOW_STATE_CHANGED) {
                this.processWindowStateEvent((WindowEvent) e);
            } else {
                this.processWindowEvent((WindowEvent) e);
            }
            return;
        }
        super.processEvent(e);
    }

    /** Tells the window listeners. */
    protected void processWindowEvent(WindowEvent e) {
        WindowListener l = this.windowListener;
        if (l == null) {
            return;
        }
        int id = e.getID();
        if (id == WindowEvent.WINDOW_OPENED) {
            l.windowOpened(e);
        } else if (id == WindowEvent.WINDOW_CLOSING) {
            l.windowClosing(e);
        } else if (id == WindowEvent.WINDOW_CLOSED) {
            l.windowClosed(e);
        } else if (id == WindowEvent.WINDOW_ICONIFIED) {
            l.windowIconified(e);
        } else if (id == WindowEvent.WINDOW_DEICONIFIED) {
            l.windowDeiconified(e);
        } else if (id == WindowEvent.WINDOW_ACTIVATED) {
            l.windowActivated(e);
        } else if (id == WindowEvent.WINDOW_DEACTIVATED) {
            l.windowDeactivated(e);
        }
    }

    /** Tells the window focus listeners. */
    protected void processWindowFocusEvent(WindowEvent e) {
        WindowFocusListener l = this.windowFocusListener;
        if (l == null) {
            return;
        }
        if (e.getID() == WindowEvent.WINDOW_GAINED_FOCUS) {
            l.windowGainedFocus(e);
        } else if (e.getID() == WindowEvent.WINDOW_LOST_FOCUS) {
            l.windowLostFocus(e);
        }
    }

    /** Tells the state listeners. */
    protected void processWindowStateEvent(WindowEvent e) {
        WindowStateListener l = this.windowStateListener;
        if (l != null && e.getID() == WindowEvent.WINDOW_STATE_CHANGED) {
            l.windowStateChanged(e);
        }
    }

    /** Declares that the window stays above all the others. */
    public final void setAlwaysOnTop(boolean alwaysOnTop) {
        boolean old;
        synchronized (this) {
            old = this.alwaysOnTop;
            this.alwaysOnTop = alwaysOnTop;
        }
        this.firePropertyChange("alwaysOnTop", old, alwaysOnTop);
    }

    /**
     * Whether the desktop supports always-on-top windows.
     *
     * @return `false`: there is no desktop
     */
    public boolean isAlwaysOnTopSupported() {
        return false;
    }

    /** Whether it was asked to stay always on top. */
    public final boolean isAlwaysOnTop() {
        return this.alwaysOnTop;
    }

    /**
     * Which component of this window has the focus.
     *
     * @return `null`: there is no focus manager that has given it to anybody
     */
    public Component getFocusOwner() {
        return null;
    }

    /**
     * Who had the focus the last time the window was active.
     *
     * @return `null` for the same reason
     */
    public Component getMostRecentFocusOwner() {
        return null;
    }

    /**
     * Whether it is the active window.
     *
     * @return `false`: with no desktop no window is active
     */
    public boolean isActive() {
        return false;
    }

    /**
     * Whether it has the keyboard focus.
     *
     * @return `false` for the same reason
     */
    public boolean isFocused() {
        return false;
    }

    /**
     * The traversal keys in that direction.
     *
     * @throws IllegalArgumentException if the direction is not one of the four
     */
    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        return super.getFocusTraversalKeys(id);
    }

    /**
     * It does nothing.
     *
     * <p>A window is **always** a focus cycle root: the tab key has nowhere to leave to.
     */
    public final void setFocusCycleRoot(boolean focusCycleRoot) {
    }

    /** Always `true`. */
    public final boolean isFocusCycleRoot() {
        return true;
    }

    /**
     * The root of the cycle that contains it.
     *
     * @return `null`: a window is the root, it is not inside another
     */
    public final Container getFocusCycleRootAncestor() {
        return null;
    }

    /**
     * Whether it can receive the focus.
     *
     * <p>Wanting to is not enough: a window with no owner and nothing to focus inside cannot
     * either.
     */
    public final boolean isFocusableWindow() {
        if (!this.getFocusableWindowState()) {
            return false;
        }
        return true;
    }

    /** Whether it was declared able to receive the focus. */
    public boolean getFocusableWindowState() {
        return this.focusableWindowState;
    }

    /**
     * Declares whether it can receive the focus.
     *
     * <p>Switching it off is what a floating toolbar does: it can be clicked without the working
     * window losing the focus.
     */
    public void setFocusableWindowState(boolean focusableWindowState) {
        boolean old;
        synchronized (this) {
            old = this.focusableWindowState;
            this.focusableWindowState = focusableWindowState;
        }
        this.firePropertyChange("focusableWindowState", old, focusableWindowState);
    }

    /** Declares whether the window asks for the focus by itself when shown. */
    public void setAutoRequestFocus(boolean autoRequestFocus) {
        this.autoRequestFocus = autoRequestFocus;
    }

    /** Whether it asks for the focus by itself when shown. */
    public boolean isAutoRequestFocus() {
        return this.autoRequestFocus;
    }

    /** Adds someone to tell about the property changes. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
    }

    /** Adds a listener for one particular property. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        super.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Whether validating has to stop here.
     *
     * <p>Always `true`: a window has a size of its own, so revalidating upwards makes no sense. It
     * is what keeps touching a button from revalidating the whole application.
     */
    public boolean isValidateRoot() {
        return true;
    }

    /**
     * Sends it an event of the old model.
     *
     * @deprecated it is from the 1.0 model. Use {@link #dispatchEvent}.
     */
    @Deprecated
    public boolean postEvent(Event e) {
        if (this.handleEvent(e)) {
            e.consume();
            return true;
        }
        return false;
    }

    /**
     * Whether it is really seen.
     *
     * @return `false`: the window never reaches the screen
     */
    public boolean isShowing() {
        return this.isVisible() && this.isDisplayable();
    }

    /**
     * Applies to it the texts of a resource bundle.
     *
     * @deprecated it did not work properly with nested containers and was left with no replacement.
     */
    @Deprecated
    public void applyResourceBundle(ResourceBundle rb) {
    }

    /**
     * The same, looking the bundle up by name.
     *
     * @deprecated for the same reason.
     */
    @Deprecated
    public void applyResourceBundle(String rbName) {
        this.applyResourceBundle(ResourceBundle.getBundle(rbName));
    }

    /**
     * Declares what the window is used for.
     *
     * <p>It only has an effect **before** showing it: the decoration is chosen by the desktop when
     * it creates it.
     *
     * @throws NullPointerException if the type is `null`
     */
    public void setType(Type type) {
        if (type == null) {
            throw new NullPointerException("type should not be null.");
        }
        this.type = type;
    }

    /** What it is used for. */
    public Type getType() {
        return this.type;
    }

    /**
     * Centres it with respect to that component.
     *
     * <p>With `null`, or with a component that is not on a screen, it centres it at the origin:
     * that is what is right when there is no screen to centre with respect to.
     */
    public void setLocationRelativeTo(Component c) {
        if (c == null || !c.isShowing()) {
            this.setLocation(0, 0);
            return;
        }
        Rectangle r = c.getBounds();
        this.setLocation(r.x + (r.width - this.getWidth()) / 2,
                r.y + (r.height - this.getHeight()) / 2);
    }

    /**
     * Builds a buffer strategy for drawing without flicker.
     *
     * @throws IllegalArgumentException if fewer than one buffer is asked for
     * @throws IllegalStateException always: a buffer strategy needs a surface of the system, and
     *     this window has none
     */
    public void createBufferStrategy(int numBuffers) {
        if (numBuffers < 1) {
            throw new IllegalArgumentException("Number of buffers must be at least 1");
        }
        throw new IllegalStateException("the window has no surface of the system: this "
                + "library ships no windowing system");
    }

    /**
     * Like the previous one, with the capabilities asked for.
     *
     * @throws IllegalArgumentException if fewer than one buffer is asked for or the capabilities
     *     are missing
     * @throws AWTException if the capabilities cannot be met
     * @throws IllegalStateException always, for the same reason
     */
    public void createBufferStrategy(int numBuffers, BufferCapabilities caps)
            throws AWTException {
        if (numBuffers < 1) {
            throw new IllegalArgumentException("Number of buffers must be at least 1");
        }
        if (caps == null) {
            throw new IllegalArgumentException("No capabilities specified");
        }
        throw new IllegalStateException("the window has no surface of the system: this "
                + "library ships no windowing system");
    }

    /**
     * The buffer strategy.
     *
     * @return `null`: none could ever be created
     */
    public BufferStrategy getBufferStrategy() {
        return null;
    }

    /** Declares that the desktop places it instead of putting it at a fixed position. */
    public void setLocationByPlatform(boolean locationByPlatform) {
        this.locationByPlatform = locationByPlatform;
    }

    /** Whether the desktop was asked to place it. */
    public boolean isLocationByPlatform() {
        return this.locationByPlatform;
    }

    /** How opaque it is, from 0 to 1. */
    public float getOpacity() {
        return this.opacity;
    }

    /**
     * Changes its opacity.
     *
     * @throws IllegalArgumentException if the value is not between 0 and 1
     * @throws IllegalComponentStateException if the window is decorated and translucency is asked
     *     for
     */
    public void setOpacity(float opacity) {
        if (opacity < 0.0f || opacity > 1.0f) {
            throw new IllegalArgumentException(
                    "The value of opacity should be in the range [0.0f .. 1.0f].");
        }
        this.opacity = opacity;
    }

    /** The clipped shape of the window, or `null` if it is rectangular. */
    public Shape getShape() {
        return this.shape;
    }

    /**
     * Clips its shape.
     *
     * <p>With `null` it goes back to being rectangular. It is what allows a round window or one
     * with a hole.
     */
    public void setShape(Shape shape) {
        this.shape = shape;
    }

    /** The background colour. */
    public Color getBackground() {
        return super.getBackground();
    }

    /**
     * Changes its background colour.
     *
     * <p>A background with an alpha below 255 asks for per-pixel transparency, which the desktop
     * may not support.
     */
    public void setBackground(Color bgColor) {
        super.setBackground(bgColor);
    }

    /** Whether it paints all of its pixels: only if its background is opaque. */
    public boolean isOpaque() {
        Color c = this.getBackground();
        if (c == null) {
            return true;
        }
        return c.getAlpha() == 255;
    }

    /** It draws itself and draws its children. */
    public void paint(Graphics g) {
        super.paint(g);
    }

    /** The graphics configuration it was created with, or `null`. */
    public GraphicsConfiguration getGraphicsConfiguration() {
        if (this.graphicsConfig != null) {
            return this.graphicsConfig;
        }
        return super.getGraphicsConfiguration();
    }

    /** The accessibility information of this window. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTWindow();
        }
        return this.accessibleContext;
    }

    /** The accessibility of a window. */
    protected class AccessibleAWTWindow extends AccessibleAWTContainer {

        /** For the subclasses. */
        protected AccessibleAWTWindow() {
        }

        /** It is a window. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.WINDOW;
        }

        /** The ones of a container, plus one more if it is active. */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (Window.this.isActive()) {
                s.add(AccessibleState.ACTIVE);
            }
            return s;
        }
    }
}
