package java.beans;

// The change of ONE element of an indexed property. Without the index the listener would have to
// walk the whole array to know what moved.
public class IndexedPropertyChangeEvent extends PropertyChangeEvent {

    private int index;

    public IndexedPropertyChangeEvent(Object source, String propertyName,
                                      Object oldValue, Object newValue, int index) {
        super(source, propertyName, oldValue, newValue);
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    void appendTo(StringBuilder sb) {
        sb.append("; index=").append(this.index);
    }
}
