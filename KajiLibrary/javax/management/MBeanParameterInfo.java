package javax.management;

/**
 * Un parametro de una operacion o de un constructor.
 *
 * <p>El tipo es una <b>cadena</b> con el nombre de la clase, no un `Class`. Es a proposito: un
 * cliente remoto describe MBeans cuyas clases no tiene cargadas, y un `Class` obligaria a cargarlas
 * solo para leer los metadatos.
 */
public class MBeanParameterInfo extends MBeanFeatureInfo implements Cloneable {

    static final long serialVersionUID = 7432616882776782338L;

    static final MBeanParameterInfo[] NO_PARAMS = new MBeanParameterInfo[0];

    /**
     * @serial el nombre de la clase del parametro
     */
    private final String type;

    public MBeanParameterInfo(String name, String type, String description) {
        this(name, type, description, null);
    }

    public MBeanParameterInfo(String name, String type, String description,
                              Descriptor descriptor) {
        super(name, description, descriptor);
        this.type = type;
    }

    /**
     * Copia superficial.
     *
     * <p>No devuelve `this` aunque la clase sea inmutable: se comprobo contra el JDK y ahi la
     * copia es un objeto <b>distinto</b>. Igual por `equals`, distinto por identidad.
     *
     * <p>Traga la `CloneNotSupportedException` y devuelve `null` en vez de propagarla, como el
     * JDK: la clase implementa `Cloneable`, asi que no puede ocurrir, y declararla obligaria a
     * atajarla a todo el que llame.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /** El nombre de la clase del parametro. */
    public String getType() {
        return type;
    }

    public String toString() {
        return getClass().getName() + "[description=" + getDescription() + ", name=" + getName()
                + ", type=" + getType() + ", descriptor=" + getDescriptor() + "]";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanParameterInfo)) {
            return false;
        }
        MBeanParameterInfo p = (MBeanParameterInfo) o;
        return igual(p.getName(), getName())
                && igual(p.getType(), getType())
                && igual(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor());
    }

    public int hashCode() {
        return getName().hashCode() ^ getType().hashCode();
    }
}
