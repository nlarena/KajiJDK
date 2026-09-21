package java.awt;

import java.util.EventObject;

/**
 * The root of every AWT event.
 *
 * <p>An event is three things: **where** it came from (the source, inherited from
 * {@link EventObject}), **what** happened (the identifier) and **whether someone already took care
 * of it** (consumed). Everything else is added by the subclasses.
 *
 * <p>That the type is an `int` and not a hierarchy of classes is a decision that shows: one same
 * {@link java.awt.event.MouseEvent} serves for pressing, releasing and dragging, and whoever
 * receives it looks at the identifier. That allows dispatching by ranges —each family has its block
 * of numbers— but forces comparing integers where a `switch` over types would be clearer.
 *
 * <p>**Consuming** an event means saying "I will take care of this": the system is not going to
 * give it the default treatment. It is what lets a component keep a key to itself and stop it from
 * also firing the menu shortcut.
 *
 * <p>The masks are bits a component turns on to ask that the events of that family reach it.
 * Delivering events nobody listens to is expensive, so by default almost nothing is delivered and
 * each `addXListener` turns on the mask that corresponds.
 */
public abstract class AWTEvent extends EventObject {

    private static final long serialVersionUID = -1825314779160409405L;

    /** The size and visibility events of a component. */
    public static final long COMPONENT_EVENT_MASK = 0x01;

    /** The ones for adding and removing children of a container. */
    public static final long CONTAINER_EVENT_MASK = 0x02;

    /** The ones for gaining and losing the focus. */
    public static final long FOCUS_EVENT_MASK = 0x04;

    /** The keyboard ones. */
    public static final long KEY_EVENT_MASK = 0x08;

    /** The mouse button ones. */
    public static final long MOUSE_EVENT_MASK = 0x10;

    /** The mouse movement ones, kept apart because there are far more of them. */
    public static final long MOUSE_MOTION_EVENT_MASK = 0x20;

    /** The window ones. */
    public static final long WINDOW_EVENT_MASK = 0x40;

    /** The action ones: the button that was pressed, the option that was chosen. */
    public static final long ACTION_EVENT_MASK = 0x80;

    /** The scrollbar ones. */
    public static final long ADJUSTMENT_EVENT_MASK = 0x100;

    /** The ones for selecting an item. */
    public static final long ITEM_EVENT_MASK = 0x200;

    /** The ones for a text change. */
    public static final long TEXT_EVENT_MASK = 0x400;

    /** The input method ones. */
    public static final long INPUT_METHOD_EVENT_MASK = 0x800;

    /** The repaint ones. */
    public static final long PAINT_EVENT_MASK = 0x2000;

    /** The ones that carry work to run on the event thread. */
    public static final long INVOCATION_EVENT_MASK = 0x4000;

    /** The ones for changes in the component tree. */
    public static final long HIERARCHY_EVENT_MASK = 0x8000;

    /** The ones for size changes within the tree. */
    public static final long HIERARCHY_BOUNDS_EVENT_MASK = 0x10000;

    /** The mouse wheel ones. */
    public static final long MOUSE_WHEEL_EVENT_MASK = 0x20000;

    /** The ones for minimising and restoring a window. */
    public static final long WINDOW_STATE_EVENT_MASK = 0x40000;

    /** The window-level focus ones. */
    public static final long WINDOW_FOCUS_EVENT_MASK = 0x80000;

    /**
     * The highest identifier reserved by AWT.
     *
     * <p>Whoever invents their own events has to number them above this, or they will clash with a
     * family AWT adds later on.
     */
    public static final int RESERVED_ID_MAX = 1999;

    /** What happened. */
    protected int id;

    /** Whether someone already took charge. */
    protected boolean consumed;

    /**
     * With the source and the identifier.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public AWTEvent(Object source, int id) {
        super(source);
        this.id = id;
        this.consumed = false;
    }

    /**
     * From an event of the old model.
     *
     * @throws NullPointerException if the event is `null`
     */
    public AWTEvent(Event event) {
        this(event.target, event.id);
    }

    /**
     * Changes the source.
     *
     * <p>It serves to dispatch the same event again from another component, which is how a
     * container hands a child an event that arrived at it.
     */
    public void setSource(Object newSource) {
        this.source = newSource;
    }

    /** What happened. */
    public int getID() {
        return this.id;
    }

    public String toString() {
        String name = "";
        if (this.source instanceof Component) {
            name = ((Component) this.source).getName();
        }
        return this.getClass().getName() + "[" + this.paramString() + "] on "
                + (name != null && !name.isEmpty() ? name : this.source);
    }

    /**
     * The description of the event, without the class name or the source.
     *
     * <p>Each subclass adds its own; {@link #toString} puts the wrapper on just once.
     */
    public String paramString() {
        return "";
    }

    /** Marks that someone took charge. */
    protected void consume() {
        this.consumed = true;
    }

    /** Whether someone already took charge. */
    protected boolean isConsumed() {
        return this.consumed;
    }
}
