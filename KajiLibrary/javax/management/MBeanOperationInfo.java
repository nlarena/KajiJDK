package javax.management;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Una operacion que el MBean expone.
 *
 * <p>Lo distintivo es {@link #getImpact()}: el modelo declara si la operacion <b>lee</b>
 * ({@link #INFO}), <b>modifica</b> ({@link #ACTION}) o las dos cosas ({@link #ACTION_INFO}). No es
 * decoracion -- una consola puede ofrecer las de solo lectura sin pedir confirmacion y pedirla para
 * las otras, y un cliente automatico puede reintentar sin miedo una operacion que declara ser
 * `INFO`. {@link #UNKNOWN} es lo que queda cuando nadie se molesto en decirlo.
 */
public class MBeanOperationInfo extends MBeanFeatureInfo implements Cloneable {

    static final long serialVersionUID = -6178860474881375330L;

    static final MBeanOperationInfo[] NO_OPERATIONS = new MBeanOperationInfo[0];

    /** La operacion solo lee: {@value}. */
    public static final int INFO = 0;

    /** La operacion modifica el MBean: {@value}. */
    public static final int ACTION = 1;

    /** Modifica y ademas devuelve informacion: {@value}. */
    public static final int ACTION_INFO = 2;

    /** No se declaro: {@value}. */
    public static final int UNKNOWN = 3;

    /**
     * @serial el nombre de la clase que devuelve
     */
    private final String type;

    /**
     * @serial los parametros
     */
    private final MBeanParameterInfo[] signature;

    /**
     * @serial INFO, ACTION, ACTION_INFO o UNKNOWN
     */
    private final int impact;

    /**
     * Desde un metodo por reflexion. El impacto queda en {@link #UNKNOWN}: la reflexion puede leer
     * la firma pero no puede saber si el metodo cambia algo.
     */
    public MBeanOperationInfo(String description, Method method) {
        this(method.getName(), description, firmaDe(method),
             method.getReturnType().getName(),
             method.getReturnType() == Void.TYPE ? ACTION : UNKNOWN, null);
    }

    public MBeanOperationInfo(String name, String description, MBeanParameterInfo[] signature,
                              String type, int impact) {
        this(name, description, signature, type, impact, null);
    }

    public MBeanOperationInfo(String name, String description, MBeanParameterInfo[] signature,
                              String type, int impact, Descriptor descriptor) {
        super(name, description, descriptor);
        this.type = type;
        this.impact = impact;
        this.signature = signature == null || signature.length == 0
                ? MBeanParameterInfo.NO_PARAMS : copia(signature);
    }

    private static MBeanParameterInfo[] copia(MBeanParameterInfo[] s) {
        MBeanParameterInfo[] r = new MBeanParameterInfo[s.length];
        System.arraycopy(s, 0, r, 0, s.length);
        return r;
    }

    private static MBeanParameterInfo[] firmaDe(Method m) {
        Class<?>[] p = m.getParameterTypes();
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

    /** El nombre de la clase que devuelve la operacion. */
    public String getReturnType() {
        return type;
    }

    /** Copia nueva en cada llamada. */
    public MBeanParameterInfo[] getSignature() {
        return copia(signature);
    }

    /** {@link #INFO}, {@link #ACTION}, {@link #ACTION_INFO} o {@link #UNKNOWN}. */
    public int getImpact() {
        return impact;
    }

    public String toString() {
        String i;
        switch (impact) {
            case ACTION:
                i = "action";
                break;
            case ACTION_INFO:
                i = "action/info";
                break;
            case INFO:
                i = "info";
                break;
            case UNKNOWN:
                i = "unknown";
                break;
            default:
                i = "(" + impact + ")";
        }
        return getClass().getName() + "[description=" + getDescription() + ", name=" + getName()
                + ", returnType=" + getReturnType()
                + ", signature=" + MBeanInfo.aTexto(signature)
                + ", impact=" + i
                + ", descriptor=" + getDescriptor() + "]";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanOperationInfo)) {
            return false;
        }
        MBeanOperationInfo p = (MBeanOperationInfo) o;
        return igual(p.getName(), getName())
                && igual(p.getReturnType(), getReturnType())
                && igual(p.getDescription(), getDescription())
                && p.getImpact() == getImpact()
                && p.getDescriptor().equals(getDescriptor())
                && Arrays.equals(p.signature, signature);
    }

    public int hashCode() {
        return getName().hashCode() ^ getReturnType().hashCode();
    }
}
