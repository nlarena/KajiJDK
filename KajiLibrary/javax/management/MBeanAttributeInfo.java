package javax.management;

import java.lang.reflect.Method;

/**
 * Un atributo declarado por un MBean: nombre, tipo y que se puede hacer con el.
 *
 * <p>Tres booleanos y no dos, que es lo que sorprende. Ademas de lectura y escritura esta
 * {@link #isIs()}, que dice si el accesor se llama {@code isX} en vez de {@code getX}. Solo tiene
 * sentido para `boolean`, y esta porque un MBean estandar se descubre por reflexion sobre los
 * nombres de los metodos: sin ese bit, el servidor no sabria como llamar de vuelta.
 *
 * <p>Un atributo con lectura y escritura en `false` es legal, aunque inutil: describe algo que el
 * MBean declara y no deja tocar.
 */
public class MBeanAttributeInfo extends MBeanFeatureInfo implements Cloneable {

    static final long serialVersionUID = 8644704819898565848L;

    static final MBeanAttributeInfo[] NO_ATTRIBUTES = new MBeanAttributeInfo[0];

    /**
     * @serial el nombre de la clase del atributo
     */
    private final String attributeType;

    /**
     * @serial si se puede escribir
     */
    private final boolean isWrite;

    /**
     * @serial si se puede leer
     */
    private final boolean isRead;

    /**
     * @serial si el accesor de lectura se llama isX
     */
    private final boolean is;

    public MBeanAttributeInfo(String name, String type, String description,
                              boolean isReadable, boolean isWritable, boolean isIs) {
        this(name, type, description, isReadable, isWritable, isIs, null);
    }

    public MBeanAttributeInfo(String name, String type, String description,
                              boolean isReadable, boolean isWritable, boolean isIs,
                              Descriptor descriptor) {
        super(name, description, descriptor);
        this.attributeType = type;
        this.isRead = isReadable;
        this.isWrite = isWritable;
        this.is = isIs;
    }

    /**
     * Deduce todo de los dos metodos accesores.
     *
     * <p>Cualquiera de los dos puede ser `null`: eso es lo que hace a un atributo de solo lectura o
     * de solo escritura.
     *
     * @throws IntrospectionException si los dos metodos no hablan del mismo tipo
     */
    public MBeanAttributeInfo(String name, String description, Method getter, Method setter)
            throws IntrospectionException {
        this(name, tipoDe(getter, setter), description,
             getter != null, setter != null, esIs(getter));
    }

    private static boolean esIs(Method getter) {
        return getter != null && getter.getName().startsWith("is");
    }

    private static String tipoDe(Method getter, Method setter) throws IntrospectionException {
        String delGetter = null;
        if (getter != null) {
            if (getter.getParameterTypes().length != 0) {
                throw new IntrospectionException("bad getter arg count");
            }
            Class<?> r = getter.getReturnType();
            if (r == Void.TYPE) {
                throw new IntrospectionException("getter returns void");
            }
            delGetter = r.getName();
        }
        String delSetter = null;
        if (setter != null) {
            Class<?>[] p = setter.getParameterTypes();
            if (p.length != 1) {
                throw new IntrospectionException("bad setter arg count");
            }
            delSetter = p[0].getName();
        }
        if (delGetter == null && delSetter == null) {
            throw new IntrospectionException("getter and setter cannot both be null");
        }
        if (delGetter != null && delSetter != null && !delGetter.equals(delSetter)) {
            throw new IntrospectionException("type mismatch between getter and setter");
        }
        return delGetter != null ? delGetter : delSetter;
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

    /** El nombre de la clase del atributo. */
    public String getType() {
        return attributeType;
    }

    /** Si se puede leer. */
    public boolean isReadable() {
        return isRead;
    }

    /** Si se puede escribir. */
    public boolean isWritable() {
        return isWrite;
    }

    /** Si el accesor de lectura se llama {@code isX} en vez de {@code getX}. */
    public boolean isIs() {
        return is;
    }

    /**
     * El acceso se imprime como {@code read-only}, {@code write-only}, {@code read/write} o
     * {@code no-access}, y el bit de {@code isIs} agrega una coma mas.
     */
    public String toString() {
        String acceso;
        if (isReadable()) {
            acceso = isWritable() ? "read/write" : "read-only";
        } else {
            acceso = isWritable() ? "write-only" : "no-access";
        }
        return getClass().getName() + "[description=" + getDescription() + ", name=" + getName()
                + ", type=" + getType() + ", " + acceso + (isIs() ? ", isIs" : "")
                + ", descriptor=" + getDescriptor() + "]";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanAttributeInfo)) {
            return false;
        }
        MBeanAttributeInfo p = (MBeanAttributeInfo) o;
        return igual(p.getName(), getName())
                && igual(p.getType(), getType())
                && igual(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor())
                && p.isReadable() == isReadable()
                && p.isWritable() == isWritable()
                && p.isIs() == isIs();
    }

    public int hashCode() {
        return getName().hashCode() ^ getType().hashCode();
    }
}
