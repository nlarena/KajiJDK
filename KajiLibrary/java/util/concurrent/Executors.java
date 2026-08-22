package java.util.concurrent;

// Factory methods for the common executor shapes, so callers configure a pool by naming
// what they want rather than by wiring a {@link ThreadPoolExecutor} by hand.
//
// Note for KajiLibrary callers: these return the {@link ExecutorService} *interface*, and a
// chained call on an interface-typed result is silently dropped by the frozen javac
// (finding #108). Bind the result to a local first:
//   ExecutorService pool = Executors.newFixedThreadPool(2);
//   Future<Integer> f = pool.submit(task);
public class Executors {

    // Not instantiable — a holder of static factories, like the JDK's.
    private Executors() {
    }

    // A pool of exactly `nThreads` reused threads, fed by an unbounded queue.
    public static ExecutorService newFixedThreadPool(int nThreads) {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        return new ThreadPoolExecutor(nThreads, nThreads, queue);
    }

    // A single worker thread: tasks run one at a time, in submission order.
    public static ExecutorService newSingleThreadExecutor() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        return new ThreadPoolExecutor(1, 1, queue);
    }

    // Adapt a Runnable to a Callable returning `result`.
    public static <T> Callable<T> callable(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        return new RunnableAdapter<T>(task, result);
    }

    // Adapt a Runnable to a Callable returning null.
    public static Callable<Object> callable(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        return new RunnableAdapter<Object>(task, null);
    }
}
