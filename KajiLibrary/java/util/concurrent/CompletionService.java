package java.util.concurrent;

// Decouples submitting tasks from consuming their results *in completion order*. With a
// plain {@link ExecutorService} you hold a list of futures and must guess which to `get`
// first — get the wrong one and you block on a slow task while fast results sit finished
// and unread. Here every task that finishes reports to a queue, and {@link #take} hands
// back whichever completed first.
//
// That is the difference between "wait for all N" and "handle each as it lands": with a
// CompletionService the total latency is bounded by the *slowest* task, but the *first*
// result is available as soon as any task finishes.
//
// The waits declare `throws InterruptedException`, as in the JDK. The note that used to be here
// said it was omitted "throughout the package"; that omission was a mistake finding #316 uncovered
// -- a wait that cannot be interrupted leaves the caller with no way of getting a thread out of
// it.
public interface CompletionService<V> {

    // Submit a value-returning task; its future will appear in the completion queue.
    Future<V> submit(Callable<V> task);

    // Submit a Runnable; its future yields `result` and appears in the completion queue.
    Future<V> submit(Runnable task, V result);

    // Take the next completed task's future, waiting if none has completed yet.
    Future<V> take() throws InterruptedException;

    // The next completed task's future, or null if none is ready right now.
    Future<V> poll();

    // The next completed task's future, waiting up to the timeout; null if none arrives.
    Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException;
}
