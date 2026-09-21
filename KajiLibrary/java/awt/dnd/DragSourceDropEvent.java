package java.awt.dnd;

/**
 * The drag finished, seen from the source.
 *
 * <p>It is the only event that **always** arrives, whether it was dropped or cancelled, and it
 * brings the only information the source really needs: whether the destination kept the data and
 * with which action.
 *
 * <p>Out of that comes the decision to delete the original: it has to be deleted only if
 * {@link #getDropSuccess} is true **and** {@link #getDropAction} is move. A successful drop with
 * copy deletes nothing, and a cancelled drag neither.
 */
public class DragSourceDropEvent extends DragSourceEvent {

    private static final long serialVersionUID = -5571321229470821891L;

    private final boolean dropSuccess;
    private final int dropAction;

    /**
     * A drag that finished **without** being dropped.
     *
     * @throws IllegalArgumentException if the context is `null`
     */
    public DragSourceDropEvent(DragSourceContext dsc) {
        super(dsc);
        this.dropSuccess = false;
        this.dropAction = DnDConstants.ACTION_NONE;
    }

    /**
     * A drag that finished by being dropped, with no position.
     *
     * @throws IllegalArgumentException if the context is `null`
     */
    public DragSourceDropEvent(DragSourceContext dsc, int action, boolean success) {
        super(dsc);
        this.dropSuccess = success;
        this.dropAction = action;
    }

    /**
     * Like the previous one, with the position on the screen.
     *
     * @throws IllegalArgumentException if the context is `null`
     */
    public DragSourceDropEvent(DragSourceContext dsc, int action, boolean success, int x, int y) {
        super(dsc, x, y);
        this.dropSuccess = success;
        this.dropAction = action;
    }

    /** Whether the destination kept the data. */
    public boolean getDropSuccess() {
        return this.dropSuccess;
    }

    /** With which action it took them. */
    public int getDropAction() {
        return this.dropAction;
    }
}
