package javax.swing.undo;

import java.util.Vector;

import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;

/**
 * The emitting side: it gathers listeners and hands the edits out to them.
 *
 * <h2>What it adds over a list of listeners</h2>
 *
 * <p><strong>Grouping</strong>. Between {@link #beginUpdate} and {@link #endUpdate} the edits are
 * not handed out: they pile up in a {@link CompoundEdit} and on closing <em>one single</em> edit
 * comes out. It is how a composite operation --a replacement that deletes and inserts-- is undone
 * in one go instead of in two steps the user never thought of as separate.
 *
 * <p>{@link #getUpdateLevel} counts nesting, so the groupings can be nested: only the outermost
 * {@code endUpdate} triggers the sending.
 *
 * <h2>Why {@link #realSource} exists</h2>
 *
 * <p>An object that wants to emit these events rarely inherits from this class: it has it inside
 * as a field. Without {@code realSource}, the event would say the edit came from the helper and
 * not from the document, which is what matters to the listener.
 */
public class UndoableEditSupport {

    /** How many {@link #beginUpdate}s are open. Zero means it is handed out right away. */
    protected int updateLevel;

    /** Where the edits pile up while a grouping is open. */
    protected CompoundEdit compoundEdit;

    /** The listeners. */
    protected Vector<UndoableEditListener> listeners;

    /** Who appears as the events' source; see the class note. */
    protected Object realSource;

    /** With this same object as the source. */
    public UndoableEditSupport() {
        this(null);
    }

    /** With {@code r} as the events' source. */
    public UndoableEditSupport(Object r) {
        this.realSource = r == null ? this : r;
        this.updateLevel = 0;
        this.compoundEdit = null;
        this.listeners = new Vector<UndoableEditListener>();
    }

    /** Adds a listener. */
    public synchronized void addUndoableEditListener(UndoableEditListener l) {
        this.listeners.addElement(l);
    }

    /** Removes a listener. */
    public synchronized void removeUndoableEditListener(UndoableEditListener l) {
        this.listeners.removeElement(l);
    }

    /** The listeners, in a new array. */
    public synchronized UndoableEditListener[] getUndoableEditListeners() {
        int n = this.listeners.size();
        UndoableEditListener[] copy = new UndoableEditListener[n];
        for (int i = 0; i < n; i++) {
            copy[i] = this.listeners.elementAt(i);
        }
        return copy;
    }

    /**
     * Hands {@code e} out to all the listeners, now.
     *
     * <p>Unsynchronized and separate from {@link #postEdit} on purpose: handing out calls foreign
     * code, and doing that with the lock held is a recipe for deadlock.
     */
    protected void _postEdit(UndoableEdit e) {
        UndoableEditEvent ev = new UndoableEditEvent(this.realSource, e);
        UndoableEditListener[] copy = getUndoableEditListeners();
        for (int i = 0; i < copy.length; i++) {
            copy[i].undoableEditHappened(ev);
        }
    }

    /**
     * Publishes an edit: it piles it up if a grouping is open, or hands it out if not.
     */
    public synchronized void postEdit(UndoableEdit e) {
        if (this.updateLevel == 0) {
            _postEdit(e);
        } else {
            this.compoundEdit.addEdit(e);
        }
    }

    /** How many groupings are open. */
    public int getUpdateLevel() {
        return this.updateLevel;
    }

    /** Opens a grouping; the edits pile up until the {@link #endUpdate} that closes it. */
    public synchronized void beginUpdate() {
        if (this.updateLevel == 0) {
            this.compoundEdit = createCompoundEdit();
        }
        this.updateLevel = this.updateLevel + 1;
    }

    /**
     * The group the grouping uses.
     *
     * <p>It exists so that a subclass can return a {@link CompoundEdit} of its own --one with a
     * name, for instance-- without rewriting the rest of the mechanism.
     */
    protected CompoundEdit createCompoundEdit() {
        return new CompoundEdit();
    }

    /** Closes a grouping; the outermost one hands the whole group out as a single edit. */
    public synchronized void endUpdate() {
        this.updateLevel = this.updateLevel - 1;
        if (this.updateLevel == 0) {
            this.compoundEdit.end();
            CompoundEdit finished = this.compoundEdit;
            this.compoundEdit = null;
            _postEdit(finished);
        }
    }

    public String toString() {
        return super.toString()
                + " updateLevel: " + String.valueOf(this.updateLevel)
                + " listeners: " + String.valueOf(this.listeners)
                + " compoundEdit: " + String.valueOf(this.compoundEdit);
    }
}
