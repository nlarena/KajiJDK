package java.util;

import java.io.Serializable;

// The root of every event: it carries the object that gave rise to it and nothing more.
//
// `source` is `transient` and the object is `Serializable` all the same: it is deliberate in the JDK
// and worth understanding. Serialising an event must not drag along the component that fired it —a
// window, a connection— which is almost never serialisable and almost never makes sense to send. A
// deserialised EventObject has `source` at null.
public class EventObject implements Serializable {

    // The object the event happened on.
    protected transient Object source;

    // An event that arose from `source`.
    public EventObject(Object source) {
        if (source == null) {
            throw new IllegalArgumentException("null source");
        }
        this.source = source;
    }

    // The object the event arose from.
    public Object getSource() {
        return this.source;
    }

    public String toString() {
        return this.getClass().getName() + "[source=" + this.source + "]";
    }
}
