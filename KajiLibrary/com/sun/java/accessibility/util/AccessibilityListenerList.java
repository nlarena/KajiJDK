package com.sun.java.accessibility.util;

import java.util.EventListener;

/**
 * A list of listeners that keeps, next to each one, <strong>what type</strong> it is.
 *
 * <h2>Why an array of pairs and not a map</h2>
 *
 * <p>Because the array is walked in the dispatch, which is what happens very many times, and
 * adding or taking out happens very few. A {@code Map<Class, List>} would be more comfortable to
 * write and slower to walk: one indirection per type on each event.
 *
 * <p>The format is {@code javax.swing.event.EventListenerList}'s: even positions the type, odd
 * ones the listener. Ugly to read and very cheap to walk.
 *
 * <h2>Why the array is copied on modifying</h2>
 *
 * <p>Because the dispatch happens on the event thread and the registration on any other one.
 * Copying instead of mutating makes whoever is walking go on with the array it had -- without
 * blocking on the hot path, and without the concurrent modification exception a mutable list
 * would bring.
 *
 * <p>It is the reason {@link #getListenerList} returns the internal array and that the JDK's
 * documentation says that it <strong>must not be modified</strong>: lending it is what avoids a
 * copy per event.
 */
public class AccessibilityListenerList {

    private static final Object[] EMPTY = new Object[0];

    /** (type, listener) pairs; see the class note about the format. */
    protected transient Object[] listenerList = EMPTY;

    public AccessibilityListenerList() {
    }

    /**
     * The array of pairs, lent.
     *
     * <p>It must not be modified: it is the one the dispatches under way are walking.
     */
    public Object[] getListenerList() {
        return this.listenerList;
    }

    /** How many listeners there are, of all the types. */
    public int getListenerCount() {
        return this.listenerList.length / 2;
    }

    /** How many there are of that type. */
    public int getListenerCount(Class<? extends EventListener> t) {
        int n = 0;
        Object[] list = this.listenerList;
        for (int i = 0; i < list.length; i += 2) {
            if (t == (Class<?>) list[i]) {
                n++;
            }
        }
        return n;
    }

    /**
     * It adds a listener of that type.
     *
     * <p>The same one may be added twice, and then it receives each event twice. It is what the JDK
     * does: deduplicating would force the list to be walked on each addition and would change the
     * behaviour of whoever registers twice on purpose.
     */
    public synchronized void add(Class<? extends EventListener> t, EventListener l) {
        if (l == null) {
            return;
        }
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException(
                    "the listener is not of the type " + t.getName());
        }
        Object[] fresh = new Object[this.listenerList.length + 2];
        System.arraycopy(this.listenerList, 0, fresh, 0, this.listenerList.length);
        fresh[this.listenerList.length] = t;
        fresh[this.listenerList.length + 1] = l;
        this.listenerList = fresh;
    }

    /**
     * It takes <strong>one</strong> occurrence of that listener with that type out.
     *
     * <p>One and not all, so as to be symmetric with {@link #add}: whoever added it twice has to
     * take it out twice.
     */
    public synchronized void remove(Class<? extends EventListener> t, EventListener l) {
        if (l == null) {
            return;
        }
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException(
                    "the listener is not of the type " + t.getName());
        }
        // It is looked for from the end: the most recently added is what is taken out most.
        int index = -1;
        for (int i = this.listenerList.length - 2; i >= 0; i -= 2) {
            if (this.listenerList[i] == t && this.listenerList[i + 1].equals(l)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        Object[] fresh = new Object[this.listenerList.length - 2];
        System.arraycopy(this.listenerList, 0, fresh, 0, index);
        if (index < fresh.length) {
            System.arraycopy(this.listenerList, index + 2, fresh, index,
                    fresh.length - index);
        }
        this.listenerList = fresh.length == 0 ? EMPTY : fresh;
    }

    public String toString() {
        Object[] list = this.listenerList;
        StringBuilder sb = new StringBuilder();
        sb.append("EventListenerList: ");
        sb.append(String.valueOf(list.length / 2)).append(" listeners: ");
        for (int i = 0; i <= list.length - 2; i += 2) {
            sb.append(" type ").append(((Class<?>) list[i]).getName());
            sb.append(" listener ").append(String.valueOf(list[i + 1]));
        }
        return sb.toString();
    }
}
