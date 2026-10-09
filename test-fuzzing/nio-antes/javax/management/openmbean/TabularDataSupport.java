package javax.management.openmbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The implementation of {@link TabularData}, over a {@code HashMap} from key to row.
 *
 * <p>It also implements {@code Map<Object, Object>}, and from there comes this class's only
 * oddity: <b>there are two sets of methods with the same name</b>. {@code get(Object[])} is the
 * table's and {@code get(Object)} the map's; {@code put(CompositeData)} is the table's and
 * {@code put(Object, Object)} the map's. They are not convenience overloads: the map's exist
 * because {@code Map} requires them.
 *
 * <p>The map's behave like the table's where possible, with two differences worth knowing:
 *
 * <ul>
 * <li>{@code put(key, value)} <b>ignores the key</b> and uses the one computed from the value.
 *     Putting a key different from the one the row implies would describe an impossible table, so
 *     it is discarded instead of stored.</li>
 * <li>{@code get(Object)} expects an {@code Object[]}; with anything else it returns null, which is
 *     what a {@code Map} answers for a key it does not have.</li>
 * </ul>
 *
 * <p>The internal key is a {@code List} and not the {@code Object[]}: two arrays with the same
 * content are neither equal nor share a {@code hashCode}, so using them as keys would mean no row
 * was ever found. A list does compare by content.
 */
public class TabularDataSupport
        implements TabularData, Map<Object, Object>, Cloneable, Serializable {

    private static final long serialVersionUID = 5720150593236309827L;

    private final TabularType tabularType;
    private final Map<Object, Object> dataMap;
    // The index names, in order: read once because they are used on every `put`.
    private final transient String[] indexNames;

    /** An empty table of that type. */
    public TabularDataSupport(TabularType tabularType) {
        this(tabularType, 16, 0.75f);
    }

    /**
     * An empty table of that type, with that initial capacity and load factor.
     *
     * @throws IllegalArgumentException if the type is null
     */
    public TabularDataSupport(TabularType tabularType, int initialCapacity, float loadFactor) {
        if (tabularType == null) {
            throw new IllegalArgumentException("the tabular type cannot be null");
        }
        this.tabularType = tabularType;
        this.dataMap = new HashMap<Object, Object>(initialCapacity, loadFactor);
        List<String> names = tabularType.getIndexNames();
        this.indexNames = names.toArray(new String[0]);
    }

    public TabularType getTabularType() {
        return this.tabularType;
    }

    public Object[] calculateIndex(CompositeData value) {
        this.requireRow(value);
        Object[] key = new Object[this.indexNames.length];
        for (int i = 0; i < this.indexNames.length; i++) {
            key[i] = value.get(this.indexNames[i]);
        }
        return key;
    }

    private void requireRow(CompositeData value) {
        if (value == null) {
            throw new NullPointerException("the row cannot be null");
        }
        if (!this.tabularType.getRowType().equals(value.getCompositeType())) {
            throw new InvalidOpenTypeException("the row is not of type "
                    + this.tabularType.getRowType().getTypeName());
        }
    }

    // The real key: a list, which compares by content. See the class note.
    private static List<Object> asKey(Object[] key) {
        List<Object> l = new ArrayList<Object>();
        for (int i = 0; i < key.length; i++) {
            l.add(key[i]);
        }
        // Read-only because `keySet()` exposes them: a key the caller could change would put the
        // map out of sync with its own index.
        return java.util.Collections.unmodifiableList(l);
    }

    // The split between the two exceptions is not symmetric and was checked against the JDK 25: a
    // null OR EMPTY key is `NullPointerException`, and one of the wrong length but not empty is
    // `InvalidKeyException`. It is odd and it is the contract; writing it the other way round makes
    // a client that catches one of the two stop working against the real JDK.
    private void requireKey(Object[] key) {
        if (key == null || key.length == 0) {
            throw new NullPointerException("the key cannot be null or empty");
        }
        if (key.length != this.indexNames.length) {
            throw new InvalidKeyException("the key has " + key.length
                    + " values and the type asks for " + this.indexNames.length);
        }
        for (int i = 0; i < key.length; i++) {
            OpenType<?> t = this.tabularType.getRowType().getType(this.indexNames[i]);
            if (key[i] != null && !t.isValue(key[i])) {
                throw new InvalidKeyException("the value of index " + this.indexNames[i]
                        + " is not of type " + t.getTypeName());
            }
        }
    }

    public boolean containsKey(Object[] key) {
        if (key == null || key.length != this.indexNames.length) {
            return false;
        }
        return this.dataMap.containsKey(asKey(key));
    }

    public boolean containsKey(Object key) {
        if (!(key instanceof Object[])) {
            return false;
        }
        return this.containsKey((Object[]) key);
    }

    public boolean containsValue(CompositeData value) {
        if (value == null) {
            return false;
        }
        return this.dataMap.containsValue(value);
    }

    public boolean containsValue(Object value) {
        return this.dataMap.containsValue(value);
    }

    public CompositeData get(Object[] key) {
        this.requireKey(key);
        return (CompositeData) this.dataMap.get(asKey(key));
    }

    public Object get(Object key) {
        if (!(key instanceof Object[])) {
            return null;
        }
        Object[] k = (Object[]) key;
        if (k.length != this.indexNames.length) {
            return null;
        }
        return this.dataMap.get(asKey(k));
    }

    public void put(CompositeData value) {
        Object[] key = this.calculateIndex(value);
        List<Object> k = asKey(key);
        if (this.dataMap.containsKey(k)) {
            throw new KeyAlreadyExistsException("there is already a row with that key");
        }
        this.dataMap.put(k, value);
    }

    /**
     * Adds that row, <b>ignoring the key</b>. See the class note.
     *
     * @return always null: it cannot replace, so there is never a previous value to return
     */
    public Object put(Object key, Object value) {
        this.put((CompositeData) value);
        return null;
    }

    public CompositeData remove(Object[] key) {
        this.requireKey(key);
        return (CompositeData) this.dataMap.remove(asKey(key));
    }

    public Object remove(Object key) {
        if (!(key instanceof Object[])) {
            return null;
        }
        Object[] k = (Object[]) key;
        if (k.length != this.indexNames.length) {
            return null;
        }
        return this.dataMap.remove(asKey(k));
    }

    /**
     * Adds all those rows, or none.
     *
     * <p>Everything is validated first and only then written. Without that, an array with the last
     * row repeated would leave the earlier ones in and the table half loaded -- which is worse than
     * not having started, because the caller does not know where it stopped.
     */
    public void putAll(CompositeData[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        Map<Object, Object> pending = new HashMap<Object, Object>();
        for (int i = 0; i < values.length; i++) {
            Object[] key = this.calculateIndex(values[i]);
            List<Object> k = asKey(key);
            if (this.dataMap.containsKey(k) || pending.containsKey(k)) {
                throw new KeyAlreadyExistsException("there is already a row with that key");
            }
            pending.put(k, values[i]);
        }
        this.dataMap.putAll(pending);
    }

    /** Adds all the rows of that map. The keys are ignored, as in {@link #put}. */
    public void putAll(Map<?, ?> t) {
        if (t == null || t.isEmpty()) {
            return;
        }
        CompositeData[] rows = new CompositeData[t.size()];
        int i = 0;
        for (Object v : t.values()) {
            rows[i] = (CompositeData) v;
            i = i + 1;
        }
        this.putAll(rows);
    }

    public void clear() {
        this.dataMap.clear();
    }

    public int size() {
        return this.dataMap.size();
    }

    public boolean isEmpty() {
        return this.dataMap.isEmpty();
    }

    public Set<Object> keySet() {
        return this.dataMap.keySet();
    }

    public Collection<Object> values() {
        return this.dataMap.values();
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return this.dataMap.entrySet();
    }

    /**
     * A shallow copy.
     *
     * <p>Shallow is enough: the rows are {@link CompositeData}, which are immutable, so sharing
     * them between the copy and the original does not let one change the other.
     */
    public Object clone() {
        TabularDataSupport copy = new TabularDataSupport(this.tabularType);
        copy.dataMap.putAll(this.dataMap);
        return copy;
    }

    /** Equality by type and rows, against any {@link TabularData}. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TabularData)) {
            return false;
        }
        TabularData other = (TabularData) obj;
        if (!this.tabularType.equals(other.getTabularType())) {
            return false;
        }
        if (this.size() != other.size()) {
            return false;
        }
        for (Object v : this.dataMap.values()) {
            if (!other.containsValue((CompositeData) v)) {
                return false;
            }
        }
        return true;
    }

    /** The sum of the type's hash and the rows', as the contract dictates. */
    public int hashCode() {
        int h = this.tabularType.hashCode();
        for (Object v : this.dataMap.values()) {
            h = h + v.hashCode();
        }
        return h;
    }

    public String toString() {
        return TabularDataSupport.class.getName()
                + "(tabularType=" + this.tabularType.toString()
                + ",contents=" + this.dataMap.toString() + ")";
    }
}
