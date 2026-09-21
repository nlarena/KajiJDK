package javax.naming.ldap;

/**
 * The simplest implementation of {@link Control}: it stores the three pieces of data and returns
 * them.
 *
 * <p>It serves two purposes. To send a control this library does not model --the OID and the bytes
 * are enough-- and as the base of the ones it does model: {@link SortControl} and company extend
 * this and all they add is building the encoded value.
 *
 * <p>The fields are {@code protected} and not private because the JDK exposes them that way to
 * subclasses.
 */
public class BasicControl implements Control {

    private static final long serialVersionUID = -4233907508771791687L;

    /** The OID. */
    protected String id;

    /** Whether it is critical. */
    protected boolean criticality = false;

    /** The encoded value, or {@code null}. */
    protected byte[] value = null;

    /** A non-critical control with no value. */
    public BasicControl(String id) {
        this.id = id;
    }

    /**
     * A control with everything.
     *
     * <p>The array is kept by reference, not copied -- that is what the JDK does, and changing it
     * after construction changes the control.
     */
    public BasicControl(String id, boolean criticality, byte[] value) {
        this.id = id;
        this.criticality = criticality;
        this.value = value;
    }

    public String getID() {
        return this.id;
    }

    public boolean isCritical() {
        return this.criticality;
    }

    public byte[] getEncodedValue() {
        return this.value;
    }
}
