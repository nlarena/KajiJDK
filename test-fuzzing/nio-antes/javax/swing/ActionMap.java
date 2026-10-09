package javax.swing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * A table from name to action, chained with another.
 *
 * <h2>What the chain is for</h2>
 *
 * <p>A component has three layers of actions: the ones the program set, the ones the look and
 * feel set, and the ones it inherits from its class. Chaining three tables allows the program to
 * cover a single action of the look and feel without copying the others, and changing the look
 * and feel to replace its layer without touching what the program set.
 *
 * <p>{@link #keys} returns only this table's and {@link #allKeys} the whole chain's. The
 * difference matters: for saving the configuration one wants one's own, and for knowing which
 * keys answer one has to look at them all.
 *
 * <h2>Putting null erases</h2>
 *
 * <p>{@code put(key, null)} removes the entry instead of keeping a null. It is what the JDK
 * does, and the useful consequence is that a parent's action cannot be covered with "no
 * action": for that it has to be removed from the whole chain.
 */
public class ActionMap implements Serializable {

    private transient HashMap<Object, Action> arrayTable;
    private ActionMap parent;

    /** An empty table, with no parent. */
    public ActionMap() {
    }

    /** The table that is consulted when this one does not have the key. */
    public void setParent(ActionMap map) {
        this.parent = map;
    }

    public ActionMap getParent() {
        return parent;
    }

    /** It keeps an action; with {@code null} it removes it. See the class note. */
    public void put(Object key, Action action) {
        if (key == null) {
            return;
        }
        if (action == null) {
            remove(key);
            return;
        }
        if (arrayTable == null) {
            arrayTable = new HashMap<Object, Action>();
        }
        arrayTable.put(key, action);
    }

    /** That key's action, looking through the chain. */
    public Action get(Object key) {
        Action value = (arrayTable == null) ? null : arrayTable.get(key);
        if (value == null) {
            ActionMap parent = getParent();
            if (parent != null) {
                return parent.get(key);
            }
        }
        return value;
    }

    public void remove(Object key) {
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
    public Object[] keys() {
        if (arrayTable == null || arrayTable.isEmpty()) {
            // Empty it returns null, not an array of zero. It is what the JDK does and there is
                        // code that tells "there is no table" from "there is a table with nothing";
                        // here the two give the same.
            return null;
        }
        Set<Object> ks = arrayTable.keySet();
        Object[] out = new Object[ks.size()];
        int i = 0;
        for (Object k : ks) {
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
    public Object[] allKeys() {
        int count = size();
        ActionMap parent = getParent();
        if (parent == null) {
            return keys();
        }
        Object[] pk = parent.allKeys();
        Object[] mk = keys();
        if (pk == null) {
            return mk;
        }
        if (mk == null) {
            return pk;
        }
        HashMap<Object, Object> merge = new HashMap<Object, Object>();
        for (int i = 0; i < pk.length; i++) {
            merge.put(pk[i], pk[i]);
        }
        for (int i = 0; i < mk.length; i++) {
            merge.put(mk[i], mk[i]);
        }
        Object[] out = new Object[merge.size()];
        int i = 0;
        for (Object k : merge.keySet()) {
            out[i] = k;
            i++;
        }
        return out;
    }
}
