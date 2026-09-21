package javax.management;

/**
 * "Attribute such-and-such went from this value to this other one."
 *
 * <p>It carries the old value <b>and</b> the new one, and that is what makes it really useful: the
 * listener does not have to have read the attribute beforehand nor keep state to know what
 * changed. It also carries the declared type, because both values are {@code Object} and without
 * it there would be no knowing how to interpret them.
 */
public class AttributeChangeNotification extends Notification {

    private static final long serialVersionUID = 535176054565814134L;

    /** The only type this class uses: {@value}. */
    public static final String ATTRIBUTE_CHANGE = "jmx.attribute.change";

    /**
     * @serial name of the attribute that changed
     */
    private String attributeName = null;

    /**
     * @serial its declared type
     */
    private String attributeType = null;

    /**
     * @serial the value before
     */
    private Object oldValue = null;

    /**
     * @serial the value now
     */
    private Object newValue = null;

    /**
     * The notification type is fixed at {@link #ATTRIBUTE_CHANGE}: it is not chosen, because
     * whoever receives it filters by that type.
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

    /** The name of the attribute that changed. */
    public String getAttributeName() {
        return attributeName;
    }

    /** Its declared type, as a string. */
    public String getAttributeType() {
        return attributeType;
    }

    /** The value before. */
    public Object getOldValue() {
        return oldValue;
    }

    /** The value now. */
    public Object getNewValue() {
        return newValue;
    }
}
