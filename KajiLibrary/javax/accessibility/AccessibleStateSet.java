package javax.accessibility;

import java.util.Vector;

/**
 * Los estados que tiene un objeto **en este momento**.
 *
 * <p>Existe porque los estados hay que leerlos todos juntos: preguntar de a uno dejaría ver un
 * objeto a medio cambiar, con el foco ya puesto y la selección todavía no.
 *
 * <p>Es un conjunto: agregar dos veces el mismo estado no lo duplica, y {@link #add} devuelve si
 * cambió algo.
 */
public class AccessibleStateSet {

    /** Los estados. Es un {@code Vector} por herencia de la API, no por elección. */
    protected Vector<AccessibleState> states = null;

    /** Un conjunto vacío. */
    public AccessibleStateSet() {
        this.states = null;
    }

    /**
     * Con esos estados.
     *
     * @throws NullPointerException si el arreglo es `null`
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
     * Agrega un estado.
     *
     * @return `true` si no estaba
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

    /** Agrega varios estados. */
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
     * Saca un estado.
     *
     * @return `true` si estaba
     */
    public boolean remove(AccessibleState state) {
        if (this.states == null) {
            return false;
        }
        return this.states.removeElement(state);
    }

    /** Deja el conjunto vacío. */
    public void clear() {
        if (this.states != null) {
            this.states.removeAllElements();
        }
    }

    /** Si el objeto tiene ese estado. */
    public boolean contains(AccessibleState state) {
        if (this.states == null) {
            return false;
        }
        return this.states.contains(state);
    }

    /** Los estados, como arreglo. */
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
