package javax.accessibility;

import java.util.Vector;

/**
 * The states an object has **at this moment**.
 *
 * <p>It exists because the states have to be read all together: asking one at a time would show an
 * object halfway through changing, with the focus already set and the selection not yet.
 *
 * <p>It is a set: adding the same state twice does not duplicate it, and {@link #add} returns
 * whether anything changed.
 */
public class AccessibleStateSet {

    /** The states. It is a {@code Vector} because of the API's inheritance, not by choice. */
    protected Vector<AccessibleState> states = null;

    /** An empty set. */
    public AccessibleStateSet() {
        this.states = null;
    }

    /**
     * With those states.
     *
     * @throws NullPointerException if the array is `null`
     */
    public AccessibleStateSet(AccessibleState[] states) {
        if (states.length != 0) {
            this.states = new Vector<AccessibleState>(states.length);
            for (int i = 0; i < states.length; i++) {
                if (!this.states.contains(states[i])) {
                    this.states.addElement(states[i]);
                }
            }
        }
    }

    /**
     * Adds a state.
     *
     * @return `true` if it was not there
     */
    public boolean add(AccessibleState state) {
        if (this.states == null) {
            this.states = new Vector<AccessibleState>();
        }
        if (!this.states.contains(state)) {
            this.states.addElement(state);
            return true;
        }
        return false;
    }

    /** Adds several states. */
    public void addAll(AccessibleState[] states) {
        if (states.length != 0) {
            if (this.states == null) {
                this.states = new Vector<AccessibleState>(states.length);
            }
            for (int i = 0; i < states.length; i++) {
                if (!this.states.contains(states[i])) {
                    this.states.addElement(states[i]);
                }
            }
        }
    }

    /**
     * Removes a state.
     *
     * @return `true` if it was there
     */
    public boolean remove(AccessibleState state) {
        if (this.states == null) {
            return false;
        }
        return this.states.removeElement(state);
    }

    /** Leaves the set empty. */
    public void clear() {
        if (this.states != null) {
            this.states.removeAllElements();
        }
    }

    /** Whether the object has that state. */
    public boolean contains(AccessibleState state) {
        if (this.states == null) {
            return false;
        }
        return this.states.contains(state);
    }

    /** The states, as an array. */
    public AccessibleState[] toArray() {
        if (this.states == null) {
            return new AccessibleState[0];
        }
        AccessibleState[] out = new AccessibleState[this.states.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = this.states.elementAt(i);
        }
        return out;
    }

    public String toString() {
        if (this.states == null || this.states.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.states.elementAt(0).toString());
        for (int i = 1; i < this.states.size(); i++) {
            sb.append(",").append(this.states.elementAt(i).toString());
        }
        return sb.toString();
    }
}
