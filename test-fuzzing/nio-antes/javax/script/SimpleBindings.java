package javax.script;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * KajiLibrary's javax.script.SimpleBindings -- the everyday {@link Bindings}.
 *
 * <p>It is a backing {@link Map} plus a guard. All the logic is in `checkKey`, which runs before
 * each operation that touches a key and decides among three different errors:
 *
 * <ul>
 *   <li>null key -&gt; {@link NullPointerException}, "key can not be null"
 *   <li>key that is not a {@code String} -&gt; {@link ClassCastException}, "key should be a String"
 *   <li>empty key -&gt; {@link IllegalArgumentException}, "key can not be empty"
 * </ul>
 *
 * <p>The order matters and it is that one: null before type, type before empty. And the guard is
 * also in the **reading** methods ({@code get}, {@code containsKey}, {@code remove}), which is not
 * usual in a map -- a `HashMap` accepts any key and returns null. Not here: if the key cannot be a
 * variable name, asking about it is the asker's error, not an "it is not there".
 *
 * <p>There is a detail of the constructor with a map worth keeping in mind: it **does not copy**.
 * It keeps the reference, so what is put into the map from outside shows up here, even skipping the
 * key guard. It is on purpose -- it allows wrapping a map that already exists -- but it means the
 * rules above hold for what comes in *through this class*, not for what was already there.
 *
 * <p>This class does not override `equals`, `hashCode` or `toString`, just like the original: two
 * `SimpleBindings` with the same contents are not equal. It is not an oversight of ours; it is what
 * the reference implementation does and there is code that depends on identity.
 */
public class SimpleBindings implements Bindings {

    /** The backing map. It is kept by reference, not copied. */
    private final Map<String, Object> map;

    /**
     * Wraps `m`, without copying it.
     *
     * @throws NullPointerException if `m` is null
     */
    public SimpleBindings(Map<String, Object> m) {
        if (m == null) {
            throw new NullPointerException();
        }
        this.map = m;
    }

    /** With an empty {@link HashMap} as backing. */
    public SimpleBindings() {
        this(new HashMap<String, Object>());
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if `name` is null
     * @throws IllegalArgumentException if `name` is empty
     */
    @Override
    public Object put(String name, Object value) {
        checkKey(name);
        return map.put(name, value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>It validates key by key as they are copied, so a map with a bad key may leave the ones
     * that came before copied. It is the same thing the original does.
     *
     * @throws NullPointerException if `toMerge` is null, or if any key is
     * @throws IllegalArgumentException if any key is empty
     */
    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        Objects.requireNonNull(toMerge, "toMerge map is null");
        for (Map.Entry<? extends String, ? extends Object> entry : toMerge.entrySet()) {
            String key = entry.getKey();
            checkKey(key);
            put(key, entry.getValue());
        }
    }

    /** Empties the map. */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if `key` is null
     * @throws ClassCastException if `key` is not a `String`
     * @throws IllegalArgumentException if `key` is empty
     */
    @Override
    public boolean containsKey(Object key) {
        checkKey(key);
        return map.containsKey(key);
    }

    /** Whether any value is equal to `value`. There is no rule on the values. */
    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    /** The entries of the backing map, live. */
    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if `key` is null
     * @throws ClassCastException if `key` is not a `String`
     * @throws IllegalArgumentException if `key` is empty
     */
    @Override
    public Object get(Object key) {
        checkKey(key);
        return map.get(key);
    }

    /** Whether there is no entry. */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /** The keys of the backing map, live. */
    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if `key` is null
     * @throws ClassCastException if `key` is not a `String`
     * @throws IllegalArgumentException if `key` is empty
     */
    @Override
    public Object remove(Object key) {
        checkKey(key);
        return map.remove(key);
    }

    /** How many entries there are. */
    @Override
    public int size() {
        return map.size();
    }

    /** The values of the backing map, live. */
    @Override
    public Collection<Object> values() {
        return map.values();
    }

    /**
     * The guard: null, then type, then empty. That order is part of the observable contract.
     */
    private void checkKey(Object key) {
        if (key == null) {
            throw new NullPointerException("key can not be null");
        }
        if (!(key instanceof String)) {
            throw new ClassCastException("key should be a String");
        }
        if (((String) key).isEmpty()) {
            throw new IllegalArgumentException("key can not be empty");
        }
    }
}
