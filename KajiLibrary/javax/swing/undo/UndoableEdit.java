package javax.swing.undo;

/**
 * Something that was done and can be undone.
 *
 * <h2>The life cycle, which is what has to be understood</h2>
 *
 * <p>An edit is born already <em>done</em>. From there it alternates between undone and redone,
 * and at any moment it can <strong>die</strong> with {@link #die}: that takes it out of play for
 * good and releases whatever it was holding. The dead state is not the same as undone -- from one
 * there is a way back, from the other there is not.
 *
 * <p>Hence there are four methods where two would seem to be enough: {@link #canUndo} and
 * {@link #canRedo} are not each other's negation, because a dead edit answers {@code false} to
 * both.
 *
 * <h2>Merging: {@link #addEdit} and {@link #replaceEdit}</h2>
 *
 * <p>Without merging, typing a word would leave one edit per letter and undoing would be letter
 * by letter. The two methods are the two directions of absorbing the neighbour:
 * {@code addEdit} asks "can you swallow this one that is coming?", {@code replaceEdit} asks
 * "can you swallow the one that was already there?". Answering {@code false} to both is always
 * valid and gives the behaviour without merging.
 */
public interface UndoableEdit {

    /** Reverts what this edit did. */
    void undo() throws CannotUndoException;

    /** Whether it can be undone now. */
    boolean canUndo();

    /** Applies again what this edit did. */
    void redo() throws CannotRedoException;

    /** Whether it can be redone now. */
    boolean canRedo();

    /** Takes it out of play for good and releases what it was holding. */
    void die();

    /** Tries to absorb {@code anEdit}, which comes after this one. */
    boolean addEdit(UndoableEdit anEdit);

    /** Tries to absorb {@code anEdit}, which was before this one. */
    boolean replaceEdit(UndoableEdit anEdit);

    /**
     * Whether it is worth showing as a step of its own.
     *
     * <p>An insignificant edit is undone together with the next significant one instead of
     * consuming a step of the user's. It is how moving the cursor is kept from counting as an
     * action.
     */
    boolean isSignificant();

    /** The name to show to a person. */
    String getPresentationName();

    /** The name for the undo command, typically "Undo" plus the previous one. */
    String getUndoPresentationName();

    /** The name for the redo command. */
    String getRedoPresentationName();
}
