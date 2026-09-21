package javax.swing;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.EventListenerList;

/**
 * It fires an {@link ActionEvent} every so many milliseconds, on the event thread.
 *
 * <h2>Why on the event thread</h2>
 *
 * <p>A timer that gives notice from its own thread forces whoever listens to synchronize
 * everything it touches. This one queues the notice on the event queue, so the listener runs
 * where the rest of the interface runs and may touch components without care. It is the
 * difference with {@code java.util.Timer}, and the reason this one exists.
 *
 * <h2>One thread per timer</h2>
 *
 * <p>The JDK has a shared queue with a single thread for every timer. Here each running timer
 * has its own, which sleeps and queues. It shows if somebody creates hundreds; for the few an
 * interface uses -- a caret's blink, an animation -- it makes no difference, and the code is a
 * tenth of the size.
 *
 * <p>{@link #setCoalesce} gathers the late notices into one: if the interface was busy for more
 * than a period, there is no point in dispatching five notices of a blink in a row.
 */
public class Timer implements Serializable {

    protected EventListenerList listenerList = new EventListenerList();

    private transient volatile boolean running;
    private transient Thread thread;
    private transient volatile boolean pending;

    private int initialDelay;
    private int delay;
    private boolean repeats = true;
    private boolean coalesce = true;
    private String actionCommand;

    private static boolean logTimers;

    /** A timer of that period, with that listener if it is not {@code null}. */
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

    /** It gives notice to the listeners; it runs on the event thread. */
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

    /** It switches the timer log on; on this VM it writes nothing. */
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

    /** How long it waits before the first notice; by default, the same as between notices. */
    public void setInitialDelay(int initialDelay) {
        if (initialDelay < 0) {
            throw new IllegalArgumentException("Invalid initial delay: " + initialDelay);
        }
        this.initialDelay = initialDelay;
    }

    public int getInitialDelay() {
        return initialDelay;
    }

    /** Whether it gives notice once or for ever. */
    public void setRepeats(boolean flag) {
        repeats = flag;
    }

    public boolean isRepeats() {
        return repeats;
    }

    /** Whether it gathers the late notices into one; see the class note. */
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

    /** It starts; if it was already running it does nothing. */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        pending = false;
        thread = new Thread(new Tick(this));
        thread.setDaemon(true);
        thread.start();
    }

    public boolean isRunning() {
        return running;
    }

    /** It stops; a notice already queued may arrive all the same. */
    public void stop() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            t.interrupt();
        }
    }

    /** It stops and starts again, with the initial wait once more. */
    public void restart() {
        stop();
        start();
    }

    /** It discards the queued notice that has not been dispatched yet. */
    void cancelEvent() {
        pending = false;
    }

    /** It queues a notice, unless there is already one waiting and they are gathered. */
    void post() {
        if (pending && coalesce) {
            return;
        }
        pending = true;
        EventQueue.invokeLater(new PostEvent(this));
    }

    /** The thread that sleeps and queues; one per running timer. */
    private static class Tick implements Runnable {

        private final Timer timer;

        Tick(Timer timer) {
            this.timer = timer;
        }

        public void run() {
            try {
                // The constructor takes a negative delay without checking (so does the JDK's), and
                // it means "fire now"; `Thread.sleep` would reject it (finding #296).
                Thread.sleep(Math.max(0, timer.getInitialDelay()));
                while (timer.isRunning()) {
                    timer.post();
                    if (!timer.isRepeats()) {
                        timer.running = false;
                        return;
                    }
                    Thread.sleep(Math.max(1, timer.getDelay()));
                }
            } catch (InterruptedException e) {
                // It was stopped: it is not an error.
            }
        }
    }

    /** The notice that runs on the event thread. */
    private static class PostEvent implements Runnable {

        private final Timer timer;

        PostEvent(Timer timer) {
            this.timer = timer;
        }

        public void run() {
            timer.pending = false;
            timer.fireActionPerformed(new ActionEvent(timer, ActionEvent.ACTION_PERFORMED,
                    timer.getActionCommand(), System.currentTimeMillis(), 0));
        }
    }
}
