package java.beans;

import java.util.EventObject;

// El cambio de una propiedad: quien lo sufrio, que propiedad, y los dos valores.
//
// `propertyName` puede ser null, y no es un descuido: significa "cambiaron varias propiedades a
// la vez, revisa el objeto entero". Los oyentes tienen que tolerar ese caso.
//
// Los valores viejo y nuevo tambien pueden ser null cuando no se conocen; recibir ambos en null
// no dice "paso de null a null", dice "no se sabe".
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

    // El nombre de la propiedad, o null si cambio mas de una.
    public String getPropertyName() {
        return this.propertyName;
    }

    public Object getNewValue() {
        return this.newValue;
    }

    public Object getOldValue() {
        return this.oldValue;
    }

    // Marca libre para que quien encadena eventos evite ciclos; la biblioteca no la interpreta.
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

    // Gancho para que IndexedPropertyChangeEvent meta su indice sin rehacer todo el toString.
    void appendTo(StringBuilder sb) {
    }
}
