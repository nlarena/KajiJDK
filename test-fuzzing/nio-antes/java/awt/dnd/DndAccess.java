package java.awt.dnd;

/**
 * Bridge for returning a {@link DropTargetContext} to its initial state from another package.
 *
 * <p>Not a JDK class: it is scaffolding of ours, the same kind as
 * {@code java.nio.channels.MapModes}. {@code DropTargetContext.reset()} is package-private on
 * purpose -- nobody outside should be able to wipe the state of a drag in progress -- but the
 * bridge to other toolkits, {@code jdk.swing.interop}, has to be able to do it between one drag and
 * the next. In the JDK that permission comes from {@code AWTAccessor}.
 */
public final class DndAccess {

    private DndAccess() {
    }

    /**
     * Returns the context to its initial state.
     *
     * @param context the context
     * @throws NullPointerException if the context is {@code null}
     */
    public static void reset(DropTargetContext context) {
        context.reset();
    }
}
