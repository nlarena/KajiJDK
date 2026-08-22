package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.Enumeration;

// The abstract key→value store from Java 1.0, superseded by {@link Map} in 1.2 and kept
// alive only because {@link Hashtable} extends it. It is worth reading precisely as a
// museum piece: it is a *class* where Map is an interface, which is the mistake it exists
// to illustrate — a store that wants to be a Dictionary cannot also extend anything else,
// and every method it might one day gain becomes a compatibility problem for its
// subclasses. Java 1.2 replaced the whole hierarchy with interfaces for that reason.
//
// The other 1.0 fossil it carries is {@link Enumeration}: iteration is handed out as
// `keys()` and `elements()` rather than as views you can operate on.
public abstract class Dictionary<K, V> {

    public Dictionary() {
    }

    public abstract int size();

    public abstract boolean isEmpty();

    public abstract Enumeration<K> keys();

    public abstract Enumeration<V> elements();

    public abstract V get(Object key);

    public abstract V put(K key, V value);

    public abstract V remove(Object key);
}
