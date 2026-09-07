package javax.swing;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.EventListenerList;

/**
 * Dispara un {@link ActionEvent} cada tantos milisegundos, en el hilo de eventos.
 *
 * <h2>Por que en el hilo de eventos</h2>
 *
 * <p>Un reloj que avisa desde su propio hilo obliga a quien escucha a sincronizar todo lo que
 * toca. Este encola el aviso en la cola de eventos, asi que el escucha corre donde corre el resto
 * de la interfaz y puede tocar componentes sin cuidados. Es la diferencia con
 * {@code java.util.Timer}, y la razon de que exista este.
 *
 * <h2>Un hilo por reloj</h2>
 *
 * <p>El JDK tiene una cola compartida con un solo hilo para todos los relojes. Aca cada reloj
 * andando tiene el suyo, que duerme y encola. Se nota si alguien crea cientos; para los pocos que
 * usa una interfaz —el parpadeo de un cursor, una animacion— da igual, y el codigo es una decima
 * parte.
 *
 * <p>{@link #setCoalesce} junta los avisos atrasados en uno: si la interfaz estuvo ocupada mas de
 * un periodo, no tiene sentido despachar cinco avisos seguidos de un parpadeo.
 */
public class Timer implements Serializable {

    protected EventListenerList listenerList = new EventListenerList();

    private transient volatile boolean corriendo;
    private transient Thread hilo;
    private transient volatile boolean pendiente;

    private int initialDelay;
    private int delay;
    private boolean repeats = true;
    private boolean coalesce = true;
    private String actionCommand;

    private static boolean logTimers;

    /** Un reloj de ese periodo, con ese escucha si no es {@code null}. */
    public Timer(int delay, ActionListener listener) {
        this.delay = delay;
        this.initialDelay = delay;
        if (listener != null) {
            addActionListener(listener);
        }
    }

    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    /** Avisa a los escuchas; corre en el hilo de eventos. */
    protected void fireActionPerformed(ActionEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /** Enciende el registro de relojes; en esta VM no escribe nada. */
    public static void setLogTimers(boolean flag) {
        logTimers = flag;
    }

    public static boolean getLogTimers() {
        return logTimers;
    }

    public void setDelay(int delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Invalid delay: " + delay);
        }
        this.delay = delay;
    }

    public int getDelay() {
        return delay;
    }

    /** Cuanto espera antes del primer aviso; por omision, lo mismo que entre avisos. */
    public void setInitialDelay(int initialDelay) {
        if (initialDelay < 0) {
            throw new IllegalArgumentException("Invalid initial delay: " + initialDelay);
        }
        this.initialDelay = initialDelay;
    }

    public int getInitialDelay() {
        return initialDelay;
    }

    /** Si avisa una sola vez o para siempre. */
    public void setRepeats(boolean flag) {
        repeats = flag;
    }

    public boolean isRepeats() {
        return repeats;
    }

    /** Si junta los avisos atrasados en uno; ver la nota de la clase. */
    public void setCoalesce(boolean flag) {
        coalesce = flag;
    }

    public boolean isCoalesce() {
        return coalesce;
    }

    public void setActionCommand(String command) {
        this.actionCommand = command;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    /** Arranca; si ya estaba andando no hace nada. */
    public void start() {
        if (corriendo) {
            return;
        }
        corriendo = true;
        pendiente = false;
        hilo = new Thread(new Latido(this));
        hilo.setDaemon(true);
        hilo.start();
    }

    public boolean isRunning() {
        return corriendo;
    }

    /** Para; un aviso ya encolado puede llegar igual. */
    public void stop() {
        corriendo = false;
        Thread t = hilo;
        hilo = null;
        if (t != null) {
            t.interrupt();
        }
    }

    /** Para y arranca de nuevo, con la espera inicial otra vez. */
    public void restart() {
        stop();
        start();
    }

    /** Descarta el aviso encolado que todavia no se despacho. */
    void cancelEvent() {
        pendiente = false;
    }

    /** Encola un aviso, salvo que ya haya uno esperando y se junten. */
    void post() {
        if (pendiente && coalesce) {
            return;
        }
        pendiente = true;
        EventQueue.invokeLater(new Aviso(this));
    }

    /** El hilo que duerme y encola; uno por reloj andando. */
    private static class Latido implements Runnable {

        private final Timer reloj;

        Latido(Timer reloj) {
            this.reloj = reloj;
        }

        public void run() {
            try {
                Thread.sleep(reloj.getInitialDelay());
                while (reloj.isRunning()) {
                    reloj.post();
                    if (!reloj.isRepeats()) {
                        reloj.corriendo = false;
                        return;
                    }
                    Thread.sleep(Math.max(1, reloj.getDelay()));
                }
            } catch (InterruptedException e) {
                // Lo pararon: no es un error.
            }
        }
    }

    /** El aviso que corre en el hilo de eventos. */
    private static class Aviso implements Runnable {

        private final Timer reloj;

        Aviso(Timer reloj) {
            this.reloj = reloj;
        }

        public void run() {
            reloj.pendiente = false;
            reloj.fireActionPerformed(new ActionEvent(reloj, ActionEvent.ACTION_PERFORMED,
                    reloj.getActionCommand(), System.currentTimeMillis(), 0));
        }
    }
}
