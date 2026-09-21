package com.sun.java.accessibility.util;

import java.beans.PropertyChangeListener;

/**
 * It listens to the property changes of <strong>the accessible objects</strong>, not of the
 * components.
 *
 * <h2>How it differs from the other two monitors</h2>
 *
 * <p>{@link AWTEventMonitor} and {@link SwingEventMonitor} listen to events of the interface: a
 * click, a key, a change of focus. This one listens to the <strong>accessibility tree</strong>,
 * which is another thing: that something's accessible name changed, that an element became
 * disabled, that the selection changed.
 *
 * <p>The distinction matters because there is no one-to-one correspondence. A single interface
 * event may change several accessible properties, and a property may change with no interface
 * event at all -- when the program modifies it directly.
 *
 * <p>A screen reader needs both: those in order to know what the user did, this one in order to
 * know what changed in what there is to read.
 */
public class AccessibilityEventMonitor {

    /** The shared list, with the same format as the other monitors'. */
    protected static final AccessibilityListenerList listenerList =
            new AccessibilityListenerList();

    public AccessibilityEventMonitor() {
    }

    /** It listens to the property changes of any accessible object. */
    public static void addPropertyChangeListener(PropertyChangeListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(PropertyChangeListener.class, l);
    }

    /** It stops listening to them. */
    public static void removePropertyChangeListener(PropertyChangeListener l) {
        listenerList.remove(PropertyChangeListener.class, l);
    }
}
