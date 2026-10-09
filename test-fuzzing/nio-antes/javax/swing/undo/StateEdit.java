package javax.swing.undo;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * An edit that undoes by <strong>comparing snapshots of the state</strong>, not by reverting the
 * action.
 *
 * <h2>How it is used, and why in two steps</h2>
 *
 * <p>The constructor takes the before snapshot; {@link #end} takes the after one. The real change
 * goes between the two calls, and this class never sees it -- hence it serves for operations
 * whose inverse nobody wants to write.
 *
 * <p>Undoing is putting the old snapshot back and redoing is putting the new one. Both operations
 * are the same call with a different table, which is what makes this class so short.
 *
 * <h2>{@link #removeRedundantState}, which is what makes it practical</h2>
 *
 * <p>Without that pruning, each edit would keep the object's <em>whole</em> state twice, even if a
 * single field had changed. The method removes from both tables the keys whose value did not
 * change, so what is left is the difference. It is the reason the snapshot approach is not
 * immediately unviable in memory.
 */
public class StateEdit extends AbstractUndoableEdit {

    private static final long serialVersionUID = 5297308062724130866L;

    /** The JDK's version identifier; it is kept for surface fidelity. */
    protected static final String RCSID = "$Id: StateEdit.java,v 1.6 1997/10/01 20:05:51 sandipc Exp $";

    /** The object whose state is snapshotted. */
    protected StateEditable object;

    /** The snapshot from before the change. */
    protected Hashtable<Object, Object> preState;

    /** The snapshot from after, which {@link #end} fills in. */
    protected Hashtable<Object, Object> postState;

    /** The name to show. */
    protected String undoRedoName;

    /** Takes the before snapshot, with no name. */
    public StateEdit(StateEditable anObject) {
        super();
        init(anObject, null);
    }

    /** Takes the before snapshot, with a name to show. */
    public StateEdit(StateEditable anObject, String name) {
        super();
        init(anObject, name);
    }

    /**
     * Keeps the object and asks it for the before snapshot.
     *
     * <p>{@code protected} and separate from the constructor because both constructors do the same
     * thing: it is the single place where a subclass can step in.
     */
    protected void init(StateEditable anObject, String name) {
        this.object = anObject;
        this.preState = new Hashtable<Object, Object>(11);
        this.object.storeState(this.preState);
        this.postState = null;
        this.undoRedoName = name;
    }

    /** Takes the after snapshot and prunes what did not change. */
    public void end() {
        this.postState = new Hashtable<Object, Object>(11);
        this.object.storeState(this.postState);
        removeRedundantState();
    }

    /** Puts the before snapshot back on the object. */
    public void undo() {
        super.undo();
        this.object.restoreState(this.preState);
    }

    /** Puts the after snapshot on the object. */
    public void redo() {
        super.redo();
        this.object.restoreState(this.postState);
    }

    public String getPresentationName() {
        return this.undoRedoName;
    }

    /**
     * Removes from both snapshots the keys whose value did not change.
     *
     * <p>The old snapshot is walked and compared against the new one; what matches leaves both. A
     * key that is in only one of the two <strong>is</strong> a change --it appeared or
     * disappeared-- and that is why it is left alone.
     */
    protected void removeRedundantState() {
        java.util.Vector<Object> uselessKeys = new java.util.Vector<Object>();
        Enumeration<Object> keys = this.preState.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (this.postState.containsKey(key)) {
                Object before = this.preState.get(key);
                Object after = this.postState.get(key);
                if (before.equals(after)) {
                    uselessKeys.addElement(key);
                }
            }
        }
        for (int i = 0; i < uselessKeys.size(); i++) {
            Object key = uselessKeys.elementAt(i);
            this.preState.remove(key);
            this.postState.remove(key);
        }
    }
}
