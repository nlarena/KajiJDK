package java.awt;

import java.awt.datatransfer.Clipboard;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.event.AWTEventListener;
import java.awt.event.AWTEventListenerProxy;
import java.awt.im.InputMethodHighlight;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The bridge between AWT and the platform.
 *
 * <p>Everything that depends on the operating system goes through here: the size of the screen, the
 * clipboard, the installed fonts, the event queue, the beep. It is the class that keeps the rest of
 * AWT from having to know which system it runs on.
 *
 * <p>It is abstract and there is **one** instance, the one {@link #getDefaultToolkit} returns. The
 * JDK loads the platform's one through a system property.
 *
 * <p><strong>Here the instance is a toolkit without a screen.</strong> It is the same thing the
 * headless mode of the real JDK does, and the rule is the same: what can be answered without a
 * screen is answered, and what needs a screen throws {@link HeadlessException}. There are no
 * invented values — a screen size that does not exist cannot be approximated.
 *
 * <p>What **does** work turns out to be quite a lot: the colour model, the event queue with its
 * dispatch thread, {@code createImage} from a producer of pixels, {@code prepareImage} and
 * {@code checkImage}, the desktop properties, the global event listeners and the system clipboard —
 * which here is a private one, because there is no system one to share with.
 */
public abstract class Toolkit {

    private static Toolkit toolkit;

    /** Who to tell about the changes in the desktop properties. */
    protected final PropertyChangeSupport desktopPropsSupport = new PropertyChangeSupport(this);

    /** The desktop properties, by name. */
    protected final Map<String, Object> desktopProperties = new HashMap<String, Object>();

    private final List<AWTEventListenerProxy> eventListeners =
            new ArrayList<AWTEventListenerProxy>();

    private boolean dynamicLayout = true;

    /** For the subclasses. */
    protected Toolkit() {
    }

    /**
     * The platform's instance.
     *
     * <p>Here it is always the same one: a toolkit without a screen.
     */
    public static synchronized Toolkit getDefaultToolkit() {
        if (toolkit == null) {
            toolkit = new HeadlessToolkit();
        }
        return toolkit;
    }

    /** The size of the screen. */
    public abstract Dimension getScreenSize() throws HeadlessException;

    /** How many dots per inch the screen has. */
    public abstract int getScreenResolution() throws HeadlessException;

    /**
     * What part of the screen is covered by bars of the desktop.
     *
     * @throws NullPointerException if the configuration is `null`
     * @throws HeadlessException if there is no screen
     */
    public Insets getScreenInsets(GraphicsConfiguration gc) throws HeadlessException {
        if (gc == null) {
            throw new NullPointerException("gc");
        }
        throw new HeadlessException();
    }

    /** The pixel format of the screen. */
    public abstract ColorModel getColorModel() throws HeadlessException;

    /**
     * The names of the installed fonts.
     *
     * @deprecated it returns only the logical families. Use
     *     {@code GraphicsEnvironment.getAvailableFontFamilyNames}.
     */
    @Deprecated
    public abstract String[] getFontList();

    /**
     * The measures of that font.
     *
     * @deprecated it depends on the screen things are drawn on. Use
     *     {@code Font.getLineMetrics(String, FontRenderContext)}.
     */
    @Deprecated
    public abstract FontMetrics getFontMetrics(Font font);

    /** Sends to the screen everything that was waiting to be drawn. */
    public abstract void sync();

    /** An image read from that file. */
    public abstract Image getImage(String filename);

    /** An image read from that address. */
    public abstract Image getImage(URL url);

    /** An image read from that file, without using the cache. */
    public abstract Image createImage(String filename);

    /** An image read from that address, without using the cache. */
    public abstract Image createImage(URL url);

    /** An image from a producer of pixels. */
    public abstract Image createImage(ImageProducer producer);

    /** An image decoded from those bytes. */
    public abstract Image createImage(byte[] imagedata, int imageoffset, int imagelength);

    /** The same, with the whole array. */
    public Image createImage(byte[] imagedata) {
        return this.createImage(imagedata, 0, imagedata.length);
    }

    /**
     * Starts preparing an image for that size.
     *
     * @return whether it is ready already
     */
    public abstract boolean prepareImage(Image image, int width, int height,
            ImageObserver observer);

    /** How much was prepared, as {@link ImageObserver} flags. */
    public abstract int checkImage(Image image, int width, int height, ImageObserver observer);

    /**
     * A print job.
     *
     * @return the job, or `null` if the user cancelled it
     */
    public abstract PrintJob getPrintJob(Frame frame, String jobtitle, Properties props);

    /**
     * A print job with attributes.
     *
     * @return the job, or `null` if the user cancelled it
     * @throws NullPointerException if the frame is `null` and the job either gives no attributes or
     *     asks for the native dialog
     */
    public PrintJob getPrintJob(Frame frame, String jobtitle, JobAttributes jobAttributes,
            PageAttributes pageAttributes) {
        if (frame == null && (jobAttributes == null
                || jobAttributes.getDialog() == JobAttributes.DialogType.NATIVE)) {
            throw new NullPointerException("frame must not be null");
        }
        return this.getPrintJob(frame, jobtitle, null);
    }

    /** Sounds the system beep. */
    public abstract void beep();

    /** The system clipboard. */
    public abstract Clipboard getSystemClipboard() throws HeadlessException;

    /**
     * The selection clipboard, the one that on X11 fills up when text is selected.
     *
     * @return `null` if the platform has none
     * @throws HeadlessException if there is no screen
     */
    public Clipboard getSystemSelection() throws HeadlessException {
        return null;
    }

    /**
     * Which key is the platform's menu modifier.
     *
     * <p>Here it answers Ctrl and never throws; the JDK throws when there is no screen.
     *
     * @deprecated it returns a mask of the old encoding. Use {@link #getMenuShortcutKeyMaskEx}.
     */
    @Deprecated
    public int getMenuShortcutKeyMask() throws HeadlessException {
        return java.awt.event.InputEvent.CTRL_MASK;
    }

    /**
     * Which key is the menu modifier, in the new encoding.
     *
     * <p>Control almost everywhere, Meta on macOS. Asking about it is what saves writing that
     * difference into every application. Here it answers Ctrl and never throws; the JDK throws when
     * there is no screen.
     */
    public int getMenuShortcutKeyMaskEx() throws HeadlessException {
        return java.awt.event.InputEvent.CTRL_DOWN_MASK;
    }

    /**
     * Whether that locking key is switched on.
     *
     * @throws IllegalArgumentException if the key is not a locking one
     * @throws UnsupportedOperationException if the platform cannot say
     */
    public boolean getLockingKeyState(int keyCode) throws UnsupportedOperationException {
        if (keyCode != java.awt.event.KeyEvent.VK_CAPS_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_NUM_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_SCROLL_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_KANA_LOCK) {
            throw new IllegalArgumentException("invalid key for Toolkit.getLockingKeyState");
        }
        throw new UnsupportedOperationException("there is no system keyboard to ask");
    }

    /**
     * Switches a locking key on or off.
     *
     * @throws IllegalArgumentException if the key is not a locking one
     * @throws UnsupportedOperationException if the platform does not support it
     */
    public void setLockingKeyState(int keyCode, boolean on) throws UnsupportedOperationException {
        if (keyCode != java.awt.event.KeyEvent.VK_CAPS_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_NUM_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_SCROLL_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_KANA_LOCK) {
            throw new IllegalArgumentException("invalid key for Toolkit.setLockingKeyState");
        }
        throw new UnsupportedOperationException("there is no system keyboard to ask it of");
    }

    /**
     * A cursor made out of an image.
     *
     * @throws IndexOutOfBoundsException if the hot spot falls outside the image
     * @throws HeadlessException if there is no screen
     */
    public Cursor createCustomCursor(Image cursor, Point hotSpot, String name)
            throws IndexOutOfBoundsException, HeadlessException {
        throw new HeadlessException();
    }

    /**
     * The cursor size the platform supports closest to the one asked for.
     *
     * @return the size, or (0,0) if it supports no custom cursors
     * @throws HeadlessException if there is no screen
     */
    public Dimension getBestCursorSize(int preferredWidth, int preferredHeight)
            throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * How many colours a custom cursor supports.
     *
     * @throws HeadlessException if there is no screen
     */
    public int getMaximumCursorColors() throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * Whether the platform supports that window state.
     *
     * @return `true` only for {@link Frame#NORMAL}: there is no window manager to apply any other
     *     state
     * @throws HeadlessException if there is no screen
     */
    public boolean isFrameStateSupported(int state) throws HeadlessException {
        return state == Frame.NORMAL;
    }

    /**
     * Whether the platform supports always-on-top windows.
     *
     * @return `false`: there is no window manager
     */
    public boolean isAlwaysOnTopSupported() {
        return false;
    }

    /** Whether it supports that modality scope. */
    public abstract boolean isModalityTypeSupported(Dialog.ModalityType modalityType);

    /** Whether it supports that kind of modality exclusion. */
    public abstract boolean isModalExclusionTypeSupported(
            Dialog.ModalExclusionType modalExclusionType);

    /**
     * Declares whether windows are laid out again while they are dragged.
     *
     * @throws HeadlessException if there is no screen
     */
    public void setDynamicLayout(boolean dynamic) throws HeadlessException {
        this.dynamicLayout = dynamic;
    }

    /** Whether the continuous layout was asked for. */
    protected boolean isDynamicLayoutSet() throws HeadlessException {
        return this.dynamicLayout;
    }

    /**
     * Whether the continuous layout is actually active.
     *
     * @return `false`: there is no window manager dragging anything
     * @throws HeadlessException if there is no screen
     */
    public boolean isDynamicLayoutActive() throws HeadlessException {
        return false;
    }

    /**
     * Whether the mouse buttons beyond the third one are told apart.
     *
     * @throws HeadlessException if there is no screen
     */
    public boolean areExtraMouseButtonsEnabled() throws HeadlessException {
        return true;
    }

    /** The system event queue. */
    public final EventQueue getSystemEventQueue() {
        return this.getSystemEventQueueImpl();
    }

    /** Where the queue comes from; each toolkit writes it. */
    protected abstract EventQueue getSystemEventQueueImpl();

    /**
     * How a stretch of text the input method is composing is drawn.
     *
     * @return the style, or `null` to let the component decide
     * @throws HeadlessException if there is no screen
     */
    public abstract Map<java.awt.font.TextAttribute, ?> mapInputMethodHighlight(
            InputMethodHighlight highlight) throws HeadlessException;

    /**
     * A desktop property.
     *
     * @return the value, or `null` if it is not defined
     */
    public final synchronized Object getDesktopProperty(String propertyName) {
        if (this.desktopProperties.isEmpty()) {
            this.initializeDesktopProperties();
        }
        Object v = this.desktopProperties.get(propertyName);
        if (v == null) {
            v = this.lazilyLoadDesktopProperty(propertyName);
            if (v != null) {
                this.setDesktopProperty(propertyName, v);
            }
        }
        return v;
    }

    /** Stores a desktop property and reports the change. */
    protected final void setDesktopProperty(String name, Object newValue) {
        Object old;
        synchronized (this) {
            old = this.desktopProperties.get(name);
            this.desktopProperties.put(name, newValue);
        }
        this.desktopPropsSupport.firePropertyChange(name, old, newValue);
    }

    /**
     * Loads a property only when it is asked for.
     *
     * @return the value, or `null` if it does not exist
     */
    protected Object lazilyLoadDesktopProperty(String name) {
        return null;
    }

    /** Fills in the desktop properties; with no desktop there are none. */
    protected void initializeDesktopProperties() {
    }

    /** Adds someone to tell about the changes of that property. */
    public void addPropertyChangeListener(String name, PropertyChangeListener pcl) {
        this.desktopPropsSupport.addPropertyChangeListener(name, pcl);
    }

    /** Removes that listener. */
    public void removePropertyChangeListener(String name, PropertyChangeListener pcl) {
        this.desktopPropsSupport.removePropertyChangeListener(name, pcl);
    }

    /** Every property listener. */
    public PropertyChangeListener[] getPropertyChangeListeners() {
        return this.desktopPropsSupport.getPropertyChangeListeners();
    }

    /** The listeners of that property. */
    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return this.desktopPropsSupport.getPropertyChangeListeners(propertyName);
    }

    /**
     * Adds a listener that sees **every** event of those families.
     *
     * <p>It is the back door of the dispatching: it registers with the toolkit and not with a
     * component. A `null` is ignored.
     */
    public void addAWTEventListener(AWTEventListener listener, long eventMask) {
        if (listener == null) {
            return;
        }
        synchronized (this) {
            this.eventListeners.add(new AWTEventListenerProxy(eventMask, listener));
        }
    }

    /** Removes that global listener. */
    public void removeAWTEventListener(AWTEventListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this) {
            for (int i = this.eventListeners.size() - 1; i >= 0; i--) {
                if (this.eventListeners.get(i).getListener() == listener) {
                    this.eventListeners.remove(i);
                }
            }
        }
    }

    /** Every global listener, each one with its mask. */
    public AWTEventListener[] getAWTEventListeners() {
        synchronized (this) {
            return this.eventListeners.toArray(new AWTEventListener[this.eventListeners.size()]);
        }
    }

    /** The global listeners that cover all of those families. */
    public AWTEventListener[] getAWTEventListeners(long eventMask) {
        synchronized (this) {
            List<AWTEventListener> out = new ArrayList<AWTEventListener>();
            for (int i = 0; i < this.eventListeners.size(); i++) {
                AWTEventListenerProxy p = this.eventListeners.get(i);
                if ((p.getEventMask() & eventMask) == eventMask) {
                    out.add(p);
                }
            }
            return out.toArray(new AWTEventListener[out.size()]);
        }
    }

    /**
     * A drag gesture recogniser of the class asked for.
     *
     * @return `null`: the concrete recogniser is supplied by the windowing system
     */
    public <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> abstractRecognizerClass, DragSource ds, Component c, int srcActions,
            DragGestureListener dgl) {
        return null;
    }

    /**
     * A system property, with a default value.
     *
     * @deprecated it is a wrapper of {@code System.getProperty} that adds nothing.
     */
    @Deprecated
    public static String getProperty(String key, String defaultValue) {
        String v = System.getProperty(key);
        return v == null ? defaultValue : v;
    }

    /**
     * The native container of a component.
     *
     * @return the nearest heavyweight ancestor, or `null` if there is none
     */
    protected static Container getNativeContainer(Component c) {
        Container p = c == null ? null : c.getParent();
        while (p != null && p.isLightweight()) {
            p = p.getParent();
        }
        return p;
    }

    /**
     * Fills that array with the system colours.
     *
     * <p>It does nothing: with no desktop there is no palette to read, and {@link SystemColor}
     * already carries reasonable default values.
     */
    protected void loadSystemColors(int[] systemColors) throws HeadlessException {
    }
}
