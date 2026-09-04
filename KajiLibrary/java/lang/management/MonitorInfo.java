package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * KajiLibrary's java.lang.management.MonitorInfo -- un monitor que un hilo tiene tomado, y donde lo
 * tomo.
 *
 * <p>Agrega sobre {@link LockInfo} las dos cosas que solo tienen los monitores --los candados de
 * {@code synchronized}--: en que marco de la pila se entro, y cual es ese marco.
 *
 * <p>Esa informacion es la que hace util un volcado: no alcanza con saber que un hilo tiene tomado un
 * candado, hay que saber <b>desde donde</b> para encontrar el bloque que no termina.
 *
 * <p>Los candados de {@code java.util.concurrent} no tienen esto y por eso salen como
 * {@code LockInfo} a secas: se toman con una llamada a metodo y no con un bloque, asi que no hay un
 * marco que los "contenga".
 *
 * <h2>La profundidad puede ser -1</h2>
 *
 * <p>Significa que la maquina virtual sabe que el monitor esta tomado pero no en que marco. Ahi el
 * marco es null, y esa es la unica combinacion permitida con null: si la profundidad es 0 o mas, el
 * marco tiene que estar.
 */
public class MonitorInfo extends LockInfo {

    /** En que marco se tomo, o -1. */
    private final int stackDepth;

    /** Cual es ese marco, o null. */
    private final StackTraceElement stackFrame;

    /**
     * @param stackDepth el indice del marco, o -1 si no se sabe
     * @param stackFrame ese marco; tiene que ser null si y solo si la profundidad es negativa
     * @throws NullPointerException si el nombre de clase es null
     * @throws IllegalArgumentException si la profundidad y el marco no concuerdan
     */
    public MonitorInfo(String className, int identityHashCode, int stackDepth,
                       StackTraceElement stackFrame) {
        super(className, identityHashCode);
        if (stackDepth >= 0 && stackFrame == null) {
            throw new IllegalArgumentException("Parameter stackDepth is " + stackDepth
                + " but stackFrame is null");
        }
        if (stackDepth < 0 && stackFrame != null) {
            throw new IllegalArgumentException("Parameter stackDepth is " + stackDepth
                + " but stackFrame is not null");
        }
        this.stackDepth = stackDepth;
        this.stackFrame = stackFrame;
    }

    /** En que marco se tomo, o -1. Ver la nota de la clase. */
    public int getLockedStackDepth() {
        return this.stackDepth;
    }

    /** Cual es ese marco, o null. */
    public StackTraceElement getLockedStackFrame() {
        return this.stackFrame;
    }

    /**
     * Lo mismo, leido de un {@link CompositeData}.
     *
     * @return el objeto, o null si el dato es null
     * @throws IllegalArgumentException si el dato no describe un {@code MonitorInfo}
     */
    public static MonitorInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        final String type = "MonitorInfo";
        Object frame = CompositeItems.optional(cd, "lockedStackFrame");
        StackTraceElement element = null;
        if (frame instanceof CompositeData) {
            element = StackTraceElements.from((CompositeData) frame);
        }
        return new MonitorInfo(CompositeItems.string(cd, "className", type),
                               CompositeItems.integer(cd, "identityHashCode", type),
                               CompositeItems.integer(cd, "lockedStackDepth", type),
                               element);
    }
}
