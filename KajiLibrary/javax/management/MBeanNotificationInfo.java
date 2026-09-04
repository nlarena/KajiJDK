package javax.management;

import java.util.Arrays;

/**
 * Una clase de notificacion que el MBean puede emitir.
 *
 * <p>Ojo con el orden de los argumentos: {@code name} es el nombre de la <b>clase Java</b> de la
 * notificacion --casi siempre {@code javax.management.Notification}-- y los <b>tipos</b>, que son
 * las cadenas con puntos por las que se filtra, van en el primer argumento. Se confunden todo el
 * tiempo porque el nombre util para un cliente es el tipo, no la clase.
 */
public class MBeanNotificationInfo extends MBeanFeatureInfo implements Cloneable {

    static final long serialVersionUID = -3888371564530107064L;

    private static final String[] NO_TYPES = new String[0];

    static final MBeanNotificationInfo[] NO_NOTIFICATIONS = new MBeanNotificationInfo[0];

    /**
     * @serial los tipos que se pueden emitir
     */
    private String[] types;

    /**
     * @param notifTypes los tipos, con la convencion de puntos
     * @param name el nombre de la clase Java de la notificacion
     */
    public MBeanNotificationInfo(String[] notifTypes, String name, String description) {
        this(notifTypes, name, description, null);
    }

    public MBeanNotificationInfo(String[] notifTypes, String name, String description,
                                 Descriptor descriptor) {
        super(name, description, descriptor);
        this.types = notifTypes == null || notifTypes.length == 0 ? NO_TYPES : copia(notifTypes);
    }

    private static String[] copia(String[] s) {
        String[] r = new String[s.length];
        System.arraycopy(s, 0, r, 0, s.length);
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

    /** Los tipos, copiados. */
    public String[] getNotifTypes() {
        return copia(types);
    }

    public String toString() {
        return getClass().getName() + "[description=" + getDescription() + ", name=" + getName()
                + ", notifTypes=" + MBeanInfo.aTexto(types)
                + ", descriptor=" + getDescriptor() + "]";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanNotificationInfo)) {
            return false;
        }
        MBeanNotificationInfo p = (MBeanNotificationInfo) o;
        return igual(p.getName(), getName())
                && igual(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor())
                && Arrays.equals(p.types, types);
    }

    public int hashCode() {
        int h = getName().hashCode();
        for (int i = 0; i < types.length; i++) {
            h ^= types[i].hashCode();
        }
        return h;
    }
}
