package javax.swing.text;

import java.util.Vector;

/**
 * La cola de trabajos de maquetado que corren fuera del hilo de eventos.
 *
 * <p>Existe para las vistas que se calculan solas mientras el documento se muestra —un documento
 * largo que se va midiendo de a poco, {@code AsyncBoxView}—: la vista encola el trabajo y sigue,
 * y un hilo aparte lo hace.
 *
 * <p>En esta VM la cola guarda los trabajos y {@link #waitForWork} los entrega, pero
 * <strong>nadie los saca</strong>: no hay hilo de maquetado. Una vista que dependa de esta cola
 * para medirse no se va a medir sola; las que estan hoy en la biblioteca miden en el momento.
 */
public class LayoutQueue {

    private Vector<Runnable> tasks = new Vector<Runnable>();

    private static LayoutQueue defaultQueue;

    public LayoutQueue() {
    }

    /** La cola compartida. */
    public static LayoutQueue getDefaultQueue() {
        if (defaultQueue == null) {
            defaultQueue = new LayoutQueue();
        }
        return defaultQueue;
    }

    /** Cambia la cola compartida; sirve para poner una que corra los trabajos de otra forma. */
    public static void setDefaultQueue(LayoutQueue q) {
        defaultQueue = q;
    }

    /** Encola un trabajo y avisa a quien este esperando. */
    public synchronized void addTask(Runnable task) {
        if (task != null) {
            tasks.addElement(task);
            notifyAll();
        }
    }

    /**
     * Espera a que haya trabajo y devuelve el primero.
     *
     * <p>Bloquea al que llama; ver la nota de la clase sobre por que en esta VM no lo llama nadie.
     */
    protected synchronized Runnable waitForWork() {
        while (tasks.size() == 0) {
            try {
                wait();
            } catch (InterruptedException ie) {
                return null;
            }
        }
        Runnable work = tasks.firstElement();
        tasks.removeElementAt(0);
        return work;
    }
}
