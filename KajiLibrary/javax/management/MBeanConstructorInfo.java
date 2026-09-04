package javax.management;

import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 * Un constructor publico del MBean.
 *
 * <p>Esta en el modelo porque {@link MBeanServer#createMBean} permite crear un MBean <b>dentro</b>
 * del agente pasando argumentos: el cliente elige la firma mirando esta lista. Sin ella no tendria
 * como saber que constructores hay.
 */
public class MBeanConstructorInfo extends MBeanFeatureInfo implements Cloneable {

    static final long serialVersionUID = 4433990064191844427L;

    static final MBeanConstructorInfo[] NO_CONSTRUCTORS = new MBeanConstructorInfo[0];

    /**
     * @serial los parametros
     */
    private final MBeanParameterInfo[] signature;

    /**
     * El <b>nombre</b> sale de la reflexion --es el nombre de la clase-- y el argumento que se pasa
     * es la descripcion. El orden invertido respecto de los otros constructores confunde, y es asi
     * en el JDK.
     */
    public MBeanConstructorInfo(String description, Constructor<?> constructor) {
        this(constructor.getName(), description, firmaDe(constructor), null);
    }

    public MBeanConstructorInfo(String name, String description,
                                MBeanParameterInfo[] signature) {
        this(name, description, signature, null);
    }

    public MBeanConstructorInfo(String name, String description,
                                MBeanParameterInfo[] signature, Descriptor descriptor) {
        super(name, description, descriptor);
        this.signature = signature == null || signature.length == 0
                ? MBeanParameterInfo.NO_PARAMS : copia(signature);
    }

    private static MBeanParameterInfo[] copia(MBeanParameterInfo[] s) {
        MBeanParameterInfo[] r = new MBeanParameterInfo[s.length];
        System.arraycopy(s, 0, r, 0, s.length);
        return r;
    }

    private static MBeanParameterInfo[] firmaDe(Constructor<?> c) {
        Class<?>[] p = c.getParameterTypes();
        MBeanParameterInfo[] r = new MBeanParameterInfo[p.length];
        for (int i = 0; i < p.length; i++) {
            r[i] = new MBeanParameterInfo("p" + (i + 1), p[i].getName(), "");
        }
        return r;
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

    /** Copia nueva en cada llamada: el arreglo interno no se presta. */
    public MBeanParameterInfo[] getSignature() {
        return copia(signature);
    }

    public String toString() {
        return getClass().getName() + "[description=" + getDescription() + ", name=" + getName()
                + ", signature=" + MBeanInfo.aTexto(signature)
                + ", descriptor=" + getDescriptor() + "]";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanConstructorInfo)) {
            return false;
        }
        MBeanConstructorInfo p = (MBeanConstructorInfo) o;
        return igual(p.getName(), getName())
                && igual(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor())
                && Arrays.equals(p.signature, signature);
    }

    public int hashCode() {
        return getName().hashCode() ^ Arrays.hashCode(signature);
    }
}
