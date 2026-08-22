package java.util;

import java.util.Map.Entry;

// The skeleton for maps. In the JDK a subclass provides `entrySet()` and everything else —
// size, get, containsKey, the iteration — is derived from walking it.
//
// KajiLibrary's `Map` is the subset without the collection views, so there is no entry set to
// walk: `entrySet()` is declared (a subclass may still provide one) but the derivations that
// would need it are left to the subclass. What this class does give, and what makes it worth
// having, is the pair every map otherwise has to repeat: `isEmpty()` in terms of `size()`, and
// the mutators refusing by default so a read-only map inherits the right behaviour.
public abstract class AbstractMap<K, V> implements Map<K, V> {

    protected AbstractMap() {
    }

    public abstract Set<Entry<K, V>> entrySet();

    public abstract int size();

    public boolean isEmpty() {
        return size() == 0;
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}
