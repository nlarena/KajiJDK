package javax.swing.undo;

/**
 * The base of almost every edit: it keeps track of whether it is done and whether it is still
 * alive.
 *
 * <h2>Two flags, four states</h2>
 *
 * <p>{@code alive} and {@code hasBeenDone} look redundant and are not: a dead edit answers
 * {@code false} to {@link #canUndo} and to {@link #canRedo} at the same time, which a single flag
 * cannot express. Dead is different from undone -- from the second one there is a way back.
 *
 * <p>The whole class is that state machine. What the concrete edit <em>does</em> is put there by
 * the subclass overriding {@link #undo} and {@link #redo}, always calling {@code super} first so
 * that the check runs before the work.
 *
 * <h2>The three names</h2>
 *
 * <p>{@link #getPresentationName} is the edit's; the other two are the menu's text, and come from
 * sticking "Undo" or "Redo" in front. They are separate because in a menu the complete name is
 * what is shown, and an edit with no name still needs the item to say something.
 */
public class AbstractUndoableEdit implements UndoableEdit, java.io.Serializable {

    private static final long serialVersionUID = 580150227676302096L;

    /** The undo command's prefix. */
    protected static final String UndoName = "Undo";

    /** The redo command's prefix. */
    protected static final String RedoName = "Redo";

    /** Whether the edit is applied. It starts at {@code true}: an edit is born done. */
    boolean hasBeenDone;

    /** Whether it can still be used. {@link #die} turns it off and it never comes back on. */
    boolean alive;

    /** A new edit: alive and already done. */
    public AbstractUndoableEdit() {
        super();
        this.hasBeenDone = true;
        this.alive = true;
    }

    /**
     * Takes it out of play for good.
     *
     * <p>It undoes nothing: killing and undoing are different things. Whoever wants both has to ask
     * for them in order.
     */
    public void die() {
        this.alive = false;
    }

    /**
     * @throws CannotUndoException if {@link #canUndo} is {@code false}
     */
    public void undo() throws CannotUndoException {
        if (!canUndo()) {
            throw new CannotUndoException();
        }
        this.hasBeenDone = false;
    }

    /** It can be undone if it is alive and done. */
    public boolean canUndo() {
        return this.alive && this.hasBeenDone;
    }

    /**
     * @throws CannotRedoException if {@link #canRedo} is {@code false}
     */
    public void redo() throws CannotRedoException {
        if (!canRedo()) {
            throw new CannotRedoException();
        }
        this.hasBeenDone = true;
    }

    /** It can be redone if it is alive and undone. */
    public boolean canRedo() {
        return this.alive && !this.hasBeenDone;
    }

    /** It absorbs nothing: merging is decided by the subclasses that know how. */
    public boolean addEdit(UndoableEdit anEdit) {
        return false;
    }

    /** It replaces nothing. */
    public boolean replaceEdit(UndoableEdit anEdit) {
        return false;
    }

    /** Significant by default, which is the safe thing: it shows up as a step of its own. */
    public boolean isSignificant() {
        return true;
    }

    /** The empty string: a generic edit has no name to show. */
    public String getPresentationName() {
        return "";
    }

    /** The undo prefix followed by the name, if there is one. */
    public String getUndoPresentationName() {
        String name = getPresentationName();
        if (name.isEmpty()) {
            return UndoName;
        }
        return UndoName + " " + name;
    }

    /** The redo prefix followed by the name, if there is one. */
    public String getRedoPresentationName() {
        String name = getPresentationName();
        if (name.isEmpty()) {
            return RedoName;
        }
        return RedoName + " " + name;
    }

    public String toString() {
        return super.toString()
                + " hasBeenDone: " + String.valueOf(this.hasBeenDone)
                + " alive: " + String.valueOf(this.alive);
    }
}
