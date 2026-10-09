package java.beans;

import java.util.EventObject;

// A property's change: who underwent it, which property, and the two values.
//
// `propertyName` may be null, and it is no oversight: it means "several properties changed at once,
// check the whole object". The listeners have to tolerate that case.
//
// The old and new values may also be null when they are not known; receiving both as null does not
// say "it went from null to null", it says "it is not known".
public class PropertyChangeEvent extends EventObject {

    private String propertyName;
    private Object newValue;
    private Object oldValue;
    private Object propagationId;

    public PropertyChangeEvent(Object source, String propertyName, Object oldValue, Object newValue) {
        super(source);
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    // The property's name, or null if more than one changed.
    public String getPropertyName() {
        return this.propertyName;
    }

    public Object getNewValue() {
        return this.newValue;
    }

    public Object getOldValue() {
        return this.oldValue;
    }

    // A free mark so that whoever chains events can avoid cycles; the library does not interpret
    // it.
    public void setPropagationId(Object propagationId) {
        this.propagationId = propagationId;
    }

    public Object getPropagationId() {
        return this.propagationId;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());
        sb.append("[propertyName=").append(this.propertyName);
        this.appendTo(sb);
        sb.append("; oldValue=").append(this.oldValue);
        sb.append("; newValue=").append(this.newValue);
        sb.append("; propagationId=").append(this.propagationId);
        sb.append("; source=").append(this.getSource());
        sb.append("]");
        return sb.toString();
    }

    // A hook so that IndexedPropertyChangeEvent can put its index in without redoing the whole
    // toString.
    void appendTo(StringBuilder sb) {
    }
}
