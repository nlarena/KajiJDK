package java.awt;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.awt.image.BufferStrategy;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.VolatileImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * Anything that takes up room on a screen and can receive input from the user.
 *
 * <p>It is the biggest class of AWT and the root of everything visible. It brings together five
 * responsibilities that in a design of today would be separate, and it helps to look at them apart
 * to understand it:
 *
 * <ul>
 *   <li><strong>geometry</strong>: where it is and how much it measures, plus the three suggested
 *       sizes —minimum, preferred and maximum— that the layouts use to share out the room;
 *   <li><strong>appearance</strong>: foreground and background colour, font, cursor, whether it is
 *       seen;
 *   <li><strong>events</strong>: registering listeners and handing them what arrives;
 *   <li><strong>focus</strong>: whether it can receive it, which keys walk through it;
 *   <li><strong>painting</strong>: {@code paint}, {@code update} and {@code repaint}.
 * </ul>
 *
 * <p>The three sizes have a rule that gets forgotten: {@link #getPreferredSize} returns whatever
 * was set on it with {@link #setPreferredSize}, and only if nothing was set does it work it out.
 * That is why {@link #isPreferredSizeSet} exists — it is the only way of telling "I was told to
 * measure this" from "I think I should measure this".
 *
 * <p>The event dispatching is three steps and each one can be intercepted: {@link #dispatchEvent}
 * receives, {@link #processEvent} sorts out, and the {@code processXEvent} methods tell the
 * listeners. Overriding the middle one allows seeing everything; overriding one of the last ones,
 * changing the treatment of one family without touching the rest.
 *
 * <p><strong>This component is never displayable.</strong> Everything that needs a window of the
 * system —{@link #isDisplayable}, {@link #getGraphics}, {@link #getLocationOnScreen},
 * {@link #createImage(int, int)}, the real focus— answers what corresponds to a component that is
 * not on a screen: `false`, `null` or the exception the method declares for that case. They are not
 * filler: they are the true answers. Everything else —the geometry, the colours, the listeners, the
 * event dispatching, the hierarchy, the accessibility— really works and can be used and tested.
 */
public abstract class Component implements ImageObserver, MenuContainer, Serializable {

    private static final long serialVersionUID = -7644114512714619750L;

    /** Aligned with the top edge. */
    public static final float TOP_ALIGNMENT = 0.0f;

    /** Centred. */
    public static final float CENTER_ALIGNMENT = 0.5f;

    /** Aligned with the bottom edge. */
    public static final float BOTTOM_ALIGNMENT = 1.0f;

    /** Aligned with the left edge. */
    public static final float LEFT_ALIGNMENT = 0.0f;

    /** Aligned with the right edge. */
    public static final float RIGHT_ALIGNMENT = 1.0f;

    /**
     * The lock the component tree synchronises on.
     *
     * <p>There is **one only** for all of AWT. One lock per component would seem better, but
     * walking the tree taking them in different orders would end in a deadlock; with a global one
     * that cannot happen.
     */
    static final Object LOCK = new Object();

    private static int nameCounter;

    /** How the baseline stretches when the component changes height. */
    public static enum BaselineResizeBehavior {

        /** The distance from the top does not change. */
        CONSTANT_ASCENT,

        /** The distance from the bottom does not change. */
        CONSTANT_DESCENT,

        /** The baseline stays at the same distance from the centre. */
        CENTER_OFFSET,

        /** None of the three: it has to be asked again at every size. */
        OTHER
    }

    private int x;
    private int y;
    private int width;
    private int height;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean valid;
    private boolean focusable = true;
    private boolean focusTraversalKeysEnabled = true;
    private boolean ignoreRepaint;
    private Color foreground;
    private Color background;
    private Font font;
    private Cursor cursor;
    private String name;
    private boolean nameExplicitlySet;
    private Locale locale;
    private ComponentOrientation componentOrientation = ComponentOrientation.UNKNOWN;
    private Dimension minSize;
    private Dimension prefSize;
    private Dimension maxSize;
    private boolean minSizeSet;
    private boolean prefSizeSet;
    private boolean maxSizeSet;
    private java.awt.dnd.DropTarget dropTarget;
    private final Set<AWTKeyStroke>[] focusTraversalKeys = newKeyStrokeSets();
    private Container parent;
    private final List<PopupMenu> popups = new ArrayList<PopupMenu>();
    private PropertyChangeSupport changeSupport;

    /** Which families of events it asked to receive. */
    long eventMask;

    private transient ComponentListener componentListener;
    private transient FocusListener focusListener;
    private transient HierarchyListener hierarchyListener;
    private transient HierarchyBoundsListener hierarchyBoundsListener;
    private transient KeyListener keyListener;
    private transient MouseListener mouseListener;
    private transient MouseMotionListener mouseMotionListener;
    private transient MouseWheelListener mouseWheelListener;
    private transient InputMethodListener inputMethodListener;

    /** The accessibility information, built on demand. */
    protected AccessibleContext accessibleContext;

    /** An array of four sets of keys, one per traversal direction. */
    @SuppressWarnings("unchecked")
    private static Set<AWTKeyStroke>[] newKeyStrokeSets() {
        return (Set<AWTKeyStroke>[]) new Set<?>[4];
    }

    /**
     * The default traversal keys.
     *
     * <p>Tab forwards, shift-tab backwards, and nothing for going up and down a cycle. They are the
     * same ones as on any desktop, and being here and not in a focus manager is what lets a loose
     * component answer correctly without a manager being installed.
     */
    private static Set<AWTKeyStroke> defaultKeyStrokes(int id) {
        Set<AWTKeyStroke> s = new HashSet<AWTKeyStroke>();
        if (id == 0) {
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK));
        } else if (id == 1) {
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.SHIFT_DOWN_MASK));
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK
                            | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        }
        return Collections.unmodifiableSet(s);
    }

    /** For the subclasses. */
    protected Component() {
    }

    /** The default name, a different one for each. */
    String constructComponentName() {
        synchronized (Component.class) {
            String n = this.getClass().getName() + nameCounter;
            nameCounter = nameCounter + 1;
            return n;
        }
    }

    /** What it is called; if nobody gave it a name, one is built so that debugging is possible. */
    public String getName() {
        if (this.name == null && !this.nameExplicitlySet) {
            synchronized (this.getObjectLock()) {
                if (this.name == null && !this.nameExplicitlySet) {
                    this.name = this.constructComponentName();
                }
            }
        }
        return this.name;
    }

    /** Gives it a name and reports the change. */
    public void setName(String name) {
        String old;
        synchronized (this.getObjectLock()) {
            old = this.name;
            this.name = name;
            this.nameExplicitlySet = true;
        }
        this.firePropertyChange("name", old, name);
    }

    /** The lock of this object; kept apart from the tree's so as not to serialise it. */
    Object getObjectLock() {
        return this;
    }

    /** Which container it hangs from, or `null`. */
    public Container getParent() {
        return this.parent;
    }

    /** The container uses it when adding or removing it. */
    void setParent(Container p) {
        this.parent = p;
    }

    /**
     * The lock of the component tree.
     *
     * <p>It is `final` and it is the same for all of them: see {@link #LOCK}.
     */
    public final Object getTreeLock() {
        return LOCK;
    }

    /** The platform's toolkit. */
    public Toolkit getToolkit() {
        return Toolkit.getDefaultToolkit();
    }

    /**
     * Whether the component has a window of the system behind it.
     *
     * <p>It answers `false` always: this library ships no windowing system, so no component gets to
     * have one. Almost every other negative answer of the class comes from here, and they are all
     * true.
     */
    public boolean isDisplayable() {
        return false;
    }

    /** Whether it is declared visible. */
    public boolean isVisible() {
        return this.visible;
    }

    /**
     * Whether it is really seen.
     *
     * <p>Being declared visible is not enough: it has to be, to have a parent, and for the parent
     * to be seen too. It answers `false` always because no component gets to be on a screen.
     */
    public boolean isShowing() {
        if (this.visible && this.isDisplayable()) {
            Container p = this.parent;
            return p == null || p.isShowing();
        }
        return false;
    }

    /** Whether it responds to the user's input. */
    public boolean isEnabled() {
        return this.enabled;
    }

    /** Enables it or disables it. */
    public void setEnabled(boolean b) {
        boolean old;
        synchronized (this.getTreeLock()) {
            old = this.enabled;
            this.enabled = b;
        }
        this.firePropertyChange("enabled", old, b);
    }

    /**
     * Enables it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setEnabled}.
     */
    @Deprecated
    public void enable() {
        this.setEnabled(true);
    }

    /**
     * Enables it or disables it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setEnabled}.
     */
    @Deprecated
    public void enable(boolean b) {
        this.setEnabled(b);
    }

    /**
     * Disables it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setEnabled}.
     */
    @Deprecated
    public void disable() {
        this.setEnabled(false);
    }

    /** Whether it draws in two steps to avoid flicker. */
    public boolean isDoubleBuffered() {
        return false;
    }

    /** Switches the input method on or off for this component. */
    public void enableInputMethods(boolean enable) {
    }

    /** Shows it or hides it, and reports. */
    public void setVisible(boolean b) {
        this.show(b);
    }

    /**
     * Shows it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setVisible}.
     */
    @Deprecated
    public void show() {
        boolean old;
        synchronized (this.getTreeLock()) {
            old = this.visible;
            this.visible = true;
        }
        if (!old) {
            this.firePropertyChange("visible", false, true);
            this.fireComponentEvent(ComponentEvent.COMPONENT_SHOWN);
        }
    }

    /**
     * Shows it or hides it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setVisible}.
     */
    @Deprecated
    public void show(boolean b) {
        if (b) {
            this.show();
        } else {
            this.hide();
        }
    }

    /**
     * Hides it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setVisible}.
     */
    @Deprecated
    public void hide() {
        boolean old;
        synchronized (this.getTreeLock()) {
            old = this.visible;
            this.visible = false;
        }
        if (old) {
            this.firePropertyChange("visible", true, false);
            this.fireComponentEvent(ComponentEvent.COMPONENT_HIDDEN);
        }
    }

    /**
     * Fires a component event if anyone asked for them **and** the component is on a screen.
     *
     * <p>The second condition is the one that surprises and it is the JDK's: these events are
     * generated by the windowing system when really moving or resizing, not by the model when a
     * number changes. A component that never reaches a screen generates none on its own — which
     * does not stop them from being delivered by hand with {@link #dispatchEvent}, which is what a
     * test harness does.
     */
    private void fireComponentEvent(int id) {
        if (!this.isDisplayable()) {
            return;
        }
        if (this.componentListener != null
                || (this.eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0) {
            this.processComponentEvent(new ComponentEvent(this, id));
        }
    }

    /** The colour it is drawn with; inherited from the parent if it has none of its own. */
    public Color getForeground() {
        Color c = this.foreground;
        if (c != null) {
            return c;
        }
        Container p = this.parent;
        if (p != null) {
            return p.getForeground();
        }
        return null;
    }

    /** Gives it a colour of its own. */
    public void setForeground(Color c) {
        Color old = this.foreground;
        this.foreground = c;
        this.firePropertyChange("foreground", old, c);
    }

    /** Whether it has a colour of its own, not counting the inherited one. */
    public boolean isForegroundSet() {
        return this.foreground != null;
    }

    /** The background colour; inherited from the parent if it has none of its own. */
    public Color getBackground() {
        Color c = this.background;
        if (c != null) {
            return c;
        }
        Container p = this.parent;
        if (p != null) {
            return p.getBackground();
        }
        return null;
    }

    /** Gives it a background colour of its own. */
    public void setBackground(Color c) {
        Color old = this.background;
        this.background = c;
        this.firePropertyChange("background", old, c);
    }

    /** Whether it has a background colour of its own. */
    public boolean isBackgroundSet() {
        return this.background != null;
    }

    /** The font; inherited from the parent if it has none of its own. */
    public Font getFont() {
        Font f = this.font;
        if (f != null) {
            return f;
        }
        Container p = this.parent;
        if (p != null) {
            return p.getFont();
        }
        return null;
    }

    /**
     * Gives it a font of its own.
     *
     * <p>It invalidates the component: changing the font changes how much the text measures, and
     * with it the preferred size.
     */
    public void setFont(Font f) {
        Font old;
        synchronized (this.getTreeLock()) {
            old = this.font;
            this.font = f;
        }
        this.firePropertyChange("font", old, f);
        this.invalidate();
    }

    /** Whether it has a font of its own. */
    public boolean isFontSet() {
        return this.font != null;
    }

    /** The locale; inherited from the parent if it has none of its own. */
    public Locale getLocale() {
        Locale l = this.locale;
        if (l != null) {
            return l;
        }
        Container p = this.parent;
        if (p == null) {
            throw new IllegalComponentStateException(
                    "This component must have a parent in order to determine its locale");
        }
        return p.getLocale();
    }

    /** Gives it a locale of its own. */
    public void setLocale(Locale l) {
        Locale old = this.locale;
        this.locale = l;
        this.firePropertyChange("locale", old, l);
        this.invalidate();
    }

    /**
     * The colour format it draws in.
     *
     * <p>With no window of its own, the toolkit's.
     */
    public ColorModel getColorModel() {
        return this.getToolkit().getColorModel();
    }

    /** Where it is, relative to its parent. */
    public Point getLocation() {
        return this.location();
    }

    /**
     * Where it is on the screen.
     *
     * @throws IllegalComponentStateException always: the component is not on a screen, so it has no
     *     position on one. It is the exception the method declares for this case.
     */
    public Point getLocationOnScreen() {
        throw new IllegalComponentStateException("component must be showing on the screen to "
                + "determine its location");
    }

    /**
     * Where it is.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getLocation}.
     */
    @Deprecated
    public Point location() {
        return new Point(this.x, this.y);
    }

    /** Moves it. */
    public void setLocation(int x, int y) {
        this.move(x, y);
    }

    /**
     * Moves it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setLocation}.
     */
    @Deprecated
    public void move(int x, int y) {
        synchronized (this.getTreeLock()) {
            this.setBoundsOp(x, y, this.width, this.height);
        }
    }

    /**
     * Moves it.
     *
     * @throws NullPointerException if the point is `null`
     */
    public void setLocation(Point p) {
        this.setLocation(p.x, p.y);
    }

    /** How much it measures. */
    public Dimension getSize() {
        return this.size();
    }

    /**
     * How much it measures.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getSize}.
     */
    @Deprecated
    public Dimension size() {
        return new Dimension(this.width, this.height);
    }

    /** Resizes it. */
    public void setSize(int width, int height) {
        this.resize(width, height);
    }

    /**
     * Resizes it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setSize}.
     */
    @Deprecated
    public void resize(int width, int height) {
        synchronized (this.getTreeLock()) {
            this.setBoundsOp(this.x, this.y, width, height);
        }
    }

    /**
     * Resizes it.
     *
     * @throws NullPointerException if the dimension is `null`
     */
    public void setSize(Dimension d) {
        this.setSize(d.width, d.height);
    }

    /**
     * Resizes it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setSize}.
     */
    @Deprecated
    public void resize(Dimension d) {
        this.setSize(d.width, d.height);
    }

    /** Where it is and how much it measures. */
    public Rectangle getBounds() {
        return this.bounds();
    }

    /**
     * Where it is and how much it measures.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getBounds}.
     */
    @Deprecated
    public Rectangle bounds() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    /** Moves it and resizes it in one go. */
    public void setBounds(int x, int y, int width, int height) {
        this.reshape(x, y, width, height);
    }

    /**
     * Moves it and resizes it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setBounds}.
     */
    @Deprecated
    public void reshape(int x, int y, int width, int height) {
        synchronized (this.getTreeLock()) {
            this.setBoundsOp(x, y, width, height);
        }
    }

    /**
     * Changes the rectangle and fires whichever events correspond.
     *
     * <p>Moving and resizing are two different events, and an operation that does both has to fire
     * both: there is code that listens to only one.
     */
    private void setBoundsOp(int x, int y, int width, int height) {
        boolean moved = this.x != x || this.y != y;
        boolean sizeChanged = this.width != width || this.height != height;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if (sizeChanged) {
            this.invalidate();
        }
        if (moved) {
            this.fireComponentEvent(ComponentEvent.COMPONENT_MOVED);
        }
        if (sizeChanged) {
            this.fireComponentEvent(ComponentEvent.COMPONENT_RESIZED);
        }
    }

    /**
     * Moves it and resizes it.
     *
     * @throws NullPointerException if the rectangle is `null`
     */
    public void setBounds(Rectangle r) {
        this.setBounds(r.x, r.y, r.width, r.height);
    }

    /** The X, relative to the parent. */
    public int getX() {
        return this.x;
    }

    /** The Y, relative to the parent. */
    public int getY() {
        return this.y;
    }

    /** The width. */
    public int getWidth() {
        return this.width;
    }

    /** The height. */
    public int getHeight() {
        return this.height;
    }

    /**
     * Its rectangle, written into the one that is passed in.
     *
     * <p>It exists so as not to create an object per query in a layout loop.
     */
    public Rectangle getBounds(Rectangle rv) {
        if (rv == null) {
            return new Rectangle(this.x, this.y, this.width, this.height);
        }
        rv.setBounds(this.x, this.y, this.width, this.height);
        return rv;
    }

    /** Its size, written into the one that is passed in. */
    public Dimension getSize(Dimension rv) {
        if (rv == null) {
            return new Dimension(this.width, this.height);
        }
        rv.setSize(this.width, this.height);
        return rv;
    }

    /** Its position, written into the one that is passed in. */
    public Point getLocation(Point rv) {
        if (rv == null) {
            return new Point(this.x, this.y);
        }
        rv.setLocation(this.x, this.y);
        return rv;
    }

    /**
     * Whether it paints all of its pixels.
     *
     * <p>It answers `false` always here —a generic component paints nothing— so whatever is below
     * shows through. A subclass that fills its whole rectangle overrides it.
     */
    public boolean isOpaque() {
        return false;
    }

    /**
     * Whether it has no window of the system of its own.
     *
     * <p>It answers `true` always here: no component of this library gets to have one.
     */
    public boolean isLightweight() {
        return true;
    }

    /** Sets its preferred size; with `null` it goes back to working it out. */
    public void setPreferredSize(Dimension preferredSize) {
        Dimension old = this.prefSize;
        this.prefSize = preferredSize;
        this.prefSizeSet = preferredSize != null;
        this.firePropertyChange("preferredSize", old, preferredSize);
    }

    /** Whether somebody set its preferred size. */
    public boolean isPreferredSizeSet() {
        return this.prefSizeSet;
    }

    /** The preferred size: the one that was set, or the worked-out one if there is none. */
    public Dimension getPreferredSize() {
        return this.preferredSize();
    }

    /**
     * The preferred size.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getPreferredSize}.
     */
    @Deprecated
    public Dimension preferredSize() {
        if (this.prefSizeSet && this.prefSize != null) {
            return new Dimension(this.prefSize);
        }
        return this.getMinimumSize();
    }

    /** Sets its minimum size; with `null` it goes back to working it out. */
    public void setMinimumSize(Dimension minimumSize) {
        Dimension old = this.minSize;
        this.minSize = minimumSize;
        this.minSizeSet = minimumSize != null;
        this.firePropertyChange("minimumSize", old, minimumSize);
    }

    /** Whether somebody set its minimum size. */
    public boolean isMinimumSizeSet() {
        return this.minSizeSet;
    }

    /** The minimum size: the one that was set, or the current size if there is none. */
    public Dimension getMinimumSize() {
        return this.minimumSize();
    }

    /**
     * The minimum size.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getMinimumSize}.
     */
    @Deprecated
    public Dimension minimumSize() {
        if (this.minSizeSet && this.minSize != null) {
            return new Dimension(this.minSize);
        }
        return new Dimension(this.width, this.height);
    }

    /** Sets its maximum size; with `null` it goes back to working it out. */
    public void setMaximumSize(Dimension maximumSize) {
        Dimension old = this.maxSize;
        this.maxSize = maximumSize;
        this.maxSizeSet = maximumSize != null;
        this.firePropertyChange("maximumSize", old, maximumSize);
    }

    /** Whether somebody set its maximum size. */
    public boolean isMaximumSizeSet() {
        return this.maxSizeSet;
    }

    /**
     * The maximum size.
     *
     * <p>Unset, it is the maximum `short` in both directions —32767, which is what the JDK
     * returns—: it means "I have no limit", which is different from "I want to be enormous".
     */
    public Dimension getMaximumSize() {
        if (this.maxSizeSet && this.maxSize != null) {
            return new Dimension(this.maxSize);
        }
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /** How it aligns horizontally inside its container. */
    public float getAlignmentX() {
        return CENTER_ALIGNMENT;
    }

    /** How it aligns vertically. */
    public float getAlignmentY() {
        return CENTER_ALIGNMENT;
    }

    /**
     * At what height it has the baseline for that size.
     *
     * @return -1: a generic component has no baseline, and saying it has one anywhere would
     *     misalign the text of a whole row
     * @throws IllegalArgumentException if either measure is negative
     */
    public int getBaseline(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height must be >= 0");
        }
        return -1;
    }

    /** How the baseline moves when the height changes. */
    public BaselineResizeBehavior getBaselineResizeBehavior() {
        return BaselineResizeBehavior.OTHER;
    }

    /** Lays its children out again; a component with no children does nothing. */
    public void doLayout() {
        this.layout();
    }

    /**
     * Lays its children out again.
     *
     * @deprecated it is from the 1.0 model. Use {@link #doLayout}.
     */
    @Deprecated
    public void layout() {
    }

    /**
     * Lays out again if it was needed.
     *
     * <p>It marks the component as valid from the bottom up; a real container does it by overriding
     * this.
     */
    public void validate() {
        synchronized (this.getTreeLock()) {
            this.valid = true;
        }
    }

    /**
     * Marks that laying out has to happen again.
     *
     * <p>It propagates **upwards**: if a child changed preferred size, the parent has to work its
     * own out again. Hence invalidating being cheap and validating expensive.
     *
     * <p>Now then, it goes up **only if the parent was valid**. If it was invalid already, somebody
     * invalidated it earlier and the branch above has found out: going on up would be walking the
     * tree again to change nothing. And it is not only efficiency: a layout that is placing its
     * children does a `setBounds` per child, and each one would invalidate **it in the middle of
     * the work**, throwing away what it had just worked out. With the guard, the invalid parent
     * —which is what a container is while it is being laid out— finds out nothing.
     */
    public void invalidate() {
        synchronized (this.getTreeLock()) {
            this.valid = false;
            this.prefSize = this.prefSizeSet ? this.prefSize : null;
            this.minSize = this.minSizeSet ? this.minSize : null;
            this.maxSize = this.maxSizeSet ? this.maxSize : null;
            this.invalidateParent();
        }
    }

    /** Invalidates the parent, if there is one and it was valid. */
    void invalidateParent() {
        Container p = this.parent;
        if (p != null) {
            p.invalidateIfValid();
        }
    }

    /** It invalidates itself only if it was valid; if it was invalid there is nothing to report. */
    void invalidateIfValid() {
        if (this.isValid()) {
            this.invalidate();
        }
    }

    /** Invalidates and asks for the branch to be revalidated. */
    public void revalidate() {
        this.invalidate();
        Container p = this.parent;
        if (p != null) {
            p.validate();
        }
    }

    /** Whether it does not need laying out again. */
    public boolean isValid() {
        return this.valid;
    }

    /**
     * A context to draw over this component.
     *
     * @return `null` always: the component is not on a screen, and it is what the JDK returns in
     *     that case. For drawing onto pixels there is {@link java.awt.image.BufferedImage}.
     */
    public Graphics getGraphics() {
        return null;
    }

    /**
     * The measures of that font.
     *
     * @throws NullPointerException if the font is `null`
     */
    public FontMetrics getFontMetrics(Font font) {
        return this.getToolkit().getFontMetrics(font);
    }

    /** The cursor; inherited from the parent if it has none of its own. */
    public Cursor getCursor() {
        Cursor c = this.cursor;
        if (c != null) {
            return c;
        }
        Container p = this.parent;
        if (p != null) {
            return p.getCursor();
        }
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    /** Gives it a cursor of its own. */
    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    /** Whether it has a cursor of its own. */
    public boolean isCursorSet() {
        return this.cursor != null;
    }

    /**
     * Draws the component.
     *
     * <p>It does nothing: a generic component has nothing to draw. The subclasses override it.
     */
    public void paint(Graphics g) {
    }

    /**
     * Clears the background and draws.
     *
     * <p>It is what gets called when repainting a component that was already drawn. Keeping it
     * apart from {@link #paint} lets a component that knows it is going to cover everything skip
     * the clearing.
     */
    public void update(Graphics g) {
        if (this.isOpaque()) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.width, this.height);
            g.setColor(this.getForeground());
        }
        this.paint(g);
    }

    /** Draws this component and all of its children. */
    public void paintAll(Graphics g) {
        if (this.isShowing()) {
            this.paint(g);
        }
    }

    /**
     * Draws the component for printing.
     *
     * <p>By default it is the same as painting it. It is kept apart so that a component can print
     * differently from how it looks — with no dark background, for instance.
     */
    public void print(Graphics g) {
        this.paint(g);
    }

    /** Prints this component and all of its children. */
    public void printAll(Graphics g) {
        this.print(g);
    }

    /**
     * Asks for it to be drawn again.
     *
     * <p>It does nothing: repainting is queueing a request on the event queue for the system to
     * serve, and with no window there is nothing on a screen to refresh. It is not a silent
     * discarding of work — it is that the work does not exist.
     */
    public void repaint() {
        this.repaint(0, 0, 0, this.width, this.height);
    }

    /** Like the previous one, with a time limit. */
    public void repaint(long tm) {
        this.repaint(tm, 0, 0, this.width, this.height);
    }

    /** Like the previous one, of that rectangle only. */
    public void repaint(int x, int y, int width, int height) {
        this.repaint(0, x, y, width, height);
    }

    /** Like the previous one, with a time limit and a rectangle. */
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /** Whether the system's repaint requests are to be ignored. */
    public boolean getIgnoreRepaint() {
        return this.ignoreRepaint;
    }

    /**
     * Declares whether they are to be ignored.
     *
     * <p>It serves the applications that draw every frame themselves: the system's repainting would
     * only add work and flicker for them.
     */
    public void setIgnoreRepaint(boolean ignoreRepaint) {
        this.ignoreRepaint = ignoreRepaint;
    }

    /** Whether that point, relative to the component, falls inside. */
    public boolean contains(int x, int y) {
        return this.inside(x, y);
    }

    /**
     * Whether that point falls inside.
     *
     * @deprecated it is from the 1.0 model. Use {@link #contains(int, int)}.
     */
    @Deprecated
    public boolean inside(int x, int y) {
        return x >= 0 && x < this.width && y >= 0 && y < this.height;
    }

    /**
     * Whether that point falls inside.
     *
     * @throws NullPointerException if the point is `null`
     */
    public boolean contains(Point p) {
        return this.contains(p.x, p.y);
    }

    /** Which component is at that point: itself, or `null` if the point falls outside. */
    public Component getComponentAt(int x, int y) {
        return this.locate(x, y);
    }

    /**
     * Which component is at that point.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getComponentAt(int, int)}.
     */
    @Deprecated
    public Component locate(int x, int y) {
        return this.contains(x, y) ? this : null;
    }

    /**
     * Which component is at that point.
     *
     * @throws NullPointerException if the point is `null`
     */
    public Component getComponentAt(Point p) {
        return this.getComponentAt(p.x, p.y);
    }

    /**
     * Where the mouse is over this component.
     *
     * @return `null` always: the component is not on a screen, so the mouse cannot be over it
     * @throws HeadlessException if there is no screen
     */
    public Point getMousePosition() throws HeadlessException {
        return null;
    }

    /**
     * Sends it an event of the old model.
     *
     * @deprecated it is from the 1.0 model. Use {@link #dispatchEvent}.
     */
    @Deprecated
    public void deliverEvent(Event e) {
        this.postEvent(e);
    }

    /**
     * Sends the parent an event of the old model.
     *
     * @deprecated it is from the 1.0 model. Use {@link #dispatchEvent}.
     */
    @Deprecated
    public boolean postEvent(Event e) {
        Container p = this.parent;
        if (p != null) {
            return p.postEvent(e);
        }
        return false;
    }

    /**
     * Delivers an event to this component.
     *
     * <p>It is `final`: the extension point is {@link #processEvent}, not this one. That it is so
     * lets the system do its part —marking the event, consuming it if needed— before the component
     * sees it.
     */
    public final void dispatchEvent(AWTEvent e) {
        this.processEvent(e);
    }

    /**
     * Sorts the event out and hands it to the method of its family.
     *
     * <p>It is the place to intercept **everything** that reaches the component. A subclass that
     * overrides it has to call `super` or the listeners stop receiving.
     */
    protected void processEvent(AWTEvent e) {
        if (e instanceof FocusEvent) {
            this.processFocusEvent((FocusEvent) e);
        } else if (e instanceof MouseWheelEvent) {
            this.processMouseWheelEvent((MouseWheelEvent) e);
        } else if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) e;
            int id = me.getID();
            if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED) {
                this.processMouseMotionEvent(me);
            } else {
                this.processMouseEvent(me);
            }
        } else if (e instanceof KeyEvent) {
            this.processKeyEvent((KeyEvent) e);
        } else if (e instanceof ComponentEvent) {
            this.processComponentEvent((ComponentEvent) e);
        } else if (e instanceof InputMethodEvent) {
            this.processInputMethodEvent((InputMethodEvent) e);
        } else if (e instanceof HierarchyEvent) {
            HierarchyEvent he = (HierarchyEvent) e;
            if (he.getID() == HierarchyEvent.HIERARCHY_CHANGED) {
                this.processHierarchyEvent(he);
            } else {
                this.processHierarchyBoundsEvent(he);
            }
        }
    }

    /** Tells the component listeners. */
    protected void processComponentEvent(ComponentEvent e) {
        ComponentListener l = this.componentListener;
        if (l == null) {
            return;
        }
        int id = e.getID();
        if (id == ComponentEvent.COMPONENT_RESIZED) {
            l.componentResized(e);
        } else if (id == ComponentEvent.COMPONENT_MOVED) {
            l.componentMoved(e);
        } else if (id == ComponentEvent.COMPONENT_SHOWN) {
            l.componentShown(e);
        } else if (id == ComponentEvent.COMPONENT_HIDDEN) {
            l.componentHidden(e);
        }
    }

    /** Tells the focus listeners. */
    protected void processFocusEvent(FocusEvent e) {
        FocusListener l = this.focusListener;
        if (l == null) {
            return;
        }
        if (e.getID() == FocusEvent.FOCUS_GAINED) {
            l.focusGained(e);
        } else if (e.getID() == FocusEvent.FOCUS_LOST) {
            l.focusLost(e);
        }
    }

    /** Tells the keyboard listeners. */
    protected void processKeyEvent(KeyEvent e) {
        KeyListener l = this.keyListener;
        if (l == null) {
            return;
        }
        int id = e.getID();
        if (id == KeyEvent.KEY_TYPED) {
            l.keyTyped(e);
        } else if (id == KeyEvent.KEY_PRESSED) {
            l.keyPressed(e);
        } else if (id == KeyEvent.KEY_RELEASED) {
            l.keyReleased(e);
        }
    }

    /** Tells the mouse button listeners. */
    protected void processMouseEvent(MouseEvent e) {
        MouseListener l = this.mouseListener;
        if (l == null) {
            return;
        }
        int id = e.getID();
        if (id == MouseEvent.MOUSE_PRESSED) {
            l.mousePressed(e);
        } else if (id == MouseEvent.MOUSE_RELEASED) {
            l.mouseReleased(e);
        } else if (id == MouseEvent.MOUSE_CLICKED) {
            l.mouseClicked(e);
        } else if (id == MouseEvent.MOUSE_ENTERED) {
            l.mouseEntered(e);
        } else if (id == MouseEvent.MOUSE_EXITED) {
            l.mouseExited(e);
        }
    }

    /** Tells the mouse motion listeners. */
    protected void processMouseMotionEvent(MouseEvent e) {
        MouseMotionListener l = this.mouseMotionListener;
        if (l == null) {
            return;
        }
        if (e.getID() == MouseEvent.MOUSE_MOVED) {
            l.mouseMoved(e);
        } else if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            l.mouseDragged(e);
        }
    }

    /** Tells the wheel listeners. */
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        MouseWheelListener l = this.mouseWheelListener;
        if (l != null && e.getID() == MouseEvent.MOUSE_WHEEL) {
            l.mouseWheelMoved(e);
        }
    }

    /** Tells the input method listeners. */
    protected void processInputMethodEvent(InputMethodEvent e) {
        InputMethodListener l = this.inputMethodListener;
        if (l == null) {
            return;
        }
        if (e.getID() == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) {
            l.inputMethodTextChanged(e);
        } else if (e.getID() == InputMethodEvent.CARET_POSITION_CHANGED) {
            l.caretPositionChanged(e);
        }
    }

    /** Tells the tree listeners. */
    protected void processHierarchyEvent(HierarchyEvent e) {
        HierarchyListener l = this.hierarchyListener;
        if (l != null && e.getID() == HierarchyEvent.HIERARCHY_CHANGED) {
            l.hierarchyChanged(e);
        }
    }

    /** Tells the listeners of ancestor size changes. */
    protected void processHierarchyBoundsEvent(HierarchyEvent e) {
        HierarchyBoundsListener l = this.hierarchyBoundsListener;
        if (l == null) {
            return;
        }
        if (e.getID() == HierarchyEvent.ANCESTOR_MOVED) {
            l.ancestorMoved(e);
        } else if (e.getID() == HierarchyEvent.ANCESTOR_RESIZED) {
            l.ancestorResized(e);
        }
    }

    /**
     * Asks to receive those families of events.
     *
     * <p>Registering a listener switches it on by itself; this serves for receiving a family
     * **without** a listener, which is what a subclass that handles the events by overriding
     * `processXEvent` does.
     */
    protected final void enableEvents(long eventsToEnable) {
        this.eventMask = this.eventMask | eventsToEnable;
    }

    /** Stops receiving them. */
    protected final void disableEvents(long eventsToDisable) {
        this.eventMask = this.eventMask & ~eventsToDisable;
    }

    /**
     * Joins two events of the same family into one.
     *
     * @return `null` always: joining events is an optimisation of the queue, and with no queue
     *     there is none to join
     */
    protected AWTEvent coalesceEvents(AWTEvent existingEvent, AWTEvent newEvent) {
        return null;
    }

    /** Adds a component listener; a `null` is ignored. */
    public synchronized void addComponentListener(ComponentListener l) {
        if (l == null) {
            return;
        }
        this.componentListener = AWTEventMulticaster.add(this.componentListener, l);
        this.enableEvents(AWTEvent.COMPONENT_EVENT_MASK);
    }

    /** Removes that listener. */
    public synchronized void removeComponentListener(ComponentListener l) {
        if (l == null) {
            return;
        }
        this.componentListener = AWTEventMulticaster.remove(this.componentListener, l);
    }

    /** The component listeners. */
    public synchronized ComponentListener[] getComponentListeners() {
        return AWTEventMulticaster.getListeners(this.componentListener, ComponentListener.class);
    }

    /** Adds a focus listener; a `null` is ignored. */
    public synchronized void addFocusListener(FocusListener l) {
        if (l == null) {
            return;
        }
        this.focusListener = AWTEventMulticaster.add(this.focusListener, l);
        this.enableEvents(AWTEvent.FOCUS_EVENT_MASK);
    }

    /** Removes that listener. */
    public synchronized void removeFocusListener(FocusListener l) {
        if (l == null) {
            return;
        }
        this.focusListener = AWTEventMulticaster.remove(this.focusListener, l);
    }

    /** The focus listeners. */
    public synchronized FocusListener[] getFocusListeners() {
        return AWTEventMulticaster.getListeners(this.focusListener, FocusListener.class);
    }

    /** Adds a tree listener; a `null` is ignored. */
    public void addHierarchyListener(HierarchyListener l) {
        if (l == null) {
            return;
        }
        synchronized (this) {
            this.hierarchyListener = AWTEventMulticaster.add(this.hierarchyListener, l);
            this.enableEvents(AWTEvent.HIERARCHY_EVENT_MASK);
        }
    }

    /** Removes that listener. */
    public void removeHierarchyListener(HierarchyListener l) {
        if (l == null) {
            return;
        }
        synchronized (this) {
            this.hierarchyListener = AWTEventMulticaster.remove(this.hierarchyListener, l);
        }
    }

    /** The tree listeners. */
    public synchronized HierarchyListener[] getHierarchyListeners() {
        return AWTEventMulticaster.getListeners(this.hierarchyListener, HierarchyListener.class);
    }

    /** Adds a listener of ancestor size changes; a `null` is ignored. */
    public void addHierarchyBoundsListener(HierarchyBoundsListener l) {
        if (l == null) {
            return;
        }
        synchronized (this) {
            this.hierarchyBoundsListener =
                    AWTEventMulticaster.add(this.hierarchyBoundsListener, l);
            this.enableEvents(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK);
        }
    }

    /** Removes that listener. */
    public void removeHierarchyBoundsListener(HierarchyBoundsListener l) {
        if (l == null) {
            return;
        }
        synchronized (this) {
            this.hierarchyBoundsListener =
                    AWTEventMulticaster.remove(this.hierarchyBoundsListener, l);
        }
    }

    /** The listeners of ancestor size changes. */
    public synchronized HierarchyBoundsListener[] getHierarchyBoundsListeners() {
        return AWTEventMulticaster.getListeners(this.hierarchyBoundsListener,
                HierarchyBoundsListener.class);
    }

    /** Adds a keyboard listener; a `null` is ignored. */
    public synchronized void addKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        this.keyListener = AWTEventMulticaster.add(this.keyListener, l);
        this.enableEvents(AWTEvent.KEY_EVENT_MASK);
    }

    /** Removes that listener. */
    public synchronized void removeKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        this.keyListener = AWTEventMulticaster.remove(this.keyListener, l);
    }

    /** The keyboard listeners. */
    public synchronized KeyListener[] getKeyListeners() {
        return AWTEventMulticaster.getListeners(this.keyListener, KeyListener.class);
    }

    /** Adds a mouse button listener; a `null` is ignored. */
    public synchronized void addMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        this.mouseListener = AWTEventMulticaster.add(this.mouseListener, l);
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }

    /** Removes that listener. */
    public synchronized void removeMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        this.mouseListener = AWTEventMulticaster.remove(this.mouseListener, l);
    }

    /** The mouse button listeners. */
    public synchronized MouseListener[] getMouseListeners() {
        return AWTEventMulticaster.getListeners(this.mouseListener, MouseListener.class);
    }

    /** Adds a mouse motion listener; a `null` is ignored. */
    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        if (l == null) {
            return;
        }
        this.mouseMotionListener = AWTEventMulticaster.add(this.mouseMotionListener, l);
        this.enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    /** Removes that listener. */
    public synchronized void removeMouseMotionListener(MouseMotionListener l) {
        if (l == null) {
            return;
        }
        this.mouseMotionListener = AWTEventMulticaster.remove(this.mouseMotionListener, l);
    }

    /** The mouse motion listeners. */
    public synchronized MouseMotionListener[] getMouseMotionListeners() {
        return AWTEventMulticaster.getListeners(this.mouseMotionListener,
                MouseMotionListener.class);
    }

    /** Adds a wheel listener; a `null` is ignored. */
    public synchronized void addMouseWheelListener(MouseWheelListener l) {
        if (l == null) {
            return;
        }
        this.mouseWheelListener = AWTEventMulticaster.add(this.mouseWheelListener, l);
        this.enableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
    }

    /** Removes that listener. */
    public synchronized void removeMouseWheelListener(MouseWheelListener l) {
        if (l == null) {
            return;
        }
        this.mouseWheelListener = AWTEventMulticaster.remove(this.mouseWheelListener, l);
    }

    /** The wheel listeners. */
    public synchronized MouseWheelListener[] getMouseWheelListeners() {
        return AWTEventMulticaster.getListeners(this.mouseWheelListener,
                MouseWheelListener.class);
    }

    /** Adds an input method listener; a `null` is ignored. */
    public synchronized void addInputMethodListener(InputMethodListener l) {
        if (l == null) {
            return;
        }
        this.inputMethodListener = AWTEventMulticaster.add(this.inputMethodListener, l);
        this.enableEvents(AWTEvent.INPUT_METHOD_EVENT_MASK);
    }

    /** Removes that listener. */
    public synchronized void removeInputMethodListener(InputMethodListener l) {
        if (l == null) {
            return;
        }
        this.inputMethodListener = AWTEventMulticaster.remove(this.inputMethodListener, l);
    }

    /** The input method listeners. */
    public synchronized InputMethodListener[] getInputMethodListeners() {
        return AWTEventMulticaster.getListeners(this.inputMethodListener,
                InputMethodListener.class);
    }

    /**
     * The listeners of that class.
     *
     * <p>The {@code T extends EventListener} bound is what keeps the question well posed: a class
     * that is not a listener one cannot be passed without raw types.
     *
     * @throws NullPointerException if the class is `null`
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if (listenerType == ComponentListener.class) {
            l = this.componentListener;
        } else if (listenerType == FocusListener.class) {
            l = this.focusListener;
        } else if (listenerType == HierarchyListener.class) {
            l = this.hierarchyListener;
        } else if (listenerType == HierarchyBoundsListener.class) {
            l = this.hierarchyBoundsListener;
        } else if (listenerType == KeyListener.class) {
            l = this.keyListener;
        } else if (listenerType == MouseListener.class) {
            l = this.mouseListener;
        } else if (listenerType == MouseMotionListener.class) {
            l = this.mouseMotionListener;
        } else if (listenerType == MouseWheelListener.class) {
            l = this.mouseWheelListener;
        } else if (listenerType == InputMethodListener.class) {
            l = this.inputMethodListener;
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }

    /**
     * The write state of the window that contains it.
     *
     * @return `null` if it hangs from no window
     */
    public InputContext getInputContext() {
        Container p = this.parent;
        if (p == null) {
            return null;
        }
        return p.getInputContext();
    }

    /**
     * What the input method needs to ask this component.
     *
     * @return `null`: a generic component shows no text being composed
     */
    public InputMethodRequests getInputMethodRequests() {
        return null;
    }

    /** The drop target attached, or `null`. */
    public synchronized java.awt.dnd.DropTarget getDropTarget() {
        return this.dropTarget;
    }

    /**
     * Attaches a drop target to it.
     *
     * <p>It detaches the previous one and tells the new one which its component is: both sides of
     * the relation are kept consistent from here.
     */
    public synchronized void setDropTarget(java.awt.dnd.DropTarget dt) {
        if (dt == this.dropTarget) {
            return;
        }
        java.awt.dnd.DropTarget previous = this.dropTarget;
        this.dropTarget = dt;
        if (previous != null && previous.getComponent() == this) {
            previous.setComponent(null);
        }
        if (dt != null && dt.getComponent() != this) {
            dt.setComponent(this);
        }
    }

    /**
     * Attaches a popup menu to it.
     *
     * @throws NullPointerException if the menu is `null`
     */
    public void add(PopupMenu popup) {
        synchronized (this.getTreeLock()) {
            if (popup.getParent() != null) {
                ((MenuContainer) popup.getParent()).remove(popup);
            }
            this.popups.add(popup);
            popup.setParent(this);
        }
    }

    /** Takes a popup menu away from it. */
    public void remove(MenuComponent popup) {
        synchronized (this.getTreeLock()) {
            if (this.popups.remove(popup)) {
                popup.setParent(null);
            }
        }
    }

    /** Notifies that it can be shown. */
    public void addNotify() {
    }

    /** Notifies that it can no longer be shown. */
    public void removeNotify() {
    }

    /** Whether it can receive the focus. */
    public boolean isFocusable() {
        return this.focusable;
    }

    /** Declares whether it can receive the focus. */
    public void setFocusable(boolean focusable) {
        boolean old;
        synchronized (this) {
            old = this.focusable;
            this.focusable = focusable;
        }
        this.firePropertyChange("focusable", old, focusable);
    }

    /**
     * Whether it can receive the focus.
     *
     * @deprecated it is from the 1.1 model. Use {@link #isFocusable}.
     */
    @Deprecated
    public boolean isFocusTraversable() {
        return this.focusable;
    }

    /**
     * The keys that walk the focus in that direction.
     *
     * <p>If none were set on it, they are inherited from the parent; if there is no parent, they
     * are the default ones.
     *
     * @throws IllegalArgumentException if the direction is not one of the four
     */
    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        this.checkEventId(id);
        Set<AWTKeyStroke> s = this.focusTraversalKeys[id];
        if (s != null) {
            return s;
        }
        Container p = this.parent;
        if (p != null) {
            return p.getFocusTraversalKeys(id);
        }
        return defaultKeyStrokes(id);
    }

    /**
     * Changes the traversal keys in that direction.
     *
     * <p>With `null` they go back to being inherited from the parent.
     *
     * @throws IllegalArgumentException if the direction is not one of the four, if the set carries
     *     a `null`, or if it carries a key-released shortcut
     */
    public void setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) {
        this.checkEventId(id);
        Set<AWTKeyStroke> fresh = null;
        if (keystrokes != null) {
            Set<AWTKeyStroke> copy = new HashSet<AWTKeyStroke>();
            java.util.Iterator<? extends AWTKeyStroke> it = keystrokes.iterator();
            while (it.hasNext()) {
                AWTKeyStroke k = it.next();
                if (k == null) {
                    throw new IllegalArgumentException(
                            "cannot set null focus traversal key");
                }
                // A shortcut on release is no good for traversing: by the time the key is released,
                // the focus has already moved with the press and the traversal would jump twice.
                if (k.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                    throw new IllegalArgumentException(
                            "focus traversal keys cannot map to KEY_TYPED events");
                }
                copy.add(k);
            }
            fresh = Collections.unmodifiableSet(copy);
        }
        Set<AWTKeyStroke> old = this.focusTraversalKeys[id];
        this.focusTraversalKeys[id] = fresh;
        this.firePropertyChange(eventIdName(id), old, fresh);
    }

    /** Whether keys of its own were set on this component in that direction. */
    public boolean areFocusTraversalKeysSet(int id) {
        this.checkEventId(id);
        return this.focusTraversalKeys[id] != null;
    }

    /**
     * Checks that the direction is one of the four.
     *
     * @throws IllegalArgumentException if it is not
     */
    private void checkEventId(int id) {
        if (id < 0 || id > 3) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }
    }

    /** The property name that corresponds to that direction. */
    private static String eventIdName(int id) {
        if (id == 0) {
            return "forwardFocusTraversalKeys";
        }
        if (id == 1) {
            return "backwardFocusTraversalKeys";
        }
        if (id == 2) {
            return "upCycleFocusTraversalKeys";
        }
        return "downCycleFocusTraversalKeys";
    }

    /**
     * Whether the component handles the traversal keys instead of receiving them as keyboard input.
     */
    public boolean getFocusTraversalKeysEnabled() {
        return this.focusTraversalKeysEnabled;
    }

    /**
     * Declares whether it handles them.
     *
     * <p>Switching it off is what lets a text editor receive the tab key as a character instead of
     * losing the focus.
     */
    public void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) {
        boolean old;
        synchronized (this) {
            old = this.focusTraversalKeysEnabled;
            this.focusTraversalKeysEnabled = focusTraversalKeysEnabled;
        }
        this.firePropertyChange("focusTraversalKeysEnabled", old, focusTraversalKeysEnabled);
    }

    /**
     * Asks for the focus.
     *
     * <p>It does nothing: moving the focus is decided by the focus manager out of what the
     * windowing system reports, and there is neither of the two. The method returns nothing, so it
     * does not claim to have got it.
     */
    public void requestFocus() {
    }

    /** Like the previous one, declaring why it is asked for. */
    public void requestFocus(FocusEvent.Cause cause) {
    }

    /**
     * Asks for the focus, saying whether the change is temporary.
     *
     * @return `false` always: the focus could not be moved because there is no focus manager
     */
    protected boolean requestFocus(boolean temporary) {
        return false;
    }

    /**
     * Like the previous one, declaring why.
     *
     * @return `false` always
     */
    protected boolean requestFocus(boolean temporary, FocusEvent.Cause cause) {
        return false;
    }

    /**
     * Asks for the focus only if its window has it already.
     *
     * @return `false` always
     */
    public boolean requestFocusInWindow() {
        return false;
    }

    /**
     * Like the previous one, declaring why.
     *
     * @return `false` always
     */
    public boolean requestFocusInWindow(FocusEvent.Cause cause) {
        return false;
    }

    /**
     * Like the previous one, saying whether the change is temporary.
     *
     * @return `false` always
     */
    protected boolean requestFocusInWindow(boolean temporary) {
        return false;
    }

    /** Whether it has the keyboard focus. */
    public boolean hasFocus() {
        return false;
    }

    /** Whether it is the component with the focus. */
    public boolean isFocusOwner() {
        return this.hasFocus();
    }

    /** Passes the focus to the next one in the traversal; it does nothing with no focus manager. */
    public void transferFocus() {
    }

    /** Passes the focus to the previous one. */
    public void transferFocusBackward() {
    }

    /** Goes up one focus cycle level. */
    public void transferFocusUpCycle() {
    }

    /**
     * Passes the focus to the next one.
     *
     * @deprecated it is from the 1.1 model. Use {@link #transferFocus}.
     */
    @Deprecated
    public void nextFocus() {
        this.transferFocus();
    }

    /** The root of the focus cycle it belongs to, or `null`. */
    public Container getFocusCycleRootAncestor() {
        Container p = this.parent;
        while (p != null) {
            if (p.isFocusCycleRoot()) {
                return p;
            }
            p = p.getParent();
        }
        return null;
    }

    /** Whether that container is the focus cycle root of this component. */
    public boolean isFocusCycleRoot(Container container) {
        return this.getFocusCycleRootAncestor() == container;
    }

    /** The graphics configuration; `null` with no window. */
    public GraphicsConfiguration getGraphicsConfiguration() {
        Container p = this.parent;
        if (p != null) {
            return p.getGraphicsConfiguration();
        }
        return null;
    }

    /**
     * An image to draw off screen.
     *
     * @return `null` always: with no window there is no destination format to fit it to. For
     *     drawing onto pixels there is {@link java.awt.image.BufferedImage}.
     */
    public Image createImage(int width, int height) {
        return null;
    }

    /**
     * An image from a producer of pixels.
     *
     * <p>This one does work: it needs no window, only the producer.
     *
     * @throws NullPointerException if the producer is `null`
     */
    public Image createImage(ImageProducer producer) {
        return this.getToolkit().createImage(producer);
    }

    /**
     * A volatile image.
     *
     * @return `null` always: a volatile image lives in the memory of the video device
     */
    public VolatileImage createVolatileImage(int width, int height) {
        return null;
    }

    /**
     * A volatile image with the capabilities asked for.
     *
     * @return `null` always, for the same reason
     * @throws AWTException if the capabilities cannot be met
     */
    public VolatileImage createVolatileImage(int width, int height, ImageCapabilities caps)
            throws AWTException {
        return null;
    }

    /**
     * Starts loading an image at that size.
     *
     * @return whether it is ready already
     */
    public boolean prepareImage(Image image, ImageObserver observer) {
        return this.prepareImage(image, -1, -1, observer);
    }

    /**
     * Like the previous one, at a concrete size.
     *
     * @return whether it is ready already
     */
    public boolean prepareImage(Image image, int width, int height, ImageObserver observer) {
        return this.getToolkit().prepareImage(image, width, height, observer);
    }

    /** How much of an image was loaded, as {@link ImageObserver} flags. */
    public int checkImage(Image image, ImageObserver observer) {
        return this.checkImage(image, -1, -1, observer);
    }

    /** Like the previous one, at a concrete size. */
    public int checkImage(Image image, int width, int height, ImageObserver observer) {
        return this.getToolkit().checkImage(image, width, height, observer);
    }

    /**
     * It is told that an image has made progress.
     *
     * <p>It repaints when new pixels arrive and stops listening when the image is complete or has
     * failed — which is what the return value means.
     */
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        if ((infoflags & (ALLBITS | FRAMEBITS)) != 0) {
            this.repaint();
            return false;
        }
        if ((infoflags & (ERROR | ABORT)) != 0) {
            return false;
        }
        if ((infoflags & SOMEBITS) != 0) {
            this.repaint();
        }
        return true;
    }

    /** Which side the content is read from. */
    public ComponentOrientation getComponentOrientation() {
        return this.componentOrientation;
    }

    /**
     * Declares which side it is read from.
     *
     * @throws NullPointerException if the orientation is `null`
     */
    public void setComponentOrientation(ComponentOrientation o) {
        ComponentOrientation old = this.componentOrientation;
        this.componentOrientation = o;
        this.firePropertyChange("componentOrientation", old, o);
        this.invalidate();
    }

    /**
     * Gives that orientation to itself and to every descendant.
     *
     * @throws NullPointerException if the orientation is `null`
     */
    public void applyComponentOrientation(ComponentOrientation orientation) {
        if (orientation == null) {
            throw new NullPointerException();
        }
        this.setComponentOrientation(orientation);
    }

    /** Shapes the clip used by the mixing of heavyweight and lightweight components. */
    public void setMixingCutoutShape(Shape shape) {
    }

    /** Adds someone to tell about the property changes. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this.getObjectLock()) {
            if (this.changeSupport == null) {
                this.changeSupport = new PropertyChangeSupport(this);
            }
            this.changeSupport.addPropertyChangeListener(listener);
        }
    }

    /** Removes that listener. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null || this.changeSupport == null) {
            return;
        }
        synchronized (this.getObjectLock()) {
            this.changeSupport.removePropertyChangeListener(listener);
        }
    }

    /** The property change listeners. */
    public PropertyChangeListener[] getPropertyChangeListeners() {
        synchronized (this.getObjectLock()) {
            if (this.changeSupport == null) {
                return new PropertyChangeListener[0];
            }
            return this.changeSupport.getPropertyChangeListeners();
        }
    }

    /** Adds a listener for one particular property. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this.getObjectLock()) {
            if (this.changeSupport == null) {
                this.changeSupport = new PropertyChangeSupport(this);
            }
            this.changeSupport.addPropertyChangeListener(propertyName, listener);
        }
    }

    /** Removes that listener of that property. */
    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        if (listener == null || this.changeSupport == null) {
            return;
        }
        synchronized (this.getObjectLock()) {
            this.changeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }

    /** The listeners of that property. */
    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        synchronized (this.getObjectLock()) {
            if (this.changeSupport == null) {
                return new PropertyChangeListener[0];
            }
            return this.changeSupport.getPropertyChangeListeners(propertyName);
        }
    }

    /** Reports that a property changed. */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        PropertyChangeSupport s = this.changeSupport;
        if (s != null) {
            s.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /** Reports that a boolean property changed. */
    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        PropertyChangeSupport s = this.changeSupport;
        if (s != null) {
            s.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /** Reports that an int property changed. */
    protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
        PropertyChangeSupport s = this.changeSupport;
        if (s != null) {
            s.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /** Reports that a `byte` property changed. */
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
        this.firePropertyChange(propertyName, Byte.valueOf(oldValue), Byte.valueOf(newValue));
    }

    /** Reports that a `char` property changed. */
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
        this.firePropertyChange(propertyName, Character.valueOf(oldValue),
                Character.valueOf(newValue));
    }

    /** Reports that a `short` property changed. */
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
        this.firePropertyChange(propertyName, Short.valueOf(oldValue), Short.valueOf(newValue));
    }

    /** Reports that a `long` property changed. */
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
        this.firePropertyChange(propertyName, Long.valueOf(oldValue), Long.valueOf(newValue));
    }

    /** Reports that a `float` property changed. */
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
        this.firePropertyChange(propertyName, Float.valueOf(oldValue), Float.valueOf(newValue));
    }

    /** Reports that a `double` property changed. */
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
        this.firePropertyChange(propertyName, Double.valueOf(oldValue), Double.valueOf(newValue));
    }

    /**
     * Handles an event of the old model.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processEvent}.
     */
    @Deprecated
    public boolean handleEvent(Event evt) {
        return false;
    }

    /**
     * The mouse was pressed.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processMouseEvent}.
     */
    @Deprecated
    public boolean mouseDown(Event evt, int x, int y) {
        return false;
    }

    /**
     * The mouse was dragged.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processMouseMotionEvent}.
     */
    @Deprecated
    public boolean mouseDrag(Event evt, int x, int y) {
        return false;
    }

    /**
     * The mouse was released.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processMouseEvent}.
     */
    @Deprecated
    public boolean mouseUp(Event evt, int x, int y) {
        return false;
    }

    /**
     * The mouse was moved.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processMouseMotionEvent}.
     */
    @Deprecated
    public boolean mouseMove(Event evt, int x, int y) {
        return false;
    }

    /**
     * The mouse came in.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processMouseEvent}.
     */
    @Deprecated
    public boolean mouseEnter(Event evt, int x, int y) {
        return false;
    }

    /**
     * The mouse went out.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processMouseEvent}.
     */
    @Deprecated
    public boolean mouseExit(Event evt, int x, int y) {
        return false;
    }

    /**
     * A key was pressed.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processKeyEvent}.
     */
    @Deprecated
    public boolean keyDown(Event evt, int key) {
        return false;
    }

    /**
     * A key was released.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processKeyEvent}.
     */
    @Deprecated
    public boolean keyUp(Event evt, int key) {
        return false;
    }

    /**
     * An action was run.
     *
     * @deprecated it is from the 1.0 model. Use an {@code ActionListener}.
     */
    @Deprecated
    public boolean action(Event evt, Object what) {
        return false;
    }

    /**
     * It gained the focus.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processFocusEvent}.
     */
    @Deprecated
    public boolean gotFocus(Event evt, Object what) {
        return false;
    }

    /**
     * It lost the focus.
     *
     * @deprecated it is from the 1.0 model. Use {@link #processFocusEvent}.
     */
    @Deprecated
    public boolean lostFocus(Event evt, Object what) {
        return false;
    }

    /** Writes the component tree to the standard output. */
    public void list() {
        this.list(System.out, 0);
    }

    /** Writes it to that stream. */
    public void list(PrintStream out) {
        this.list(out, 0);
    }

    /** Writes it with that indentation. */
    public void list(PrintStream out, int indent) {
        for (int i = 0; i < indent; i++) {
            out.print(" ");
        }
        out.println(this.toString());
    }

    /** Writes it to that writer. */
    public void list(PrintWriter out) {
        this.list(out, 0);
    }

    /** Writes it with that indentation. */
    public void list(PrintWriter out, int indent) {
        for (int i = 0; i < indent; i++) {
            out.print(" ");
        }
        out.println(this.toString());
    }

    /** The description of the component, without the class name. */
    protected String paramString() {
        String s = this.getName() + "," + this.x + "," + this.y + "," + this.width + "x"
                + this.height;
        if (!this.valid) {
            s = s + ",invalid";
        }
        if (!this.visible) {
            s = s + ",hidden";
        }
        if (!this.enabled) {
            s = s + ",disabled";
        }
        return s;
    }

    public String toString() {
        return this.getClass().getName() + "[" + this.paramString() + "]";
    }

    /**
     * The accessibility information of this component.
     *
     * <p>It returns `null` while no concrete subclass has built its own, and it **does not build
     * one itself**: a generic component does not know which role it has, and building a context
     * that answers `AWT_COMPONENT` to everything would be worse than not answering. The concrete
     * subclasses override it.
     */
    public AccessibleContext getAccessibleContext() {
        return this.accessibleContext;
    }

    /**
     * The accessibility of a component.
     *
     * <p>It reports what can be known without a screen: the name, the role, whether it is enabled,
     * whether it is declared visible and whether it can receive the focus. {@code SHOWING} is asked
     * for too and comes out false, because the component never reaches a screen; {@code FOCUSED} is
     * not reported at all, because there is no focus manager that could have given it.
     */
    protected abstract class AccessibleAWTComponent extends AccessibleContext {

        /** For the subclasses. */
        protected AccessibleAWTComponent() {
        }

        /** {@code AWT_COMPONENT}: the non-specific role, which the concrete subclasses refine. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.AWT_COMPONENT;
        }

        /** The states that can be known without a screen. */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = new AccessibleStateSet();
            if (Component.this.isEnabled()) {
                s.add(AccessibleState.ENABLED);
            }
            if (Component.this.isFocusable()) {
                s.add(AccessibleState.FOCUSABLE);
            }
            if (Component.this.isVisible()) {
                s.add(AccessibleState.VISIBLE);
            }
            if (Component.this.isShowing()) {
                s.add(AccessibleState.SHOWING);
            }
            return s;
        }

        /** The name of the component. */
        public String getAccessibleName() {
            return Component.this.getName();
        }

        /** Zero: a component with no children. */
        public int getAccessibleChildrenCount() {
            return 0;
        }

        /** Always `null`: a component has no accessible children. */
        public javax.accessibility.Accessible getAccessibleChild(int i) {
            return null;
        }

        /** Its position inside the parent, or -1 if it has none. */
        public int getAccessibleIndexInParent() {
            Container p = Component.this.getParent();
            if (p == null) {
                return -1;
            }
            Component[] children = p.getComponents();
            for (int i = 0; i < children.length; i++) {
                if (children[i] == Component.this) {
                    return i;
                }
            }
            return -1;
        }

        /** The locale of the component. */
        public Locale getLocale() {
            return Component.this.getLocale();
        }
    }
}
