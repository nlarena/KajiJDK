package java.util.concurrent;

// A task that returns a result and may throw — the counterpart of {@link Runnable}, which
// can do neither. This is what makes a task's outcome available through a {@link Future}.
public interface Callable<V> {

    V call() throws Exception;
}
