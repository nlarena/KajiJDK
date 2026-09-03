package java.util.concurrent;

import java.util.Collection;
import java.util.List;

// An {@link Executor} with a lifecycle and results: tasks can be submitted for a
// {@link Future}, and the service can be shut down and waited on. This is the interface
// application code normally holds — a pool, hidden behind its contract.
public interface ExecutorService extends Executor, AutoCloseable {

    // Stop accepting tasks; those already submitted still run.
    void shutdown();

    /**
     * Stops accepting tasks, gives up on the ones still queued, and returns them.
     *
     * <p>The list is the point. {@link #shutdown} drains the backlog; this one abandons it, and a
     * caller that has to abandon work usually has to *account* for it -- log it, persist it, hand it
     * to another service. Discarding the tasks and returning nothing would leave no way to do that.
     *
     * <p>Tasks already running are asked to stop by interruption, which they are free to ignore; the
     * name promises a best effort and not a guarantee.
     */
    List<Runnable> shutdownNow();

    boolean isShutdown();

    // True once shutdown completed and every task has finished.
    boolean isTerminated();

    // Wait for termination, up to the given time; reports whether it terminated.
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    // Submit a value-returning task.
    <T> Future<T> submit(Callable<T> task);

    // Submit a Runnable; the Future yields `result` when it completes.
    <T> Future<T> submit(Runnable task, T result);

    // Submit a Runnable; the Future yields null when it completes.
    Future<?> submit(Runnable task);

    /**
     * Runs every task and returns once they have ALL finished, one Future per task in the order of
     * the collection's iterator.
     *
     * <p>Every returned Future {@code isDone()}; that is the contract, and it is what separates this
     * from a loop of {@code submit}. Finished does not mean succeeded -- a task that threw is done
     * too, and its Future reports the failure when read.
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException;

    /**
     * The same, but giving up on whatever has not finished when the timeout expires.
     *
     * <p>The Futures of the unfinished tasks come back cancelled rather than missing, so the result
     * list still lines up one-to-one with the input -- the caller can tell *which* task did not make
     * it, which a shorter list would not say.
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * Runs the tasks and returns the result of the first one to SUCCEED, cancelling the rest.
     *
     * <p>"Succeed", not "finish": tasks that throw are passed over and the others keep running. This
     * is the redundant-request pattern -- ask three replicas, take the first good answer -- and it
     * only works if a failure is a reason to wait for the others rather than to give up.
     *
     * @throws ExecutionException if no task succeeded; the cause is the last failure seen
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException;

    // The same with a deadline; a TimeoutException means nothing succeeded in time.
    <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Shuts the service down and waits for the tasks already submitted to finish -- which is what
     * lets an ExecutorService be a try-with-resources resource (#276).
     *
     * <p>It is a {@code default} and not an abstract method on purpose: it is one in the JDK too,
     * so every implementation gets the behaviour without writing it, and no existing implementor
     * of this interface breaks by the addition.
     *
     * <p>Waits in slices rather than forever so that a service that never terminates does not
     * wedge the caller with no way out. The JDK's version waits indefinitely and handles
     * interruption; there is no interruption to handle here, so the honest form is a bounded loop
     * that gives up rather than one that pretends to wait for ever.
     */
    @Override
    default void close() {
        this.shutdown();
        int rounds = 0;
        boolean interrumpido = false;
        while (!this.isTerminated() && rounds < 100) {
            // `close()` viene de `AutoCloseable` y **no** puede declarar `InterruptedException`: un
            // try-with-resources no podria cerrarla. El JDK hace lo mismo -- atrapa, corta la espera
            // y remarca el hilo, que es lo unico honesto: el pool queda pidiendo apagarse aunque no
            // se lo haya esperado hasta el final.
            try {
                this.awaitTermination(100L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                interrumpido = true;
                rounds = 100;
            }
            rounds = rounds + 1;
        }
        if (interrumpido) {
            Thread.currentThread().interrupt();
        }
    }
}
