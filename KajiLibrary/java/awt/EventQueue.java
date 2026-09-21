package java.awt;

import java.awt.event.InvocationEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;

/**
 * The queue every AWT event goes through, and the thread that serves them.
 *
 * <p>It is the heart of the strictest rule of the graphical interface: **everything that touches
 * the screen runs on a single thread**. It is not a technical limitation but the only way for the
 * state of the interface to be consistent without putting a lock on every component.
 *
 * <p>From there come {@link #invokeLater} and {@link #invokeAndWait}, which are the legal way in
 * from another thread: they queue work for the event thread to run. The difference between the two
 * is whether the caller waits, and that wait is exactly where the classic deadlock is born —calling
 * `invokeAndWait` **from** the event thread is waiting for oneself, and that is why it is forbidden
 * explicitly.
 *
 * <p>{@link #push} and {@link #pop} allow putting a queue of one's own above the current one. It is
 * what a modal dialog uses: it stacks a queue that filters what arrives until the dialog closes.
 *
 * <p><strong>This queue really works.</strong> Serving events needs no windows, only a thread, so
 * here there is a real one: the events that get queued are dispatched, `invokeLater` runs what it
 * is given and `invokeAndWait` waits for it to finish. What there is not is anyone to **produce**
 * keyboard or mouse events, because for that a windowing system is needed.
 */
public class EventQueue {

    private final Deque<AWTEvent> queue = new ArrayDeque<AWTEvent>();
    private EventQueue nextQueue;
    private EventQueue previousQueue;
    private Thread dispatchThread;
    private volatile DispatcherAccess.Dispatcher foreign;
    private volatile boolean stopped;

    private static AWTEvent currentEvent;
    private static long mostRecentEventTime = System.currentTimeMillis();
    private static final Object STATIC_LOCK = new Object();

    /** A new queue, with its thread not started yet. */
    public EventQueue() {
    }

    /** The queue that actually serves: the last one stacked. */
    private EventQueue topmost() {
        EventQueue q = this;
        while (q.nextQueue != null) {
            q = q.nextQueue;
        }
        return q;
    }

    /**
     * Queues an event.
     *
     * <p>It starts the dispatch thread the first time it is needed: a queue nobody uses should not
     * cost a thread.
     *
     * @throws NullPointerException if the event is `null`
     */
    public void postEvent(AWTEvent theEvent) {
        if (theEvent == null) {
            throw new NullPointerException("theEvent");
        }
        EventQueue q = this.topmost();
        synchronized (q) {
            q.queue.addLast(theEvent);
            q.startDispatchThread();
            q.notifyAll();
        }
    }

    /** Starts the dispatch thread if it is not up yet. */
    private void startDispatchThread() {
        if (this.dispatchThread != null) {
            return;
        }
        Thread t = new Thread(new Pump(), "AWT-EventQueue");
        t.setDaemon(true);
        this.dispatchThread = t;
        t.start();
    }

    /** The loop that takes events out and dispatches them. */
    private final class Pump implements Runnable {

        public void run() {
            while (!EventQueue.this.stopped) {
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
                    // An event that throws cannot kill the dispatch thread: the whole interface
                    // would stop responding because of a single listener's error.
                    System.err.println("Exception occurred during event dispatching:");
                    re.printStackTrace();
                }
            }
        }
    }

    /**
     * Takes the next event out, waiting if there is none.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public AWTEvent getNextEvent() throws InterruptedException {
        EventQueue q = this.topmost();
        synchronized (q) {
            while (q.queue.isEmpty()) {
                q.wait();
            }
            return q.queue.removeFirst();
        }
    }

    /**
     * Looks at the next event without taking it out.
     *
     * @return the event, or `null` if the queue is empty
     */
    public AWTEvent peekEvent() {
        EventQueue q = this.topmost();
        synchronized (q) {
            return q.queue.peekFirst();
        }
    }

    /**
     * Looks at the first event with that identifier without taking it out.
     *
     * @return the event, or `null` if there is none of those
     */
    public AWTEvent peekEvent(int id) {
        EventQueue q = this.topmost();
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
     * Dispatches an event to whoever it belongs to.
     *
     * <p>An event that serves itself —an {@link ActiveEvent}— is dispatched to itself; the rest go
     * to their source. Overriding it is the way to see everything that goes through the queue.
     *
     * @throws NullPointerException if the event is `null`
     */
    protected void dispatchEvent(AWTEvent event) {
        DispatcherAccess.Dispatcher d = this.foreign;
        if (d != null && !this.isDispatchThreadImpl()) {
            // Already on the other toolkit's thread: dispatch straight away. Scheduling it would be
            // waiting for a thread that is this one, and that does not end.
            d.scheduleDispatch(new Dispatch(this, event));
            return;
        }
        this.dispatchHere(event);
    }

    /** The ordinary dispatch, the one that runs when there is nobody foreign to hand it to. */
    private void dispatchHere(AWTEvent event) {
        Object src = event.getSource();
        if (event instanceof ActiveEvent) {
            ((ActiveEvent) event).dispatch();
        } else if (src instanceof Component) {
            ((Component) src).dispatchEvent(event);
        } else if (src instanceof MenuComponent) {
            ((MenuComponent) src).dispatchEvent(event);
        }
    }

    /** One pending dispatch, to hand to the foreign dispatcher. */
    private static final class Dispatch implements Runnable {

        private final EventQueue queue;
        private final AWTEvent event;

        Dispatch(EventQueue queue, AWTEvent event) {
            this.queue = queue;
            this.event = event;
        }

        public void run() {
            this.queue.dispatchHere(this.event);
        }
    }

    /**
     * Whether the current thread is the one serving this queue.
     *
     * <p>When there is a foreign dispatcher the answer is its own: the dispatch thread became that
     * toolkit's. And the question always goes to the topmost queue, which is the one serving.
     */
    final boolean isDispatchThreadImpl() {
        EventQueue q = this.topmost();
        DispatcherAccess.Dispatcher d = q.foreign;
        if (d != null) {
            return d.isDispatchThread();
        }
        return Thread.currentThread() == q.dispatchThread;
    }

    /** Stores the foreign dispatcher in the topmost queue, which is the one serving. */
    final void installDispatcher(DispatcherAccess.Dispatcher d) {
        this.topmost().foreign = d;
    }

    /**
     * When the last input event happened.
     *
     * <p>It serves to detect user inactivity: the difference with the current time is how long it
     * has been since they touched anything.
     */
    public static long getMostRecentEventTime() {
        synchronized (STATIC_LOCK) {
            return mostRecentEventTime;
        }
    }

    /**
     * The event being dispatched right now.
     *
     * @return the event, or `null` if none is being dispatched
     */
    public static AWTEvent getCurrentEvent() {
        synchronized (STATIC_LOCK) {
            return currentEvent;
        }
    }

    /** Notes which event is being dispatched and when. */
    static void setCurrentEventAndMostRecentTime(AWTEvent e) {
        synchronized (STATIC_LOCK) {
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
     * Puts another queue above this one.
     *
     * <p>The events still left in this one are passed to the new one: if they were lost, a modal
     * dialog would make the pending repaints disappear as it opened.
     *
     * @throws NullPointerException if the queue is `null`
     * @throws RuntimeException if this queue gave up dispatching to another toolkit
     */
    public void push(EventQueue newEventQueue) {
        if (newEventQueue == null) {
            throw new NullPointerException("newEventQueue");
        }
        EventQueue q = this.topmost();
        synchronized (q) {
            if (q.foreign != null) {
                // Stacking another queue on top would cover the foreign dispatcher without taking
                // it out: events would go to the new one and the other toolkit would stop seeing
                // anything.
                throw new RuntimeException("push() to queue with fwDispatcher");
            }
            while (!q.queue.isEmpty()) {
                newEventQueue.postEvent(q.queue.removeFirst());
            }
            q.nextQueue = newEventQueue;
            newEventQueue.previousQueue = q;
        }
    }

    /**
     * Takes this queue off the stack and gives whatever is left in it back to the one below.
     *
     * @throws EmptyStackException if this queue is not stacked on top of another
     */
    protected void pop() throws EmptyStackException {
        EventQueue prev = this.previousQueue;
        if (prev == null) {
            throw new EmptyStackException();
        }
        synchronized (this) {
            while (!this.queue.isEmpty()) {
                prev.postEvent(this.queue.removeFirst());
            }
            this.stopped = true;
            this.notifyAll();
        }
        prev.nextQueue = null;
        this.previousQueue = null;
    }

    /**
     * A secondary loop, to wait without blocking the event thread.
     *
     * @return the loop, or `null` if it cannot be created
     */
    public SecondaryLoop createSecondaryLoop() {
        DispatcherAccess.Dispatcher d = this.topmost().foreign;
        if (d != null) {
            return d.secondaryLoop();
        }
        return new SimpleSecondaryLoop();
    }

    /**
     * A secondary loop that really waits.
     *
     * <p>The event thread keeps serving on its side; this only blocks whoever calls
     * {@link SecondaryLoop#enter}, which is what a modal dialog needs.
     */
    private static final class SimpleSecondaryLoop implements SecondaryLoop {

        private boolean running;

        public boolean enter() {
            synchronized (this) {
                if (this.running) {
                    return false;
                }
                this.running = true;
                while (this.running) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        this.running = false;
                        return false;
                    }
                }
                return true;
            }
        }

        public boolean exit() {
            synchronized (this) {
                if (!this.running) {
                    return false;
                }
                this.running = false;
                this.notifyAll();
                return true;
            }
        }
    }

    /** Whether the current thread is the one that serves the events. */
    public static boolean isDispatchThread() {
        return Thread.currentThread().getName().startsWith("AWT-EventQueue");
    }

    /**
     * Queues work for the event thread and returns right away.
     *
     * @throws NullPointerException if the task is `null`
     */
    public static void invokeLater(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable");
        }
        Toolkit.getDefaultToolkit().getSystemEventQueue()
                .postEvent(new InvocationEvent(Toolkit.getDefaultToolkit(), runnable));
    }

    /**
     * Queues work for the event thread and **waits** for it to finish.
     *
     * <p>Calling it from the event thread would be waiting for oneself, and that is why it is
     * forbidden: it is not an arbitrary restriction, it is a sure deadlock.
     *
     * @throws NullPointerException if the task is `null`
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws InvocationTargetException if the task threw something
     * @throws Error if it is called from the event thread
     */
    public static void invokeAndWait(Runnable runnable)
            throws InterruptedException, InvocationTargetException {
        if (runnable == null) {
            throw new NullPointerException("runnable");
        }
        if (isDispatchThread()) {
            throw new Error("Cannot call invokeAndWait from the event dispatcher thread");
        }
        Object lock = new Object();
        InvocationEvent event =
                new InvocationEvent(Toolkit.getDefaultToolkit(), runnable, lock, true);
        synchronized (lock) {
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(event);
            while (!event.isDispatched()) {
                lock.wait();
            }
        }
        Throwable t = event.getThrowable();
        if (t != null) {
            throw new InvocationTargetException(t);
        }
    }
}
