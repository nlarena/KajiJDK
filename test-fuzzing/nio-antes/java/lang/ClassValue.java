package java.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's java.lang.ClassValue — a value computed once per Class and remembered.
 *
 * It is a cache whose key is a type, which sounds like a HashMap<Class<?>, T> and in the JDK
 * is not one: the association is meant to live IN the class, so it dies with the class
 * instead of pinning it alive in a static map. That is the whole reason the type exists
 * rather than being a documented idiom.
 *
 * The subclass supplies computeValue and nothing else. Everything about WHEN it runs belongs
 * to this class: at most once per class, and again after remove().
 *
 * @implNote A KajiLibrary subset, and a simplification with teeth: the association is held in
 * a plain map here, so a class in this cache is a class that cannot be unloaded.
 * Ours does not unload classes yet, which is the only reason that is tolerable.
 */
public abstract class ClassValue<T> {

    private final Object sync = new Object();

    private final Map<Class<?>, Object> cache;

    protected ClassValue() {
        this.cache = new HashMap<Class<?>, Object>();
    }

    /**
     * Computes the value for `type`. Called at most once per class until remove() is called,
     * and never with null.
     */
    protected abstract T computeValue(Class<?> type);

    // The value for `type`, computing it the first time.
    //
    // computeValue runs OUTSIDE the monitor, deliberately: it is the subclass code and may do
    // anything, including asking another ClassValue for a value. Two threads racing may both
    // compute; only the first to store wins, and both callers see that one.
    @SuppressWarnings("unchecked")
    public T get(Class<?> type) {
        if (type == null) {
            throw new NullPointerException();
        }
        Object found;
        synchronized (this.sync) {
            found = this.cache.get(type);
        }
        if (found != null) {
            return (T) found;
        }
        T computed = this.computeValue(type);
        Object winner;
        synchronized (this.sync) {
            Object other = this.cache.get(type);
            if (other == null) {
                this.cache.put(type, computed);
                winner = computed;
            } else {
                winner = other;
            }
        }
        return (T) winner;
    }

    /**
     * Forgets the value for `type`, so the next get() computes it again.
     */
    public void remove(Class<?> type) {
        if (type == null) {
            throw new NullPointerException();
        }
        synchronized (this.sync) {
            this.cache.remove(type);
        }
    }
}
