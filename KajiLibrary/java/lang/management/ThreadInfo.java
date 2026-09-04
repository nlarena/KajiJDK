package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * KajiLibrary's java.lang.management.ThreadInfo -- la foto de un hilo.
 *
 * <p>Todo lo que se puede saber de un hilo desde afuera: su estado, su pila, que candado espera, quien
 * lo tiene, y --si se pidio-- que candados tiene tomados el.
 *
 * <h2>No tiene constructor publico</h2>
 *
 * <p>Y es a proposito: solo la maquina virtual puede armar uno coherente. Se consigue por
 * {@link ThreadMXBean}, o se rearma desde un {@link CompositeData} con {@link #from} cuando vino de
 * otra maquina virtual.
 *
 * <h2>Es una foto, no una vista</h2>
 *
 * <p>Lo que dice era cierto en el instante en que se tomo. Un hilo que figura {@code BLOCKED} puede
 * estar corriendo cuando se lee. Sirve para diagnosticar, no para decidir.
 *
 * <p>La excepcion util es el interbloqueo: si {@code findDeadlockedThreads} lo reporto, eso no cambia
 * solo -- por definicion.
 *
 * <h2>Los valores que faltan</h2>
 *
 * <p>{@link #getStackTrace} devuelve un arreglo <b>vacio</b> si no se pidio pila, no null.
 * {@link #getBlockedTime} y {@link #getWaitedTime} devuelven -1 si el seguimiento de contencion esta
 * apagado, que es lo normal. {@link #getLockOwnerId} devuelve -1 si no lo tiene nadie, y
 * {@link #getLockInfo} null si el hilo no espera nada.
 */
public class ThreadInfo {

    private final long threadId;

    private final String threadName;

    private final Thread.State threadState;

    private final long blockedTime;

    private final long blockedCount;

    private final long waitedTime;

    private final long waitedCount;

    private final LockInfo lock;

    private final String lockName;

    private final long lockOwnerId;

    private final String lockOwnerName;

    private final boolean inNative;

    private final boolean suspended;

    private final boolean daemon;

    private final int priority;

    private final StackTraceElement[] stackTrace;

    private final MonitorInfo[] lockedMonitors;

    private final LockInfo[] lockedSynchronizers;

    /**
     * El unico constructor, de acceso de paquete.
     *
     * <p>Lo usan {@link #from} y la implementacion de {@link ThreadMXBean} de esta biblioteca. Ver la
     * nota de la clase sobre por que no es publico.
     */
    ThreadInfo(long threadId, String threadName, Thread.State threadState, long blockedTime,
               long blockedCount, long waitedTime, long waitedCount, LockInfo lock,
               String lockName, long lockOwnerId, String lockOwnerName, boolean inNative,
               boolean suspended, boolean daemon, int priority, StackTraceElement[] stackTrace,
               MonitorInfo[] lockedMonitors, LockInfo[] lockedSynchronizers) {
        this.threadId = threadId;
        this.threadName = threadName;
        this.threadState = threadState;
        this.blockedTime = blockedTime;
        this.blockedCount = blockedCount;
        this.waitedTime = waitedTime;
        this.waitedCount = waitedCount;
        this.lock = lock;
        this.lockName = lockName;
        this.lockOwnerId = lockOwnerId;
        this.lockOwnerName = lockOwnerName;
        this.inNative = inNative;
        this.suspended = suspended;
        this.daemon = daemon;
        this.priority = priority;
        if (stackTrace == null) {
            this.stackTrace = new StackTraceElement[0];
        } else {
            this.stackTrace = stackTrace;
        }
        if (lockedMonitors == null) {
            this.lockedMonitors = new MonitorInfo[0];
        } else {
            this.lockedMonitors = lockedMonitors;
        }
        if (lockedSynchronizers == null) {
            this.lockedSynchronizers = new LockInfo[0];
        } else {
            this.lockedSynchronizers = lockedSynchronizers;
        }
    }

    /** Su identificador. Ver {@link ThreadMXBean}: se reusan. */
    public long getThreadId() {
        return this.threadId;
    }

    /** Su nombre. */
    public String getThreadName() {
        return this.threadName;
    }

    /** En que estado estaba. */
    public Thread.State getThreadState() {
        return this.threadState;
    }

    /** Milisegundos bloqueado, o -1. Ver la nota de la clase. */
    public long getBlockedTime() {
        return this.blockedTime;
    }

    /** Cuantas veces se bloqueo. */
    public long getBlockedCount() {
        return this.blockedCount;
    }

    /** Milisegundos esperando, o -1. */
    public long getWaitedTime() {
        return this.waitedTime;
    }

    /** Cuantas veces espero. */
    public long getWaitedCount() {
        return this.waitedCount;
    }

    /** Que candado esperaba, o null. */
    public LockInfo getLockInfo() {
        return this.lock;
    }

    /** Ese candado como texto, o null. */
    public String getLockName() {
        return this.lockName;
    }

    /** Quien lo tenia, o -1. */
    public long getLockOwnerId() {
        return this.lockOwnerId;
    }

    /** El nombre de quien lo tenia, o null. */
    public String getLockOwnerName() {
        return this.lockOwnerName;
    }

    /** La pila, o un arreglo vacio si no se pidio. */
    public StackTraceElement[] getStackTrace() {
        return this.stackTrace;
    }

    /**
     * Si estaba suspendido.
     *
     * <p>Solo puede ser true por {@code Thread.suspend()}, que quedo obsoleto y ya no hace nada.
     */
    public boolean isSuspended() {
        return this.suspended;
    }

    /** Si estaba ejecutando codigo nativo. */
    public boolean isInNative() {
        return this.inNative;
    }

    /** Si es un hilo demonio. */
    public boolean isDaemon() {
        return this.daemon;
    }

    /** Su prioridad. */
    public int getPriority() {
        return this.priority;
    }

    /** Los monitores que tenia tomados, o un arreglo vacio. */
    public MonitorInfo[] getLockedMonitors() {
        return this.lockedMonitors;
    }

    /** Los candados de {@code java.util.concurrent} que tenia tomados, o un arreglo vacio. */
    public LockInfo[] getLockedSynchronizers() {
        return this.lockedSynchronizers;
    }

    /** Cuantos marcos como maximo imprime {@link #toString}; el resto sale como puntos suspensivos. */
    private static final int MAX_FRAMES = 8;

    /**
     * El encabezado del hilo y hasta ocho marcos de pila.
     *
     * <p>Corta a proposito: un volcado de cien hilos con la pila entera de cada uno es ilegible. Para
     * verla completa esta {@link #getStackTrace}.
     *
     * <p>Anota ademas, en el marco que corresponde, el candado que el hilo espera y los que tiene
     * tomados. Esa correlacion entre marco y candado es lo que hace legible un volcado.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('"').append(this.threadName).append('"');
        if (this.daemon) {
            sb.append(" daemon");
        }
        sb.append(" prio=").append(this.priority);
        sb.append(" Id=").append(this.threadId);
        sb.append(' ').append(this.threadState);
        if (this.lockName != null) {
            sb.append(" on ").append(this.lockName);
        }
        if (this.lockOwnerName != null) {
            sb.append(" owned by \"").append(this.lockOwnerName)
                .append("\" Id=").append(this.lockOwnerId);
        }
        if (this.suspended) {
            sb.append(" (suspended)");
        }
        if (this.inNative) {
            sb.append(" (in native)");
        }
        sb.append('\n');
        int i = 0;
        while (i < this.stackTrace.length && i < MAX_FRAMES) {
            sb.append("\tat ").append(this.stackTrace[i].toString()).append('\n');
            if (i == 0 && this.lock != null) {
                if (this.threadState == Thread.State.BLOCKED) {
                    sb.append("\t-  blocked on ").append(this.lock).append('\n');
                } else if (this.threadState == Thread.State.WAITING
                    || this.threadState == Thread.State.TIMED_WAITING) {
                    sb.append("\t-  waiting on ").append(this.lock).append('\n');
                }
            }
            int j = 0;
            while (j < this.lockedMonitors.length) {
                if (this.lockedMonitors[j].getLockedStackDepth() == i) {
                    sb.append("\t-  locked ").append(this.lockedMonitors[j]).append('\n');
                }
                j = j + 1;
            }
            i = i + 1;
        }
        if (i < this.stackTrace.length) {
            sb.append("\t...").append('\n');
        }
        if (this.lockedSynchronizers.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = ")
                .append(this.lockedSynchronizers.length).append('\n');
            int k = 0;
            while (k < this.lockedSynchronizers.length) {
                sb.append("\t- ").append(this.lockedSynchronizers[k]).append('\n');
                k = k + 1;
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    /**
     * Lo mismo, leido de un {@link CompositeData}.
     *
     * <p>Es como llega de otra maquina virtual. Los items de candados tomados aparecieron despues y se
     * leen si estan.
     *
     * @return el objeto, o null si el dato es null
     * @throws IllegalArgumentException si el dato no describe un {@code ThreadInfo}
     */
    public static ThreadInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        final String type = "ThreadInfo";
        String stateName = CompositeItems.string(cd, "threadState", type);
        Thread.State state = null;
        if (stateName != null) {
            state = Thread.State.valueOf(stateName);
        }
        return new ThreadInfo(
            CompositeItems.longValue(cd, "threadId", type),
            CompositeItems.string(cd, "threadName", type),
            state,
            CompositeItems.longValue(cd, "blockedTime", type),
            CompositeItems.longValue(cd, "blockedCount", type),
            CompositeItems.longValue(cd, "waitedTime", type),
            CompositeItems.longValue(cd, "waitedCount", type),
            lockOf(cd),
            CompositeItems.string(cd, "lockName", type),
            CompositeItems.longValue(cd, "lockOwnerId", type),
            CompositeItems.string(cd, "lockOwnerName", type),
            CompositeItems.bool(cd, "inNative", type),
            CompositeItems.bool(cd, "suspended", type),
            boolOrFalse(cd, "daemon"),
            intOrDefault(cd, "priority", Thread.NORM_PRIORITY),
            framesOf(cd),
            monitorsOf(cd),
            synchronizersOf(cd));
    }

    /** El candado esperado; el item existe desde Java 6 y puede faltar. */
    private static LockInfo lockOf(CompositeData cd) {
        Object v = CompositeItems.optional(cd, "lockInfo");
        if (v instanceof CompositeData) {
            return LockInfo.from((CompositeData) v);
        }
        return null;
    }

    /** Un booleano que puede faltar. */
    private static boolean boolOrFalse(CompositeData cd, String name) {
        Object v = CompositeItems.optional(cd, name);
        return v instanceof Boolean && ((Boolean) v).booleanValue();
    }

    /** Un entero que puede faltar. */
    private static int intOrDefault(CompositeData cd, String name, int fallback) {
        Object v = CompositeItems.optional(cd, name);
        if (v instanceof Integer) {
            return ((Integer) v).intValue();
        }
        return fallback;
    }

    /** La pila. */
    private static StackTraceElement[] framesOf(CompositeData cd) {
        Object v = CompositeItems.optional(cd, "stackTrace");
        if (!(v instanceof CompositeData[])) {
            return new StackTraceElement[0];
        }
        CompositeData[] raw = (CompositeData[]) v;
        StackTraceElement[] out = new StackTraceElement[raw.length];
        int i = 0;
        while (i < raw.length) {
            out[i] = StackTraceElements.from(raw[i]);
            i = i + 1;
        }
        return out;
    }

    /** Los monitores tomados. */
    private static MonitorInfo[] monitorsOf(CompositeData cd) {
        Object v = CompositeItems.optional(cd, "lockedMonitors");
        if (!(v instanceof CompositeData[])) {
            return new MonitorInfo[0];
        }
        CompositeData[] raw = (CompositeData[]) v;
        MonitorInfo[] out = new MonitorInfo[raw.length];
        int i = 0;
        while (i < raw.length) {
            out[i] = MonitorInfo.from(raw[i]);
            i = i + 1;
        }
        return out;
    }

    /** Los candados de {@code java.util.concurrent} tomados. */
    private static LockInfo[] synchronizersOf(CompositeData cd) {
        Object v = CompositeItems.optional(cd, "lockedSynchronizers");
        if (!(v instanceof CompositeData[])) {
            return new LockInfo[0];
        }
        CompositeData[] raw = (CompositeData[]) v;
        LockInfo[] out = new LockInfo[raw.length];
        int i = 0;
        while (i < raw.length) {
            out[i] = LockInfo.from(raw[i]);
            i = i + 1;
        }
        return out;
    }
}
