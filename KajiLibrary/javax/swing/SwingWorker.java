package javax.swing;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A long task that runs outside the interface's thread and keeps telling how it is going.
 *
 * <p>It resolves the oldest problem of a graphical interface: the thread that draws is a single
 * one, and anything that takes a while -- reading a file, querying a database -- freezes it.
 * But moving the task to another thread is not enough, because **the components cannot be
 * touched from another thread**. Both halves are needed, and that is what the two type
 * parameters are:
 *
 * <ul>
 *   <li>`T` is the final result: {@link #doInBackground} returns it and {@link #get} picks it
 *       up.
 *   <li>`V` are the intermediate advances: {@link #publish} sends them from the background
 *       thread and {@link #process} receives them -- in the JDK, already on the interface's
 *       thread.
 * </ul>
 *
 * <p>And that is why {@link #done} exists separately from `doInBackground`: it is the hook for
 * touching the interface when it has finished.
 *
 * <p>A `SwingWorker` **is used only once**. {@link #execute} called twice runs nothing again:
 * the state goes from `PENDING` to `STARTED` to `DONE` and does not come back.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Everything that is concurrency is really done: the task runs in a pool, the result travels
 * through a {@link FutureTask}, {@link #cancel} and {@link #get} are the usual ones, and the
 * progress and the state are given notice of through {@link PropertyChangeSupport}.
 *
 * <p>What is **not** there is the jump to the interface's thread. In the JDK, `process`, `done`
 * and the property notices are executed on the EDT, and `publish` also gathers the advances so
 * as not to flood it. This library has no EDT, so `process` and `done` run **on the background
 * thread** and each `publish` arrives whole. It is documented in each method: whoever writes a
 * `SwingWorker` against this library has to know that it does not inherit the thread safety the
 * name promises.
 */
public abstract class SwingWorker<T, V> implements RunnableFuture<T> {

    /** How the work is going. */
    public enum StateValue {

        /** Created, not started yet. */
        PENDING,

        /** Running. */
        STARTED,

        /** Finished, cancelled or broken: in any case, it is not going to do anything else. */
        DONE
    }

    /** How many jobs may run at a time. The same number the JDK uses. */
    private static final int MAX_WORKER_THREADS = 10;

    private static ExecutorService executorService;

    private volatile int progress;
    private volatile StateValue state = StateValue.PENDING;
    private final FutureTask<T> future;
    private final PropertyChangeSupport propertyChangeSupport;

    /** A job that has not been started. */
    public SwingWorker() {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.future = new Task<T>(this, new Body<T>(this));
    }

    /**
     * The long work. The subclass writes it.
     *
     * <p>It runs on a background thread, so it **cannot touch components**. Whatever has to be
     * shown comes out through {@link #publish} or is returned and picked up in {@link #done}.
     *
     * @return the result
     * @throws Exception whatever fails; it comes out afterwards wrapped by {@link #get}
     */
    protected abstract T doInBackground() throws Exception;

    /** It runs the work on the current thread. The pool calls it; it is not called by hand. */
    public final void run() {
        this.future.run();
    }

    /**
     * It sends intermediate advances to {@link #process}.
     *
     * <p>It is called from {@link #doInBackground}. In the JDK the advances are gathered and
     * arrive in batches on the interface's thread; here they arrive one per call and on the same
     * thread. See the class note.
     */
    @SafeVarargs
    protected final void publish(V... chunks) {
        // The intermediate local is a detour around #285: `new ArrayList<V>(Arrays.asList(chunks))`
                // in a single expression does not compile because `V` is a variable of **the
                // class**. Naming the type is just what the inference did not deduce. Remove it
                // when #285 is closed.
        List<V> chunk = Arrays.asList(chunks);
        process(new ArrayList<V>(chunk));
    }

    /**
     * It receives {@link #publish}'s advances. The subclass overrides it.
     *
     * <p>In the JDK it runs on the interface's thread; here, on the background one. See the class
     * note.
     */
    protected void process(List<V> chunks) {
    }

    /**
     * It is called when {@link #doInBackground} has finished -- well, badly or cancelled.
     *
     * <p>It is where what touches the interface goes. In the JDK it runs on the interface's
     * thread; here, on the background one. See the class note.
     */
    protected void done() {
    }

    /**
     * It fixes the progress, between 0 and 100, and gives notice to the listeners of the
     * {@code "progress"} property.
     *
     * <p>If the value does not change no notice is given: a job that reports the same number a
     * thousand times has no reason to wake anybody up.
     *
     * @throws IllegalArgumentException if it is outside 0..100
     */
    protected final void setProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("the value should be from 0 to 100");
        }
        int old;
        synchronized (this) {
            if (this.progress == progress) {
                return;
            }
            old = this.progress;
            this.progress = progress;
        }
        firePropertyChange("progress", Integer.valueOf(old), Integer.valueOf(progress));
    }

    /** The reported progress, between 0 and 100. */
    public final int getProgress() {
        return this.progress;
    }

    /**
     * It starts the work on a background thread.
     *
     * <p>It returns at once. Calling it twice starts nothing the second time.
     */
    public final void execute() {
        getWorkersExecutorService().execute(this);
    }

    /**
     * It cancels the work.
     *
     * @param mayInterruptIfRunning whether the thread that is running it may be interrupted
     * @return `false` if it had already finished or was already cancelled
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return this.future.cancel(mayInterruptIfRunning);
    }

    /** Whether it was cancelled before finishing. */
    public final boolean isCancelled() {
        return this.future.isCancelled();
    }

    /** Whether it is not going to do anything else: it finished, broke or was cancelled. */
    public final boolean isDone() {
        return this.future.isDone();
    }

    /**
     * The result, waiting for it to be there.
     *
     * <p><b>It blocks.</b> Calling it from the interface's thread freezes it, which is exactly
     * what this class exists in order to avoid: `get`'s place is {@link #done}, where the result
     * is already known to be there.
     *
     * @throws InterruptedException if the wait is interrupted
     * @throws ExecutionException if {@link #doInBackground} threw
     */
    public final T get() throws InterruptedException, ExecutionException {
        return this.future.get();
    }

    /**
     * The result, waiting at most that long.
     *
     * @throws InterruptedException if the wait is interrupted
     * @throws ExecutionException if {@link #doInBackground} threw
     * @throws TimeoutException if the term runs out
     */
    public final T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return this.future.get(timeout, unit);
    }

    /** It adds a listener of the {@code "state"} and {@code "progress"} properties. */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /** It removes a listener. */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * It gives notice of a property change.
     *
     * <p>In the JDK the notice arrives on the interface's thread; here, on the one that fires
     * it.
     */
    public final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * The property support, in order to add listeners of a specific property.
     *
     * <p>It is exposed because {@link #addPropertyChangeListener} only adds general listeners, and
     * a client that only wants the progress has the right not to be woken up by the state.
     */
    public final PropertyChangeSupport getPropertyChangeSupport() {
        return this.propertyChangeSupport;
    }

    /** How the work is going. */
    public final StateValue getState() {
        return isDone() ? StateValue.DONE : this.state;
    }

    /** It changes the state and gives notice of it as the {@code "state"} property. */
    private void setState(StateValue state) {
        StateValue old = this.state;
        this.state = state;
        firePropertyChange("state", old, state);
    }

    /** {@link Task} calls it when the `FutureTask` finishes, however it finishes. */
    private void onDone() {
        setState(StateValue.DONE);
        done();
    }

    /** The shared pool, built the first time somebody needs it. */
    private static synchronized ExecutorService getWorkersExecutorService() {
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(MAX_WORKER_THREADS);
        }
        return executorService;
    }

    /**
     * The work's body, as a {@link Callable}.
     *
     * <p>It is a named class and not anonymous -- the JDK has it anonymous -- because that way it
     * reads: the only thing it does is mark the start and delegate.
     */
    private static final class Body<T> implements Callable<T> {

        private final SwingWorker<T, ?> owner;

        Body(SwingWorker<T, ?> owner) {
            this.owner = owner;
        }

        public T call() throws Exception {
            this.owner.setState(StateValue.STARTED);
            return this.owner.doInBackground();
        }
    }

    /** The {@link FutureTask} that tells the job when it finishes. */
    private static final class Task<T> extends FutureTask<T> {

        private final SwingWorker<T, ?> owner;

        Task(SwingWorker<T, ?> owner, Callable<T> body) {
            super(body);
            this.owner = owner;
        }

        protected void done() {
            this.owner.onDone();
        }
    }
}
