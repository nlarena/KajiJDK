package javax.management;

/**
 * "El atributo tal paso de este valor a este otro."
 *
 * <p>Lleva el valor viejo <b>y</b> el nuevo, y eso es lo que la hace util de verdad: el que escucha
 * no tiene que haber leido antes el atributo ni guardar estado para saber que cambio. Lleva ademas
 * el tipo declarado, porque los dos valores son `Object` y sin el no se sabria como interpretarlos.
 */
public class AttributeChangeNotification extends Notification {

    private static final long serialVersionUID = 535176054565814134L;

    /** El unico tipo que usa esta clase: {@value}. */
    public static final String ATTRIBUTE_CHANGE = "jmx.attribute.change";

    /**
     * @serial nombre del atributo que cambio
     */
    private String attributeName = null;

    /**
     * @serial su tipo declarado
     */
    private String attributeType = null;

    /**
     * @serial el valor de antes
     */
    private Object oldValue = null;

    /**
     * @serial el valor de ahora
     */
    private Object newValue = null;

    /**
     * El tipo de la notificacion queda fijado en {@link #ATTRIBUTE_CHANGE}: no se elige, porque el
     * que la recibe filtra por ese tipo.
     */
    public AttributeChangeNotification(Object source, long sequenceNumber, long timeStamp,
                                       String msg, String attributeName, String attributeType,
                                       Object oldValue, Object newValue) {
        super(AttributeChangeNotification.ATTRIBUTE_CHANGE, source, sequenceNumber, timeStamp, msg);
        this.attributeName = attributeName;
        this.attributeType = attributeType;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /** El nombre del atributo que cambio. */
    public String getAttributeName() {
        return attributeName;
    }

    /** Su tipo declarado, como cadena. */
    public String getAttributeType() {
        return attributeType;
    }

    /** El valor de antes. */
    public Object getOldValue() {
        return oldValue;
    }

    /** El valor de ahora. */
    public Object getNewValue() {
        return newValue;
    }
}
