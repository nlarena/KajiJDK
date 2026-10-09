package java.util.concurrent;

// Both halves of a submitted task in one type: the {@link Runnable} an executor runs, and
// the {@link Future} the submitter waits on. Naming the pair is what lets a framework hand
// a task to an executor and a handle to the caller *without* knowing it is a
// {@link FutureTask} — see {@link AbstractExecutorService#newTaskFor}, whose whole purpose
// is to let a subclass substitute a different implementation of this interface.
public interface RunnableFuture<V> extends Runnable, Future<V> {

    // Compute the result, unless the task was already cancelled.
    void run();
}
