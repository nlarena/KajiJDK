package java.util.concurrent;

// java.util.concurrent.Callable — like Runnable but returns a value and may throw. The unit of work
// for `submit` when you want a result back.
public interface Callable<V> {
    V call() throws Exception;
}
