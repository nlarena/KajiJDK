package javax.swing.undo;

import java.util.Vector;

/**
 * Several edits that are undone and redone as a single one.
 *
 * <h2>The two moments: in progress and closed</h2>
 *
 * <p>It is the state that governs everything else. While it is <em>in progress</em> it accepts
 * edits and cannot be undone; after {@link #end} it stops accepting and only then behaves like a
 * normal edit. That asymmetry avoids the senseless case of undoing a group that is still being
 * assembled.
 *
 * <h2>The order matters</h2>
 *
 * <p>Undo walks the list <strong>backwards</strong> and redo forwards. It is obvious said out
 * loud and easy to write wrong: undoing in order of application would revert the effects in the
 * wrong sequence every time two edits touch the same thing.
 *
 * <p>A group is significant if <em>any</em> of its parts is, and its name is the last one's,
 * which is the one the user has just made.
 */
public class CompoundEdit extends AbstractUndoableEdit {

    private static final long serialVersionUID = -6512679417930021399L;

    /** Whether it still accepts edits. */
    boolean inProgress;

    /** The edits it groups, in order of application. */
    protected Vector<UndoableEdit> edits;

    /** A new group, in progress and empty. */
    public CompoundEdit() {
        super();
        this.inProgress = true;
        this.edits = new Vector<UndoableEdit>();
    }

    /** Undoes them all, from the last to the first. */
    public void undo() throws CannotUndoException {
        super.undo();
        int i = this.edits.size();
        while (i > 0) {
            i = i - 1;
            UndoableEdit e = this.edits.elementAt(i);
            e.undo();
        }
    }

    /** Redoes them all, from the first to the last. */
    public void redo() throws CannotRedoException {
        super.redo();
        for (int i = 0; i < this.edits.size(); i++) {
            this.edits.elementAt(i).redo();
        }
    }

    /** The last edit added, or {@code null} if there is none. */
    protected UndoableEdit lastEdit() {
        int n = this.edits.size();
        if (n > 0) {
            return this.edits.elementAt(n - 1);
        }
        return null;
    }

    /** Kills them all, from the last to the first, and then itself. */
    public void die() {
        int i = this.edits.size();
        while (i > 0) {
            i = i - 1;
            this.edits.elementAt(i).die();
        }
        super.die();
    }

    /**
     * Adds an edit to the group, if it is still in progress.
     *
     * <p>Before queueing it, it tries to <strong>merge</strong> it with the last one: first it asks
     * the last one whether it can absorb the new one, and if not, it asks the new one whether it
     * can absorb the last one. Only if both say no does the list grow.
     */
    public boolean addEdit(UndoableEdit anEdit) {
        if (!this.inProgress) {
            return false;
        }
        UndoableEdit last = lastEdit();
        if (last == null) {
            this.edits.addElement(anEdit);
            return true;
        }
        if (!last.addEdit(anEdit)) {
            if (anEdit.replaceEdit(last)) {
                this.edits.removeElementAt(this.edits.size() - 1);
            }
            this.edits.addElement(anEdit);
        }
        return true;
    }

    /** Closes the group: it stops accepting edits and starts being able to be undone. */
    public void end() {
        this.inProgress = false;
    }

    /** It can be undone if it is closed and alive. */
    public boolean canUndo() {
        return !isInProgress() && super.canUndo();
    }

    /** It can be redone if it is closed and undone. */
    public boolean canRedo() {
        return !isInProgress() && super.canRedo();
    }

    /** Whether it still accepts edits. */
    public boolean isInProgress() {
        return this.inProgress;
    }

    /** Significant if any of its parts is. */
    public boolean isSignificant() {
        for (int i = 0; i < this.edits.size(); i++) {
            if (this.edits.elementAt(i).isSignificant()) {
                return true;
            }
        }
        return false;
    }

    /** The last edit's name: the one the user has just made. */
    public String getPresentationName() {
        UndoableEdit last = lastEdit();
        if (last != null) {
            return last.getPresentationName();
        }
        return super.getPresentationName();
    }

    public String getUndoPresentationName() {
        UndoableEdit last = lastEdit();
        if (last != null) {
            return last.getUndoPresentationName();
        }
        return super.getUndoPresentationName();
    }

    public String getRedoPresentationName() {
        UndoableEdit last = lastEdit();
        if (last != null) {
            return last.getRedoPresentationName();
        }
        return super.getRedoPresentationName();
    }

    public String toString() {
        return super.toString()
                + " inProgress: " + String.valueOf(this.inProgress)
                + " edits: " + String.valueOf(this.edits);
    }
}
