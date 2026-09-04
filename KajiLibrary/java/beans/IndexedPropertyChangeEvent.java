package java.beans;

// El cambio de UN elemento de una propiedad indexada. Sin el indice el oyente tendria que
// recorrer el arreglo entero para saber que se movio.
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
