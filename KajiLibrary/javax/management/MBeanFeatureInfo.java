package javax.management;

import java.io.Serializable;

/**
 * Lo que comparten todas las piezas de un {@link MBeanInfo}: un nombre, una descripcion y un
 * {@link Descriptor}.
 *
 * <p>La descripcion es para leer, no para programar: nada de JMX la interpreta. El nombre si es
 * significativo -- es por el que se pide el atributo o se invoca la operacion.
 */
public class MBeanFeatureInfo implements Serializable, DescriptorRead {

    static final long serialVersionUID = 3952882688968447265L;

    /**
     * @serial el nombre
     */
    protected String name;

    /**
     * @serial el texto para leer
     */
    protected String description;

    private transient Descriptor descriptor;

    public MBeanFeatureInfo(String name, String description) {
        this(name, description, null);
    }

    /** Un `descriptor` nulo se guarda como el vacio: {@link #getDescriptor()} nunca da `null`. */
    public MBeanFeatureInfo(String name, String description, Descriptor descriptor) {
        this.name = name;
        this.description = description;
        this.descriptor = descriptor == null ? ImmutableDescriptor.EMPTY_DESCRIPTOR : descriptor;
    }

    /** El nombre. */
    public String getName() {
        return name;
    }

    /** El texto para leer. */
    public String getDescription() {
        return description;
    }

    /** Nunca `null`. */
    public Descriptor getDescriptor() {
        return descriptor == null ? ImmutableDescriptor.EMPTY_DESCRIPTOR : descriptor;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanFeatureInfo)) {
            return false;
        }
        MBeanFeatureInfo p = (MBeanFeatureInfo) o;
        return igual(p.getName(), getName())
                && igual(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor());
    }

    static boolean igual(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    public int hashCode() {
        return getName().hashCode() ^ getDescription().hashCode();
    }
}
