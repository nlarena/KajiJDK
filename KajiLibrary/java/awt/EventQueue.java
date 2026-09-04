package java.awt;

import java.awt.event.InvocationEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;

/**
 * La cola por la que pasan todos los eventos de AWT, y el hilo que los atiende.
 *
 * <p>Es el corazón de la regla más estricta de la interfaz gráfica: **todo lo que toca la pantalla
 * corre en un solo hilo**. No es una limitación técnica sino la única forma de que el estado de la
 * interfaz sea consistente sin poner un candado en cada componente.
 *
 * <p>De ahí salen {@link #invokeLater} e {@link #invokeAndWait}, que son la puerta de entrada legal
 * desde otro hilo: encolan trabajo para que lo corra el hilo de eventos. La diferencia entre las dos
 * es si el que llama espera, y esa espera es exactamente donde nace el abrazo mortal clásico —
 * llamar a `invokeAndWait` **desde** el hilo de eventos es esperarse a uno mismo, y por eso está
 * prohibido explícitamente.
 *
 * <p>{@link #push} y {@link #pop} permiten meter una cola propia por encima de la que hay. Es lo que
 * usa un diálogo modal: apila una cola que filtra lo que llega hasta que el diálogo se cierra.
 *
 * <p><strong>Esta cola funciona de verdad.</strong> Atender eventos no necesita ventanas, sólo un
 * hilo, así que acá hay uno real: los eventos que se encolan se despachan, `invokeLater` corre lo
 * que se le da e `invokeAndWait` espera a que termine. Lo que no hay es quién **produzca** eventos de
 * teclado o de ratón, porque para eso sí hace falta un sistema de ventanas.
 */
public class EventQueue {

    private final Deque<AWTEvent> queue = new ArrayDeque<AWTEvent>();
    private EventQueue nextQueue;
    private EventQueue previousQueue;
    private Thread dispatchThread;
    private volatile boolean detiene;

    private static AWTEvent currentEvent;
    private static long mostRecentEventTime = System.currentTimeMillis();
    private static final Object ESTATICO = new Object();

    /** Una cola nueva, con su hilo todavía sin arrancar. */
    public EventQueue() {
    }

    /** La cola que efectivamente atiende: la última que se apiló. */
    private EventQueue laDeArriba() {
        EventQueue q = this;
        while (q.nextQueue != null) {
            q = q.nextQueue;
        }
        return q;
    }

    /**
     * Encola un evento.
     *
     * <p>Arranca el hilo de despacho la primera vez que hace falta: una cola que nadie usa no
     * debería costar un hilo.
     *
     * @throws NullPointerException si el evento es `null`
     */
    public void postEvent(AWTEvent theEvent) {
        if (theEvent == null) {
            throw new NullPointerException("theEvent");
        }
        EventQueue q = this.laDeArriba();
        synchronized (q) {
            q.queue.addLast(theEvent);
            q.arrancarHilo();
            q.notifyAll();
        }
    }

    /** Arranca el hilo de despacho si todavía no está. */
    private void arrancarHilo() {
        if (this.dispatchThread != null) {
            return;
        }
        Thread t = new Thread(new Bombeo(), "AWT-EventQueue");
        t.setDaemon(true);
        this.dispatchThread = t;
        t.start();
    }

    /** El bucle que saca eventos y los despacha. */
    private final class Bombeo implements Runnable {

        public void run() {
            while (!EventQueue.this.detiene) {
                AWTEvent e;
                try {
                    e = EventQueue.this.getNextEvent();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                setCurrentEventAndMostRecentTime(e);
                try {
                    EventQueue.this.dispatchEvent(e);
                } catch (RuntimeException re) {
                    // Un evento que tira no puede matar al hilo de despacho: la interfaz entera
                    // dejaria de responder por un error de un solo oyente.
                    System.err.println("Exception occurred during event dispatching:");
                    re.printStackTrace();
                }
            }
        }
    }

    /**
     * Saca el evento siguiente, esperando si no hay ninguno.
     *
     * @throws InterruptedException si se interrumpe el hilo mientras espera
     */
    public AWTEvent getNextEvent() throws InterruptedException {
        EventQueue q = this.laDeArriba();
        synchronized (q) {
            while (q.queue.isEmpty()) {
                q.wait();
            }
            return q.queue.removeFirst();
        }
    }

    /**
     * Mira el evento siguiente sin sacarlo.
     *
     * @return el evento, o `null` si la cola está vacía
     */
    public AWTEvent peekEvent() {
        EventQueue q = this.laDeArriba();
        synchronized (q) {
            return q.queue.peekFirst();
        }
    }

    /**
     * Mira el primer evento de ese identificador sin sacarlo.
     *
     * @return el evento, o `null` si no hay ninguno de ésos
     */
    public AWTEvent peekEvent(int id) {
        EventQueue q = this.laDeArriba();
        synchronized (q) {
            java.util.Iterator<AWTEvent> it = q.queue.iterator();
            while (it.hasNext()) {
                AWTEvent e = it.next();
                if (e.getID() == id) {
                    return e;
                }
            }
            return null;
        }
    }

    /**
     * Despacha un evento a quien corresponda.
     *
     * <p>Un evento que se atiende solo —un {@link ActiveEvent}— se despacha a sí mismo; el resto va
     * a su fuente. Redefinirlo es la forma de ver todo lo que pasa por la cola.
     *
     * @throws NullPointerException si el evento es `null`
     */
    protected void dispatchEvent(AWTEvent event) {
        Object src = event.getSource();
        if (event instanceof ActiveEvent) {
            ((ActiveEvent) event).dispatch();
        } else if (src instanceof Component) {
            ((Component) src).dispatchEvent(event);
        } else if (src instanceof MenuComponent) {
            ((MenuComponent) src).dispatchEvent(event);
        }
    }

    /**
     * Cuándo pasó el último evento de entrada.
     *
     * <p>Sirve para detectar inactividad del usuario: la diferencia con la hora actual es cuánto
     * hace que no toca nada.
     */
    public static long getMostRecentEventTime() {
        synchronized (ESTATICO) {
            return mostRecentEventTime;
        }
    }

    /**
     * El evento que se está despachando ahora.
     *
     * @return el evento, o `null` si no se está despachando ninguno
     */
    public static AWTEvent getCurrentEvent() {
        synchronized (ESTATICO) {
            return currentEvent;
        }
    }

    /** Anota qué evento se está despachando y cuándo. */
    static void setCurrentEventAndMostRecentTime(AWTEvent e) {
        synchronized (ESTATICO) {
            currentEvent = e;
            if (e instanceof java.awt.event.InputEvent) {
                mostRecentEventTime = ((java.awt.event.InputEvent) e).getWhen();
            } else if (e instanceof InvocationEvent) {
                mostRecentEventTime = ((InvocationEvent) e).getWhen();
            } else {
                mostRecentEventTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Mete otra cola por encima de ésta.
     *
     * <p>Los eventos que quedaban en ésta se pasan a la nueva: si se perdieran, un diálogo modal
     * haría desaparecer los repintados pendientes al abrirse.
     *
     * @throws NullPointerException si la cola es `null`
     */
    public void push(EventQueue newEventQueue) {
        if (newEventQueue == null) {
            throw new NullPointerException("newEventQueue");
        }
        EventQueue q = this.laDeArriba();
        synchronized (q) {
            while (!q.queue.isEmpty()) {
                newEventQueue.postEvent(q.queue.removeFirst());
            }
            q.nextQueue = newEventQueue;
            newEventQueue.previousQueue = q;
        }
    }

    /**
     * Saca esta cola de la pila y devuelve lo que quede en ella a la de abajo.
     *
     * @throws EmptyStackException si esta cola no está apilada sobre otra
     */
    protected void pop() throws EmptyStackException {
        EventQueue anterior = this.previousQueue;
        if (anterior == null) {
            throw new EmptyStackException();
        }
        synchronized (this) {
            while (!this.queue.isEmpty()) {
                anterior.postEvent(this.queue.removeFirst());
            }
            this.detiene = true;
            this.notifyAll();
        }
        anterior.nextQueue = null;
        this.previousQueue = null;
    }

    /**
     * Un bucle secundario, para esperar sin bloquear el hilo de eventos.
     *
     * @return el bucle, o `null` si no se puede crear
     */
    public SecondaryLoop createSecondaryLoop() {
        return new BucleSecundario();
    }

    /**
     * Un bucle secundario que espera de verdad.
     *
     * <p>El hilo de eventos sigue atendiendo por su lado; esto sólo bloquea a quien llame
     * {@link SecondaryLoop#enter}, que es lo que un diálogo modal necesita.
     */
    private static final class BucleSecundario implements SecondaryLoop {

        private boolean corriendo;

        public boolean enter() {
            synchronized (this) {
                if (this.corriendo) {
                    return false;
                }
                this.corriendo = true;
                while (this.corriendo) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        this.corriendo = false;
                        return false;
                    }
                }
                return true;
            }
        }

        public boolean exit() {
            synchronized (this) {
                if (!this.corriendo) {
                    return false;
                }
                this.corriendo = false;
                this.notifyAll();
                return true;
            }
        }
    }

    /** Si el hilo actual es el que atiende los eventos. */
    public static boolean isDispatchThread() {
        return Thread.currentThread().getName().startsWith("AWT-EventQueue");
    }

    /**
     * Encola trabajo para el hilo de eventos y vuelve enseguida.
     *
     * @throws NullPointerException si la tarea es `null`
     */
    public static void invokeLater(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable");
        }
        Toolkit.getDefaultToolkit().getSystemEventQueue()
                .postEvent(new InvocationEvent(Toolkit.getDefaultToolkit(), runnable));
    }

    /**
     * Encola trabajo para el hilo de eventos y **espera** a que termine.
     *
     * <p>Llamarlo desde el hilo de eventos sería esperarse a uno mismo, y por eso está prohibido: no
     * es una restricción arbitraria, es un abrazo mortal seguro.
     *
     * @throws NullPointerException si la tarea es `null`
     * @throws InterruptedException si se interrumpe el hilo mientras espera
     * @throws InvocationTargetException si la tarea tiró algo
     * @throws Error si se lo llama desde el hilo de eventos
     */
    public static void invokeAndWait(Runnable runnable)
            throws InterruptedException, InvocationTargetException {
        if (runnable == null) {
            throw new NullPointerException("runnable");
        }
        if (isDispatchThread()) {
            throw new Error("Cannot call invokeAndWait from the event dispatcher thread");
        }
        Object candado = new Object();
        InvocationEvent event =
                new InvocationEvent(Toolkit.getDefaultToolkit(), runnable, candado, true);
        synchronized (candado) {
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(event);
            while (!event.isDispatched()) {
                candado.wait();
            }
        }
        Throwable t = event.getThrowable();
        if (t != null) {
            throw new InvocationTargetException(t);
        }
    }
}
