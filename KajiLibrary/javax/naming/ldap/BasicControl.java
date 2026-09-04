package javax.naming.ldap;

/**
 * La implementacion mas simple de {@link Control}: guarda los tres datos y los devuelve.
 *
 * <p>Sirve para dos cosas. Para mandar un control que esta biblioteca no modela —basta el OID y los
 * bytes— y como base de los que si modela: {@link SortControl} y compania heredan de aca y lo unico
 * que agregan es armar el valor codificado.
 *
 * <p>Los campos son {@code protected} y no privados porque el JDK los expone asi a las subclases,
 * que es justamente como {@link PagedResultsResponseControl} lee lo que llego.
 */
public class BasicControl implements Control {

    private static final long serialVersionUID = -4233907508771791687L;

    /** El OID. */
    protected String id;

    /** Si es critico. */
    protected boolean criticality = false;

    /** El valor codificado, o {@code null}. */
    protected byte[] value = null;

    /** Un control no critico y sin valor. */
    public BasicControl(String id) {
        this.id = id;
    }

    /**
     * Un control con todo.
     *
     * <p>El arreglo se guarda por referencia, no se copia — es lo que hace el JDK, y cambiarlo
     * despues de construir cambia el control.
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
