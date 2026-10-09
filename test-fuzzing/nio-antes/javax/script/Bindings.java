package javax.script;

import java.util.Map;

/**
 * KajiLibrary's javax.script.Bindings -- a map from names to values, with the keys restricted.
 *
 * <p>It is the bridge between the hosting program and the running script: what the engine sees as a
 * global variable comes from here, and what the script leaves defined comes back the same way.
 * That is why the interface is a {@code Map<String,Object>} and not something of its own -- the
 * host already knows how to use a map.
 *
 * <p>The only thing it adds over {@link Map} is a restriction on the keys: they have to be {@code
 * String}s, non-null and non-empty. The restriction is not decorative. An empty variable name
 * exists in no scripting language, and a key that is not text cannot be translated into an
 * identifier; letting them in would postpone the error until inside the engine, where it is no
 * longer known where it came from. The five redeclarations of {@code Map} methods here change no
 * signature: they are there to document those exceptions in the contract.
 *
 * <p>The default {@code put(Object,Object)} is the one {@link Map} demands after type erasure. It
 * casts and delegates to {@link #put(String,Object)}, which is the only honest way of serving it:
 * if the key is not a {@code String}, the one that complains is the cast.
 */
public interface Bindings extends Map<String, Object> {

    /**
     * Associates `value` with `name`.
     *
     * @throws NullPointerException if `name` is null
     * @throws IllegalArgumentException if `name` is empty
     */
    Object put(String name, Object value);

    /**
     * Copies every entry of `toMerge`, each one with the same rules as {@link #put(String,Object)}.
     *
     * @throws NullPointerException if `toMerge` is null, or if any key is
     * @throws IllegalArgumentException if any key is empty
     */
    void putAll(Map<? extends String, ? extends Object> toMerge);

    /**
     * Whether there is an entry with that key.
     *
     * @throws NullPointerException if `key` is null
     * @throws ClassCastException if `key` is not a `String`
     * @throws IllegalArgumentException if `key` is empty
     */
    boolean containsKey(Object key);

    /**
     * The value associated with `key`, or null.
     *
     * @throws NullPointerException if `key` is null
     * @throws ClassCastException if `key` is not a `String`
     * @throws IllegalArgumentException if `key` is empty
     */
    Object get(Object key);

    /**
     * Removes the entry of `key` and returns what it held.
     *
     * @throws NullPointerException if `key` is null
     * @throws ClassCastException if `key` is not a `String`
     * @throws IllegalArgumentException if `key` is empty
     */
    Object remove(Object key);

    /**
     * {@link Map}'s {@code put} seen with erased types: it casts and delegates.
     *
     * @throws ClassCastException if `key` is not a `String`
     */
    default Object put(Object key, Object value) {
        return put((String) key, value);
    }
}
