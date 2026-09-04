package java.awt.event;

import java.awt.AWTEvent;
import java.awt.ActiveEvent;

/**
 * Trabajo para correr en el hilo de eventos.
 *
 * <p>Es la pieza que resuelve la regla más estricta de AWT: **la interfaz sólo se toca desde el hilo
 * de eventos**. Un hilo que quiera cambiar algo de la pantalla no lo hace directamente; encola uno
 * de éstos con lo que hay que hacer, y el hilo de eventos lo saca a su turno y lo ejecuta.
 *
 * <p>Se atiende solo, sin oyentes: implementa {@link ActiveEvent}, así que la cola le llama
 * {@link #dispatch} y él corre lo suyo.
 *
 * <p>Las excepciones se pueden atrapar o dejar pasar. Atraparlas y guardarlas sirve para las
 * llamadas **sincrónicas**: el hilo que espera necesita enterarse de que la tarea falló, y una
 * excepción que se propague en el hilo de eventos no le llegaría nunca.
 */
public class InvocationEvent extends AWTEvent implements ActiveEvent {

    private static final long serialVersionUID = 436056344909459450L;

    /** El identificador de siempre. */
    public static final int INVOCATION_DEFAULT = 1200;

    /** El primer identificador de la familia. */
    public static final int INVOCATION_FIRST = 1200;

    /** El último identificador de la familia. */
    public static final int INVOCATION_LAST = 1200;

    /** Qué hay que hacer. */
    protected Runnable runnable;

    /** Sobre qué avisar cuando terminó, o `null`. */
    protected volatile Object notifier;

    /** Si hay que atrapar las excepciones en vez de dejarlas pasar. */
    protected boolean catchExceptions;

    private Runnable listener;
    private volatile boolean dispatched;
    private Throwable throwable;
    private final long when;

    /**
     * Con la tarea, dejando pasar las excepciones.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public InvocationEvent(Object source, Runnable runnable) {
        this(source, INVOCATION_DEFAULT, runnable, null, false);
    }

    /**
     * Con un objeto sobre el que avisar al terminar.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public InvocationEvent(Object source, Runnable runnable, Object notifier,
            boolean catchThrowables) {
        this(source, INVOCATION_DEFAULT, runnable, notifier, catchThrowables);
    }

    /**
     * Con una tarea que se corre al terminar.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public InvocationEvent(Object source, Runnable runnable, Runnable listener,
            boolean catchThrowables) {
        this(source, INVOCATION_DEFAULT, runnable, null, catchThrowables);
        this.listener = listener;
    }

    /**
     * El constructor general, para las subclases.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    protected InvocationEvent(Object source, int id, Runnable runnable, Object notifier,
            boolean catchThrowables) {
        super(source, id);
        this.runnable = runnable;
        this.notifier = notifier;
        this.catchExceptions = catchThrowables;
        this.when = System.currentTimeMillis();
    }

    /**
     * Corre la tarea y avisa que terminó.
     *
     * <p>El aviso va en un `finally`: si la tarea tira y las excepciones no se atrapan, el hilo que
     * esperaba tiene que despertarse igual, o queda colgado para siempre.
     */
    public void dispatch() {
        try {
            if (this.catchExceptions) {
                try {
                    this.runnable.run();
                } catch (Throwable t) {
                    this.throwable = t;
                }
            } else {
                this.runnable.run();
            }
        } finally {
            this.finishedDispatching();
        }
    }

    /** Marca que terminó y despierta a quien estuviera esperando. */
    private void finishedDispatching() {
        this.dispatched = true;
        Object n = this.notifier;
        if (n != null) {
            synchronized (n) {
                n.notifyAll();
            }
        }
        if (this.listener != null) {
            this.listener.run();
        }
    }

    /**
     * La excepción que tiró la tarea, si fue una `Exception`.
     *
     * @return la excepción, o `null` si no hubo o si fue un `Error`
     */
    public Exception getException() {
        if (this.catchExceptions && this.throwable instanceof Exception) {
            return (Exception) this.throwable;
        }
        return null;
    }

    /**
     * Lo que haya tirado la tarea.
     *
     * <p>A diferencia de {@link #getException}, incluye los `Error`.
     */
    public Throwable getThrowable() {
        if (this.catchExceptions) {
            return this.throwable;
        }
        return null;
    }

    /** Cuándo se encoló. */
    public long getWhen() {
        return this.when;
    }

    /** Si ya se ejecutó. */
    public boolean isDispatched() {
        return this.dispatched;
    }

    public String paramString() {
        String tipo = this.id == INVOCATION_DEFAULT ? "INVOCATION_DEFAULT" : "unknown type";
        return tipo + ",runnable=" + this.runnable + ",notifier=" + this.notifier
                + ",catchExceptions=" + this.catchExceptions + ",when=" + this.when;
    }
}
