package javax.swing.undo;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

/**
 * The undo and redo stack.
 *
 * <h2>A {@link CompoundEdit} that is read differently</h2>
 *
 * <p>It inherits the list of edits but not its meaning: a {@code CompoundEdit} undoes
 * <em>all</em> its parts in one go, and this one undoes <strong>one at a time</strong>. What
 * achieves that is a single field, {@link #indexOfNextAdd}: the cursor that splits the list into
 * what is done and what is undone. Everything else in the class is moving that cursor.
 *
 * <p>And that is why {@code UndoManager} stays "in progress" for ever: it never closes, because
 * another edit can always arrive.
 *
 * <h2>Adding throws away what was redoable, and it has to</h2>
 *
 * <p>If the user undid three steps and then does something new, those three stop making sense:
 * redoing them would apply changes over a state that is no longer the one they had in front of
 * them. {@link #addEdit} kills them explicitly instead of leaving them hanging.
 *
 * <h2>The limit</h2>
 *
 * <p>{@link #setLimit} bounds how many edits are remembered, because a stack with no limit grows
 * with the whole session. When pruning, <strong>the ones at the ends</strong> are removed,
 * keeping those next to the cursor: they are the ones the user is closest to undoing or redoing.
 */
public class UndoManager extends CompoundEdit implements UndoableEditListener {

    private static final long serialVersionUID = -2077529998244066750L;

    /** The cursor: how many edits in the list are done. */
    int indexOfNextAdd;

    /** How many edits are remembered at most. */
    int limit;

    /** A new stack, with room for a hundred edits. */
    public UndoManager() {
        super();
        this.indexOfNextAdd = 0;
        this.limit = 100;
    }

    /** The current limit. */
    public synchronized int getLimit() {
        return this.limit;
    }

    /** Throws away everything it remembered and goes back to the initial state. */
    public synchronized void discardAllEdits() {
        for (int i = 0; i < this.edits.size(); i++) {
            this.edits.elementAt(i).die();
        }
        this.edits.removeAllElements();
        this.indexOfNextAdd = 0;
    }

    /** Prunes until the limit is met, removing from the ends. */
    protected void trimForLimit() {
        if (this.limit < 0) {
            return;
        }
        int size = this.edits.size();
        if (size <= this.limit) {
            return;
        }
        // The cursor is the centre of interest: the `limit` edits closest to it are kept, spread
        // over both sides. Pruning from one end only would leave the user with nothing to redo
        // right after undoing a lot.
        int half = this.limit / 2;
        int from = this.indexOfNextAdd - half;
        int to = this.indexOfNextAdd + (this.limit - half) - 1;
        if (from < 0) {
            to = to - from;
            from = 0;
        }
        if (to >= size) {
            from = from - (to - size + 1);
            to = size - 1;
            if (from < 0) {
                from = 0;
            }
        }
        trimEdits(to + 1, size - 1);
        trimEdits(0, from - 1);
    }

    /**
     * Kills and removes the edits in the range, inclusive.
     *
     * <p>From the back to the front, because removing shifts the indices: doing it the other way
     * round would skip elements, which is the classic mistake of deleting while walking.
     */
    protected void trimEdits(int from, int to) {
        if (from > to) {
            return;
        }
        for (int i = to; i >= from; i--) {
            this.edits.elementAt(i).die();
            this.edits.removeElementAt(i);
        }
        if (this.indexOfNextAdd > to) {
            this.indexOfNextAdd = this.indexOfNextAdd - (to - from + 1);
        } else if (this.indexOfNextAdd >= from) {
            this.indexOfNextAdd = from;
        }
    }

    /** Changes the limit and prunes right away if needed. */
    public synchronized void setLimit(int l) {
        if (!isInProgress()) {
            throw new RuntimeException("The stack is already closed");
        }
        this.limit = l;
        trimForLimit();
    }

    /** The next edit that will be undone, or {@code null}. */
    protected UndoableEdit editToBeUndone() {
        int i = this.indexOfNextAdd;
        while (i > 0) {
            i = i - 1;
            UndoableEdit e = this.edits.elementAt(i);
            if (e.isSignificant()) {
                return e;
            }
        }
        return null;
    }

    /** The next edit that will be redone, or {@code null}. */
    protected UndoableEdit editToBeRedone() {
        int count = this.edits.size();
        int i = this.indexOfNextAdd;
        while (i < count) {
            UndoableEdit e = this.edits.elementAt(i);
            if (e.isSignificant()) {
                return e;
            }
            i = i + 1;
        }
        return null;
    }

    /** Undoes backwards until past {@code edit}, inclusive. */
    protected void undoTo(UndoableEdit edit) throws CannotUndoException {
        boolean keepGoing = true;
        while (keepGoing) {
            this.indexOfNextAdd = this.indexOfNextAdd - 1;
            UndoableEdit next = this.edits.elementAt(this.indexOfNextAdd);
            next.undo();
            keepGoing = next != edit;
        }
    }

    /** Redoes forwards until past {@code edit}, inclusive. */
    protected void redoTo(UndoableEdit edit) throws CannotRedoException {
        boolean keepGoing = true;
        while (keepGoing) {
            UndoableEdit next = this.edits.elementAt(this.indexOfNextAdd);
            this.indexOfNextAdd = this.indexOfNextAdd + 1;
            next.redo();
            keepGoing = next != edit;
        }
    }

    /**
     * Undoes or redoes, whichever applies.
     *
     * <p>For a single button. Which of the two depends on where the cursor is, not on a separate
     * state.
     */
    public void undoOrRedo() throws CannotRedoException, CannotUndoException {
        if (this.indexOfNextAdd == this.edits.size()) {
            undo();
        } else {
            redo();
        }
    }

    /** Whether either of the two can be done. */
    public synchronized boolean canUndoOrRedo() {
        if (this.indexOfNextAdd == this.edits.size()) {
            return canUndo();
        }
        return canRedo();
    }

    /** Undoes the last significant edit. */
    public synchronized void undo() throws CannotUndoException {
        if (isInProgress()) {
            UndoableEdit e = editToBeUndone();
            if (e == null) {
                throw new CannotUndoException();
            }
            undoTo(e);
            return;
        }
        super.undo();
    }

    /** Whether there is anything to undo. */
    public synchronized boolean canUndo() {
        if (isInProgress()) {
            UndoableEdit e = editToBeUndone();
            return e != null && e.canUndo();
        }
        return super.canUndo();
    }

    /** Redoes the next significant edit. */
    public synchronized void redo() throws CannotRedoException {
        if (isInProgress()) {
            UndoableEdit e = editToBeRedone();
            if (e == null) {
                throw new CannotRedoException();
            }
            redoTo(e);
            return;
        }
        super.redo();
    }

    /** Whether there is anything to redo. */
    public synchronized boolean canRedo() {
        if (isInProgress()) {
            UndoableEdit e = editToBeRedone();
            return e != null && e.canRedo();
        }
        return super.canRedo();
    }

    /**
     * Adds an edit, throwing away what was left to redo.
     *
     * <p>See the class note: redoing after a new edit would apply changes over a state that is no
     * longer the one they had in front of them.
     */
    public synchronized boolean addEdit(UndoableEdit anEdit) {
        boolean retVal;
        trimEdits(this.indexOfNextAdd, this.edits.size() - 1);
        retVal = super.addEdit(anEdit);
        if (isInProgress()) {
            retVal = true;
        }
        this.indexOfNextAdd = this.edits.size();
        trimForLimit();
        return retVal;
    }

    /** Closes the stack: from there on it behaves like an ordinary {@link CompoundEdit}. */
    public synchronized void end() {
        super.end();
        trimEdits(this.indexOfNextAdd, this.edits.size() - 1);
    }

    /** The command's name for a single undo-or-redo button. */
    public synchronized String getUndoOrRedoPresentationName() {
        if (this.indexOfNextAdd == this.edits.size()) {
            return getUndoPresentationName();
        }
        return getRedoPresentationName();
    }

    /** The undo command's name. */
    public synchronized String getUndoPresentationName() {
        if (!isInProgress()) {
            return super.getUndoPresentationName();
        }
        if (canUndo()) {
            return editToBeUndone().getUndoPresentationName();
        }
        return UndoName;
    }

    /** The redo command's name. */
    public synchronized String getRedoPresentationName() {
        if (!isInProgress()) {
            return super.getRedoPresentationName();
        }
        if (canRedo()) {
            return editToBeRedone().getRedoPresentationName();
        }
        return RedoName;
    }

    /**
     * Takes an edit from whoever produced it and adds it.
     *
     * <p>Implementing {@link UndoableEditListener} is what allows connecting the stack to a
     * document in one line, without the document knowing that a stack exists.
     */
    public void undoableEditHappened(UndoableEditEvent e) {
        addEdit(e.getEdit());
    }

    public String toString() {
        return super.toString()
                + " limit: " + String.valueOf(this.limit)
                + " indexOfNextAdd: " + String.valueOf(this.indexOfNextAdd);
    }
}
