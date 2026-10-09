package java.awt.dnd;

/**
 * What can be done with what is dragged: copy it, move it or link it.
 *
 * <p>They are **bits**, not exclusive values, and that is the point of them: the source declares
 * everything it accepts —usually copy or move— and the destination chooses one. The key the user
 * has held down tilts the choice, and that is why dragging with Control copies where dragging with
 * nothing would move.
 *
 * <p>The class cannot be instantiated and is final: they are constants and nothing else.
 */
public final class DnDConstants {

    /** Nothing; the drag is not accepted. */
    public static final int ACTION_NONE = 0x0;

    /** Copy: the original stays where it was. */
    public static final int ACTION_COPY = 0x1;

    /** Move: the original is taken out of the source. */
    public static final int ACTION_MOVE = 0x2;

    /** Either of the two; the destination chooses. */
    public static final int ACTION_COPY_OR_MOVE = ACTION_COPY | ACTION_MOVE;

    /** Link: a reference to the original is created. */
    public static final int ACTION_LINK = 0x40000000;

    /** The other name of {@link #ACTION_LINK}. */
    public static final int ACTION_REFERENCE = ACTION_LINK;

    /** It is not instantiated. */
    private DnDConstants() {
    }
}
