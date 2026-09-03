package java.util.concurrent;

import java.util.List;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

// Factory methods for the common executor shapes, so callers configure a pool by naming
// what they want rather than by wiring a {@link ThreadPoolExecutor} by hand.
//
// The shapes differ in exactly one thing — which queue is paired with which sizing — and
// that pairing is the whole design of each pool:
//
//   fixed        bounded thread count, unbounded queue   -> a backlog absorbs bursts
//   cached       unbounded thread count, no queue at all -> a burst becomes threads
//   single       one thread, unbounded queue             -> serialised execution
//   work-stealing  a ForkJoinPool                        -> tasks that fork subtasks
//
// Note for KajiLibrary callers: these return the {@link ExecutorService} *interface*. Bind
// the result to a local before chaining:
//   ExecutorService pool = Executors.newFixedThreadPool(2);
//   Future<Integer> f = pool.submit(task);
//
// La nota anterior decia que `callable(PrivilegedAction)` y `callable(PrivilegedExceptionAction)`
// quedaban afuera porque sus tipos de parametro no existian en esta biblioteca, y que sustituirlos
// habria dado "otro metodo con el nombre correcto". Era cierto y ya no lo es: `java.security` trajo
// las dos interfaces, asi que los dos metodos van con su firma de verdad.
//
// Los tres `privileged*` estan porque el JDK 25 ya los vacio: sin Security Manager no establecen
// ningun contexto de control de acceso, y lo que queda --el class loader de contexto-- esta.
public class Executors {

    // Not instantiable — a holder of static factories, like the JDK's.
    private Executors() {
    }

    // ---------------------------------------------------------------- fixed-size pools

    // A pool of exactly `nThreads` reused threads, fed by an unbounded queue.
    public static ExecutorService newFixedThreadPool(int nThreads) {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        return new ThreadPoolExecutor(nThreads, nThreads, queue);
    }

    // The same, with the threads built by the given factory — which is how a pool's threads get
    // named or made daemons without the pool knowing anything about that policy.
    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        return new ThreadPoolExecutor(nThreads, nThreads, queue, threadFactory, null);
    }

    /**
     * A single worker thread: tasks run one at a time, in submission order.
     *
     * <p>Wrapped so the result cannot be cast back to a ThreadPoolExecutor and resized. The
     * guarantee this factory sells is "one thread, ever" -- a caller who could reach through the
     * interface and call {@code setCorePoolSize(4)} would break it for everyone else holding the
     * same service, including code that relies on serialisation for its own thread safety.
     */
    public static ExecutorService newSingleThreadExecutor() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        return new DelegatedExecutorService(new ThreadPoolExecutor(1, 1, queue));
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        return new DelegatedExecutorService(
                new ThreadPoolExecutor(1, 1, queue, threadFactory, null));
    }

    // ---------------------------------------------------------------- elastic pools

    /**
     * A pool that grows on demand and lets idle threads go after a minute.
     *
     * <p>The queue is a {@link SynchronousQueue}, and that is the whole trick: it has no capacity,
     * so an offer succeeds only if a worker is waiting to take it right then. Every other
     * submission fails the offer and the pool grows. Pair the same sizing with a
     * {@code LinkedBlockingQueue} and the pool would never grow past its core -- which is zero
     * here, so nothing would ever run.
     */
    public static ExecutorService newCachedThreadPool() {
        SynchronousQueue<Runnable> queue = new SynchronousQueue<Runnable>();
        return new ThreadPoolExecutor(0, 2147483647, 60L, TimeUnit.SECONDS, queue);
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        SynchronousQueue<Runnable> queue = new SynchronousQueue<Runnable>();
        return new ThreadPoolExecutor(0, 2147483647, 60L, TimeUnit.SECONDS, queue, threadFactory);
    }

    /**
     * An executor that starts a brand new thread for every task and pools nothing.
     *
     * <p>The opposite trade from every pool above: no reuse, no queue, no bound. It is the right
     * shape only when threads are cheap -- which is what {@link #newVirtualThreadPerTaskExecutor}
     * is for -- or when the task count is small and known.
     */
    public static ExecutorService newThreadPerTaskExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }
        return new ThreadPerTaskExecutor(threadFactory);
    }

    /**
     * A thread-per-task executor over virtual threads.
     *
     * <p>KajiJDK has no virtual-thread carrier: {@link Thread#ofVirtual} builds a thread that runs
     * on the platform substrate and reports {@code isVirtual() == true}. So this behaves exactly
     * like {@link #newThreadPerTaskExecutor} with that factory, and the difference from the JDK is
     * cost, not semantics -- a thousand tasks here cost a thousand OS threads.
     */
    public static ExecutorService newVirtualThreadPerTaskExecutor() {
        // Bound to locals rather than chained: the intermediate is interface-typed, and this is
        // the shape that the frozen javac used to drop silently (finding #108).
        Thread.Builder.OfVirtual builder = Thread.ofVirtual();
        ThreadFactory factory = builder.factory();
        return new ThreadPerTaskExecutor(factory);
    }

    /**
     * A work-stealing pool with one thread per available processor.
     *
     * <p>A {@link ForkJoinPool}, not a ThreadPoolExecutor, and in async mode: the tasks this pool
     * is for are event-style ones that are never joined, so its queue is FIFO rather than the
     * LIFO order a divide-and-conquer computation wants.
     */
    public static ExecutorService newWorkStealingPool() {
        Runtime runtime = Runtime.getRuntime();
        return new ForkJoinPool(runtime.availableProcessors(),
                ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    public static ExecutorService newWorkStealingPool(int parallelism) {
        return new ForkJoinPool(parallelism,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    // ---------------------------------------------------------------- scheduled pools

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return new ScheduledThreadPoolExecutor(corePoolSize);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize,
                                                                  ThreadFactory threadFactory) {
        return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
    }

    // One thread, on a clock. Wrapped for the same reason as newSingleThreadExecutor.
    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return new DelegatedScheduledExecutorService(new ScheduledThreadPoolExecutor(1));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(
            ThreadFactory threadFactory) {
        return new DelegatedScheduledExecutorService(
                new ScheduledThreadPoolExecutor(1, threadFactory));
    }

    // ---------------------------------------------------------------- wrappers

    /**
     * Hides everything but the {@link ExecutorService} contract.
     *
     * <p>Handing a ThreadPoolExecutor to code that only needs to submit tasks lets that code
     * resize it, replace its rejection policy or drain its queue. This wrapper is the answer:
     * the configuration methods are not merely undocumented, they are unreachable, because the
     * returned object is not a ThreadPoolExecutor and no cast can make it one.
     */
    public static ExecutorService unconfigurableExecutorService(ExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        return new DelegatedExecutorService(executor);
    }

    public static ScheduledExecutorService unconfigurableScheduledExecutorService(
            ScheduledExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        return new DelegatedScheduledExecutorService(executor);
    }

    // ---------------------------------------------------------------- thread factories

    /**
     * The factory a pool uses when none is given: non-daemon threads at normal priority, named
     * {@code pool-N-thread-M}.
     *
     * <p>The names are the point. A thread dump of an application with four pools is unreadable
     * when every thread is called "Thread-17", and the counters here are what make each one say
     * which pool it belongs to and which of that pool's threads it is.
     */
    public static ThreadFactory defaultThreadFactory() {
        return new DefaultThreadFactory();
    }

    /**
     * A factory whose threads run with the context class loader of the thread that called this.
     *
     * <p>In the JDK this also captured an access control context; the Security Manager is gone as
     * of JDK 17, so the class loader is all that is left, and it is the half that still matters --
     * a pool thread created at start-up otherwise carries the application class loader into work
     * submitted from a plugin that has its own.
     */
    public static ThreadFactory privilegedThreadFactory() {
        return new PrivilegedThreadFactory();
    }

    // ---------------------------------------------------------------- Runnable/Callable adapters

    // Adapt a Runnable to a Callable returning `result`.
    public static <T> Callable<T> callable(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        return new RunnableAdapter<T>(task, result);
    }

    // Adapt a Runnable to a Callable returning null.
    /**
     * Un {@link Callable} que corre esa accion privilegiada.
     *
     * <p>El puente entre las dos formas que Java tiene de decir "una operacion sin argumentos que
     * devuelve algo": `PrivilegedAction` es la de `java.security` y `Callable` la de acá. Existe
     * porque un `ExecutorService` solo sabe de la segunda.
     *
     * <p>Devuelve `Callable<Object>` y no `Callable<T>`, cosa que parece un descuido y no lo es: la
     * firma es de 2004, anterior a que la accion fuera generica, y cambiarla ahora rompería a todo el
     * que la usa. El valor que sale es el que la accion devuelve, sin tocar.
     */
    public static Callable<Object> callable(java.security.PrivilegedAction<?> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return new PrivilegedActionAdapter(action);
    }

    /**
     * El de arriba para una accion que **puede fallar**.
     *
     * <p>Son dos metodos y no uno porque las dos interfaces son distintas: la de arriba no puede
     * lanzar nada chequeado y esta si. `Callable.call` declara `throws Exception`, asi que del lado
     * de aca las dos entran igual -- la diferencia esta del lado de la accion.
     */
    public static Callable<Object> callable(java.security.PrivilegedExceptionAction<?> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return new PrivilegedExceptionActionAdapter(action);
    }

    // Los dos adaptadores. Clases con nombre y no lambdas porque el `Callable` que sale de aca puede
    // terminar en una traza, y un nombre dice de donde vino.

    private static final class PrivilegedActionAdapter implements Callable<Object> {
        private final java.security.PrivilegedAction<?> action;

        PrivilegedActionAdapter(java.security.PrivilegedAction<?> action) {
            this.action = action;
        }

        public Object call() {
            return this.action.run();
        }
    }

    private static final class PrivilegedExceptionActionAdapter implements Callable<Object> {
        private final java.security.PrivilegedExceptionAction<?> action;

        PrivilegedExceptionActionAdapter(java.security.PrivilegedExceptionAction<?> action) {
            this.action = action;
        }

        public Object call() throws Exception {
            return this.action.run();
        }
    }

    public static Callable<Object> callable(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        return new RunnableAdapter<Object>(task, null);
    }

    /**
     * Historically, a callable that ran under the caller's access control context.
     *
     * <p>There is no access control context any more, so what it returns is a wrapper that calls
     * through. Kept because the JDK keeps it -- deprecated for removal there since 17 -- and
     * because a wrapper that does nothing is honest, whereas dropping the method would break
     * source that still names it.
     */
    public static <T> Callable<T> privilegedCallable(Callable<T> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        return new PrivilegedCallable<T>(callable);
    }

    // The same, plus the one part that still has an effect: the callable runs with the context
    // class loader that was current when this method was called, not the worker's.
    public static <T> Callable<T> privilegedCallableUsingCurrentClassLoader(Callable<T> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        return new PrivilegedCallableUsingCurrentClassLoader<T>(callable);
    }
}

// `RunnableAdapter`, which the two `callable` factories return, lives in FutureTask.java: the
// same wrapper is what a FutureTask built from a Runnable uses, and one copy is enough.

// The empty shell of what used to run under an access control context.
final class PrivilegedCallable<T> implements Callable<T> {

    private final Callable<T> task;

    PrivilegedCallable(Callable<T> task) {
        this.task = task;
    }

    public T call() throws Exception {
        return task.call();
    }

    public String toString() {
        return "PrivilegedCallable[Wrapped task = " + task.toString() + "]";
    }
}

// Runs the task with the class loader captured at construction, and puts the worker's own back
// afterwards — a pool thread outlives the task, so leaving it with a foreign loader would leak
// into whatever runs next on that thread.
final class PrivilegedCallableUsingCurrentClassLoader<T> implements Callable<T> {

    private final Callable<T> task;
    private final ClassLoader captured;

    PrivilegedCallableUsingCurrentClassLoader(Callable<T> task) {
        this.task = task;
        Thread here = Thread.currentThread();
        this.captured = here.getContextClassLoader();
    }

    public T call() throws Exception {
        Thread t = Thread.currentThread();
        ClassLoader previous = t.getContextClassLoader();
        T value;
        if (previous == captured) {
            value = task.call();
        } else {
            t.setContextClassLoader(captured);
            try {
                value = task.call();
            } finally {
                t.setContextClassLoader(previous);
            }
        }
        return value;
    }

    public String toString() {
        return "PrivilegedCallableUsingCurrentClassLoader[Wrapped task = " + task.toString() + "]";
    }
}

// The default factory: named, non-daemon, normal-priority threads.
class DefaultThreadFactory implements ThreadFactory {

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    DefaultThreadFactory() {
        Thread here = Thread.currentThread();
        this.group = here.getThreadGroup();
        this.namePrefix = "pool-" + poolNumber.getAndIncrement() + "-thread-";
    }

    public Thread newThread(Runnable r) {
        String name = namePrefix + threadNumber.getAndIncrement();
        Thread t = new Thread(group, r, name, 0L);
        // Non-daemon on purpose: a pool thread holding an unfinished task must keep the JVM
        // alive, or shutting down would abandon work the caller was told had been accepted.
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}

// The default factory plus the captured context class loader.
//
// Composition and not `extends DefaultThreadFactory`, because the override would have to call
// `super.newThread(...)` and our javac emits no invokespecial for that (finding #125).
final class PrivilegedThreadFactory implements ThreadFactory {

    private final DefaultThreadFactory base = new DefaultThreadFactory();
    private final ClassLoader captured;

    PrivilegedThreadFactory() {
        Thread here = Thread.currentThread();
        this.captured = here.getContextClassLoader();
    }

    public Thread newThread(Runnable r) {
        return base.newThread(new ContextSettingRunnable(captured, r));
    }
}

// Installs a class loader on the thread it runs on, then runs the real task.
final class ContextSettingRunnable implements Runnable {

    private final ClassLoader loader;
    private final Runnable task;

    ContextSettingRunnable(ClassLoader loader, Runnable task) {
        this.loader = loader;
        this.task = task;
    }

    public void run() {
        Thread t = Thread.currentThread();
        t.setContextClassLoader(loader);
        task.run();
    }
}

// A new thread per task, and nothing else: no queue, no reuse, no bound.
//
// The lifecycle still has to be real, which is the only part with any design in it: `shutdown`
// stops accepting, and termination is reached when the last started task finishes -- so the count
// of live tasks, not a worker list, is what awaitTermination waits on.
final class ThreadPerTaskExecutor extends AbstractExecutorService {

    private final Object sync = new Object();
    private final ThreadFactory factory;
    private final java.util.ArrayList<Thread> live = new java.util.ArrayList<Thread>();
    private boolean shutdown;

    ThreadPerTaskExecutor(ThreadFactory factory) {
        this.factory = factory;
    }

    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        Thread t = null;
        boolean rejected = false;
        synchronized (sync) {
            if (shutdown) {
                rejected = true;
            } else {
                t = factory.newThread(new TrackedTask(this, command));
                if (t == null) {
                    rejected = true;
                } else {
                    live.add(t);
                }
            }
        }
        if (rejected) {
            throw new RejectedExecutionException("executor has been shut down");
        }
        t.start();
    }

    // Called by each task's wrapper when it finishes.
    void taskFinished(Thread t) {
        synchronized (sync) {
            live.remove(t);
            sync.notifyAll();
        }
    }

    public void shutdown() {
        synchronized (sync) {
            shutdown = true;
            sync.notifyAll();
        }
    }

    // There is no queue, so there is nothing to hand back: every accepted task is already
    // running. The interrupt is the only thing left that can stop them.
    public List<Runnable> shutdownNow() {
        java.util.ArrayList<Thread> toInterrupt;
        synchronized (sync) {
            shutdown = true;
            toInterrupt = new java.util.ArrayList<Thread>(live);
            sync.notifyAll();
        }
        for (int i = 0; i < toInterrupt.size(); i++) {
            Thread t = toInterrupt.get(i);
            t.interrupt();
        }
        return new java.util.ArrayList<Runnable>();
    }

    public boolean isShutdown() {
        boolean s;
        synchronized (sync) {
            s = shutdown;
        }
        return s;
    }

    // How many tasks are running right now, which for this executor is also how many threads it
    // has: there is no queue, so a task that has been accepted is a thread that exists.
    public long threadCount() {
        long n;
        synchronized (sync) {
            n = (long) live.size();
        }
        return n;
    }

    // The live threads, as a snapshot. A copy and not the list itself: this library's streams are
    // eager, so the stream is built from the array the snapshot already froze, and a thread that
    // starts or finishes while the caller reads it cannot corrupt the traversal.
    public java.util.stream.Stream<Thread> threads() {
        java.util.ArrayList<Thread> snapshot;
        synchronized (sync) {
            snapshot = new java.util.ArrayList<Thread>(live);
        }
        return snapshot.stream();
    }

    public boolean isTerminated() {
        boolean t;
        synchronized (sync) {
            t = shutdown && live.isEmpty();
        }
        return t;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        boolean done;
        synchronized (sync) {
            long ms = unit.toMillis(timeout);
            long slice = ms / 8L + 1L;
            for (int i = 0; i < 8; i++) {
                if (!(shutdown && live.isEmpty()) && ms > 0L) {
                    sync.wait(slice);
                }
            }
            done = shutdown && live.isEmpty();
        }
        return done;
    }
}

// Runs a task and tells its executor when the thread is done, which is what makes termination
// observable at all in a pool that keeps no workers.
final class TrackedTask implements Runnable {

    private final ThreadPerTaskExecutor owner;
    private final Runnable task;

    TrackedTask(ThreadPerTaskExecutor owner, Runnable task) {
        this.owner = owner;
        this.task = task;
    }

    public void run() {
        Thread me = Thread.currentThread();
        try {
            task.run();
        } catch (RuntimeException e) {
            // A failing task is the task's business; the bookkeeping below is not optional.
        }
        owner.taskFinished(me);
    }
}

// Exposes only the ExecutorService contract of the service it wraps.
class DelegatedExecutorService implements ExecutorService {

    private final ExecutorService e;

    DelegatedExecutorService(ExecutorService executor) {
        this.e = executor;
    }

    public void execute(Runnable command) {
        e.execute(command);
    }

    public void shutdown() {
        e.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return e.shutdownNow();
    }

    public boolean isShutdown() {
        return e.isShutdown();
    }

    public boolean isTerminated() {
        return e.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return e.awaitTermination(timeout, unit);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return e.submit(task);
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return e.submit(task, result);
    }

    public Future<?> submit(Runnable task) {
        return e.submit(task);
    }

    // Every batch call spells its type argument out (`e.<T>invokeAll`). Our javac cannot infer a
    // generic method's type argument when the actual argument's static type is built from the
    // ENCLOSING method's type variable, and reports "tipo de retorno incompatible" -- the same
    // workaround AbstractExecutorService applies to newTaskFor.
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return e.<T>invokeAll(tasks);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                                         TimeUnit unit) throws InterruptedException {
        return e.<T>invokeAll(tasks, timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return e.<T>invokeAny(tasks);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return e.<T>invokeAny(tasks, timeout, unit);
    }
}

// The same for a scheduled service: the four scheduling methods pass through, everything
// configurable stays hidden.
//
// Composition again rather than `extends DelegatedExecutorService`, because forwarding the
// inherited half would need `super.method()` calls the compiler cannot emit (finding #125).
final class DelegatedScheduledExecutorService implements ScheduledExecutorService {

    private final ScheduledExecutorService e;

    DelegatedScheduledExecutorService(ScheduledExecutorService executor) {
        this.e = executor;
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return e.schedule(command, delay, unit);
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return e.schedule(callable, delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                                                  TimeUnit unit) {
        return e.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                     long delay, TimeUnit unit) {
        return e.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    public void execute(Runnable command) {
        e.execute(command);
    }

    public void shutdown() {
        e.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return e.shutdownNow();
    }

    public boolean isShutdown() {
        return e.isShutdown();
    }

    public boolean isTerminated() {
        return e.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return e.awaitTermination(timeout, unit);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return e.submit(task);
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return e.submit(task, result);
    }

    public Future<?> submit(Runnable task) {
        return e.submit(task);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return e.<T>invokeAll(tasks);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                                         TimeUnit unit) throws InterruptedException {
        return e.<T>invokeAll(tasks, timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return e.<T>invokeAny(tasks);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return e.<T>invokeAny(tasks, timeout, unit);
    }
}
