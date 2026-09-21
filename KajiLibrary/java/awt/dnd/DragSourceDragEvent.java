package java.awt.dnd;

/**
 * The drag is passing over a destination, seen from the source.
 *
 * <p>It brings **three** different actions and confusing them is easy:
 *
 * <ul>
 *   <li>{@link #getUserAction}: what the user asks for, according to the keys they have held down;
 *   <li>{@link #getTargetActions}: what the destination declared it accepts;
 *   <li>{@link #getDropAction}: the intersection of the two, which is what is actually going to
 *       happen.
 * </ul>
 *
 * <p>The third one is the one that decides the cursor, and it is zero when the user asks for
 * something the destination does not accept.
 */
public class DragSourceDragEvent extends DragSourceEvent {

    private static final long serialVersionUID = 481346297933902471L;

    private final int targetActions;
    private final int dropAction;
    private final int gestureModifiers;

    /**
     * With no position.
     *
     * @throws IllegalArgumentException if the context is `null`
     */
    public DragSourceDragEvent(DragSourceContext dsc, int dropAction, int action, int modifiers) {
        super(dsc);
        this.targetActions = action;
        this.dropAction = dropAction;
        this.gestureModifiers = modifiers;
    }

    /**
     * With the position on the screen.
     *
     * @throws IllegalArgumentException if the context is `null`
     */
    public DragSourceDragEvent(DragSourceContext dsc, int dropAction, int action, int modifiers,
            int x, int y) {
        super(dsc, x, y);
        this.targetActions = action;
        this.dropAction = dropAction;
        this.gestureModifiers = modifiers;
    }

    /** What the destination accepts. */
    public int getTargetActions() {
        return this.targetActions;
    }

    /**
     * The modifiers in the old encoding.
     *
     * @deprecated it mixes keys with buttons in an ambiguous way. Use {@link
     *     #getGestureModifiersEx}.
     */
    @Deprecated
    public int getGestureModifiers() {
        return this.gestureModifiers;
    }

    /** The modifiers in the new encoding. */
    public int getGestureModifiersEx() {
        return this.gestureModifiers;
    }

    /**
     * Which action the user asks for, according to the keys.
     *
     * <p>It is the one the drag source passed in, worked out from the modifiers, and **not** from
     * the actions of the destination: it is what the user wants, even if it cannot be done.
     */
    public int getUserAction() {
        return this.dropAction;
    }

    /**
     * What is really going to happen: what the user asks for **and** the destination accepts.
     *
     * <p>Zero if they do not coincide, which is when the cursor shows that nothing can be dropped
     * there.
     */
    public int getDropAction() {
        return this.dropAction & this.targetActions;
    }
}
