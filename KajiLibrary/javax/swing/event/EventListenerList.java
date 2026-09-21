package javax.swing.event;

import java.util.EventListener;

/**
 * The list of listeners all of Swing uses, kept as {@code (class, listener)} pairs.
 *
 * <h2>Why a flat array and not a map</h2>
 *
 * <p>A component listens for many kinds of event and almost never has listeners of more than one
 * or two. A {@code Map<Class, List>} would cost several objects per component to keep, typically,
 * one element. The flat array costs a single one, and with two listeners walking it whole is
 * faster than hashing.
 *
 * <p>It is a measured optimization of the JDK's over a case that repeats thousands of times in an
 * interface.
 *
 * <h2>Copying on write, which is what makes it safe</h2>
 *
 * <p>{@link #add} and {@link #remove} create a new array instead of modifying the one there is,
 * and {@link #getListenerList} returns the array <strong>without copying</strong>. That allows
 * handing out an event by walking it without synchronizing and without fear that somebody
 * unsubscribes in the middle of the round: whoever is walking has a snapshot.
 *
 * <p>For that very reason the returned array <strong>is not touched</strong>. It is the price of
 * the deal.
 */
public class EventListenerList implements java.io.Serializable {

    private static final long serialVersionUID = -5677132037850737084L;

    private static final Object[] EMPTY = new Object[0];

    /** The pairs. Volatile: it is replaced whole, and whoever reads has to see the replacement. */
    protected transient volatile Object[] listenerList = EMPTY;

    /** An empty list. */
    public EventListenerList() {
    }

    /**
     * The raw array of pairs, <strong>without copying</strong>.
     *
     * <p>It is walked two at a time: at {@code i} the class, at {@code i+1} the listener. It is not
     * modified.
     */
    public Object[] getListenerList() {
        return this.listenerList;
    }

    /** The listeners of {@code t}, in a new array of the requested type. */
    public <T extends EventListener> T[] getListeners(Class<T> t) {
        Object[] list = this.listenerList;
        int n = getListenerCount(list, t);
        @SuppressWarnings("unchecked")
        T[] result = (T[]) java.lang.reflect.Array.newInstance(t, n);
        int j = 0;
        for (int i = list.length - 2; i >= 0; i = i - 2) {
            if (list[i] == t) {
                result[j] = (T) list[i + 1];
                j = j + 1;
            }
        }
        return result;
    }

    /** How many listeners there are, of all types. */
    public int getListenerCount() {
        return this.listenerList.length / 2;
    }

    /** How many listeners of {@code t} there are. */
    public int getListenerCount(Class<?> t) {
        return getListenerCount(this.listenerList, t);
    }

    private int getListenerCount(Object[] list, Class<?> t) {
        int n = 0;
        for (int i = 0; i < list.length; i = i + 2) {
            if (t == list[i]) {
                n = n + 1;
            }
        }
        return n;
    }

    /**
     * Adds a listener.
     *
     * @throws IllegalArgumentException if {@code l} is not of type {@code t}
     */
    public synchronized <T extends EventListener> void add(Class<T> t, T l) {
        if (l == null) {
            return;
        }
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException("The listener is not of " + t.getName());
        }
        Object[] old = this.listenerList;
        Object[] updated = new Object[old.length + 2];
        for (int i = 0; i < old.length; i++) {
            updated[i] = old[i];
        }
        updated[old.length] = t;
        updated[old.length + 1] = l;
        this.listenerList = updated;
    }

    /** Removes a listener. If it was there more than once, it removes only one. */
    public synchronized <T extends EventListener> void remove(Class<T> t, T l) {
        if (l == null) {
            return;
        }
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException("The listener is not of " + t.getName());
        }
        Object[] old = this.listenerList;
        int at = -1;
        for (int i = old.length - 2; i >= 0; i = i - 2) {
            if (old[i] == t && old[i + 1].equals(l)) {
                at = i;
                break;
            }
        }
        if (at < 0) {
            return;
        }
        Object[] updated = new Object[old.length - 2];
        int j = 0;
        for (int i = 0; i < old.length; i = i + 2) {
            if (i != at) {
                updated[j] = old[i];
                updated[j + 1] = old[i + 1];
                j = j + 2;
            }
        }
        this.listenerList = updated;
    }

    public String toString() {
        Object[] list = this.listenerList;
        StringBuilder sb = new StringBuilder("EventListenerList: ");
        sb.append(String.valueOf(list.length / 2));
        sb.append(" listeners: ");
        for (int i = 0; i < list.length; i = i + 2) {
            sb.append(" type ");
            sb.append(((Class) list[i]).getName());
            sb.append(" listener ");
            sb.append(list[i + 1]);
        }
        return sb.toString();
    }
}
