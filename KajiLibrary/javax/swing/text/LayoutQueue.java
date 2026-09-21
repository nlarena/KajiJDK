package javax.swing.text;

import java.util.Vector;

/**
 * The queue of layout jobs that run off the event thread.
 *
 * <p>It exists for the views that compute themselves while the document is shown --a long
 * document that is measured bit by bit, {@code AsyncBoxView}--: the view queues the job and goes
 * on, and a separate thread does it.
 *
 * <p>In this VM the queue keeps the jobs and {@link #waitForWork} hands them out, but
 * <strong>nobody takes them</strong>: there is no layout thread. A view that depends on this
 * queue to measure itself will not measure itself; those that are in the library today measure
 * on the spot.
 */
public class LayoutQueue {

    private Vector<Runnable> tasks = new Vector<Runnable>();

    private static LayoutQueue defaultQueue;

    public LayoutQueue() {
    }

    /** The shared queue. */
    public static LayoutQueue getDefaultQueue() {
        if (defaultQueue == null) {
            defaultQueue = new LayoutQueue();
        }
        return defaultQueue;
    }

    /** It changes the shared queue; it serves for putting one that runs the jobs another way. */
    public static void setDefaultQueue(LayoutQueue q) {
        defaultQueue = q;
    }

    /** It queues a job and notifies whoever is waiting. */
    public synchronized void addTask(Runnable task) {
        if (task != null) {
            tasks.addElement(task);
            notifyAll();
        }
    }

    /**
     * It waits until there is work and returns the first one.
     *
     * <p>It blocks the caller; see the class note about why in this VM nobody calls it.
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
