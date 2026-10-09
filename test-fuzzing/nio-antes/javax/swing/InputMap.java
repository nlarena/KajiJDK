package javax.swing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * A table from key to action name, chained with another.
 *
 * <h2>What the chain is for</h2>
 *
 * <p>The same as in {@link ActionMap}: there is a layer of the program's, one of the look and
 * feel's and one of the class's. Chaining them allows a shortcut to be covered without copying
 * the others and the look and feel to be changed without losing the ones the program set.
 *
 * <p>What it keeps is not the action but its <em>name</em>. That indirection is what allows
 * changing an action's shortcut and changing what the action does to be two separate things.
 *
 * <p>{@link #keys} returns only this table's and {@link #allKeys} the whole chain's. The
 * difference matters: for saving the configuration one wants one's own, and for knowing which
 * keys answer one has to look at them all.
 *
 * <h2>Putting null erases</h2>
 *
 * <p>{@code put(key, null)} removes the entry instead of keeping a null, just as in
 * {@link ActionMap}.
 */
public class InputMap implements Serializable {

    private transient HashMap<KeyStroke, Object> arrayTable;
    private InputMap parent;

    /** An empty table, with no parent. */
    public InputMap() {
    }

    /** The table that is consulted when this one does not have the key. */
    public void setParent(InputMap map) {
        this.parent = map;
    }

    public InputMap getParent() {
        return parent;
    }

    /** It keeps that key's action name; with {@code null} it removes it. */
    public void put(KeyStroke key, Object actionMapKey) {
        if (key == null) {
            return;
        }
        if (actionMapKey == null) {
            remove(key);
            return;
        }
        if (arrayTable == null) {
            arrayTable = new HashMap<KeyStroke, Object>();
        }
        arrayTable.put(key, actionMapKey);
    }

    /** That key's action name, looking through the chain. */
    public Object get(KeyStroke key) {
        Object value = (arrayTable == null) ? null : arrayTable.get(key);
        if (value == null) {
            InputMap parent = getParent();
            if (parent != null) {
                return parent.get(key);
            }
        }
        return value;
    }

    public void remove(KeyStroke key) {
        if (arrayTable != null) {
            arrayTable.remove(key);
        }
    }

    /** It empties this table; the parent is not touched. */
    public void clear() {
        if (arrayTable != null) {
            arrayTable.clear();
        }
    }

    /** This table's keys, without the parent's. */
    public KeyStroke[] keys() {
        if (arrayTable == null || arrayTable.isEmpty()) {
            // Empty it returns null, not an array of zero. It is what the JDK does and there is
                        // code that tells "there is no table" from "there is a table with nothing";
                        // here the two give the same.
            return null;
        }
        Set<KeyStroke> ks = arrayTable.keySet();
        KeyStroke[] out = new KeyStroke[ks.size()];
        int i = 0;
        for (KeyStroke k : ks) {
            out[i] = k;
            i++;
        }
        return out;
    }

    public int size() {
        return (arrayTable == null) ? 0 : arrayTable.size();
    }

    /**
     * The keys of the whole chain, without repeating.
     *
     * <p>It returns null if there is none, not an empty array. It is what the JDK does and
     * there is code that tells the two cases apart.
     */
    public KeyStroke[] allKeys() {
        int count = size();
        InputMap parent = getParent();
        if (parent == null) {
            return keys();
        }
        KeyStroke[] pk = parent.allKeys();
        KeyStroke[] mk = keys();
        if (pk == null) {
            return mk;
        }
        if (mk == null) {
            return pk;
        }
        HashMap<KeyStroke, KeyStroke> merge = new HashMap<KeyStroke, KeyStroke>();
        for (int i = 0; i < pk.length; i++) {
            merge.put(pk[i], pk[i]);
        }
        for (int i = 0; i < mk.length; i++) {
            merge.put(mk[i], mk[i]);
        }
        KeyStroke[] out = new KeyStroke[merge.size()];
        int i = 0;
        for (KeyStroke k : merge.keySet()) {
            out[i] = k;
            i++;
        }
        return out;
    }
}
