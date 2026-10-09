package java.util.concurrent;

// Results in **completion order** instead of submission order. That reordering is the entire
// value of the class, and it is worth stating what it replaces: holding a `List<Future<V>>`
// and calling get() on them in the order you submitted. Do that and you block on future #0
// while futures #3 and #7 sit finished and unread — the slowest task at the front stalls
// every result behind it. Here each task announces itself as it finishes, and {@link #take}
// hands back whichever landed first.
//
// The mechanism is small: {@link #submit} does not hand the raw task to the executor, it
// wraps it in a {@link RunnableFuture} whose run() is "run the real task, then push myself
// onto a queue". The executor still just runs Runnables and knows nothing about any of this;
// the reordering happens because *completion itself* is what enqueues, so the queue's order
// is completion order by construction. No polling, no timers, no scanning a list of futures.
//
// Note which end the queue is on. The executor's own work queue orders tasks going *in*;
// this completion queue orders results coming *out*. They are independent — you can pair a
// single-threaded executor with a completion service and still get results in the order the
// tasks happened to finish.
//
// WHERE THIS DIVERGES FROM THE JDK. The JDK's QueueingFuture *extends* FutureTask and
// overrides its protected `done()` hook. FutureTask here has that hook too, but subclassing is
// still not available: the override would have to call `super.run()`, and our javac emits no
// invokespecial for it (finding #125). So {@link QueueingFuture} below *wraps* a FutureTask and
// forwards the Future methods to it.
// Observably it is the same — the wrapper is what the caller gets back and what lands in the
// queue, so the identity comparison `completionService.take() == theFutureIGotBack` still
// holds. The JDK also reuses an {@link AbstractExecutorService}'s newTaskFor when it can; we
// always build our own wrapper, which costs one object and no behaviour.
//
// Single-exit style throughout (finding #105).
public class ExecutorCompletionService<V> implements CompletionService<V> {

    private final Executor executor;
    // Finished tasks, in the order they finished. Unbounded, so a completing task never
    // blocks trying to report itself.
    private final BlockingQueue<Future<V>> completionQueue;

    public ExecutorCompletionService(Executor executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.completionQueue = new LinkedBlockingQueue<Future<V>>();
    }

    // Supply your own completion queue — a bounded one to cap unread results, or a
    // PriorityBlockingQueue to impose a second ordering on top of completion order.
    public ExecutorCompletionService(Executor executor, BlockingQueue<Future<V>> completionQueue) {
        if (executor == null || completionQueue == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.completionQueue = completionQueue;
    }

    public Future<V> submit(Callable<V> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        FutureTask<V> inner = new FutureTask<V>(task);
        QueueingFuture<V> wrapper = new QueueingFuture<V>(inner, completionQueue);
        executor.execute(wrapper);
        return wrapper;
    }

    public Future<V> submit(Runnable task, V result) {
        if (task == null) {
            throw new NullPointerException();
        }
        FutureTask<V> inner = new FutureTask<V>(task, result);
        QueueingFuture<V> wrapper = new QueueingFuture<V>(inner, completionQueue);
        executor.execute(wrapper);
        return wrapper;
    }

    // The next task to have finished, blocking until one has.
    public Future<V> take() throws InterruptedException {
        return completionQueue.take();
    }

    // The next finished task, or null if none has finished yet — never blocks.
    public Future<V> poll() {
        return completionQueue.poll();
    }

    public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return completionQueue.poll(timeout, unit);
    }
}

// The wrapper that makes completion self-reporting: run the real task, then put myself on
// the completion queue. Everything a Future can be asked is forwarded to the inner task.
//
// Top-level and package-private rather than nested, since a nested class inside a *generic*
// class is miscompiled (finding #13).
final class QueueingFuture<V> implements RunnableFuture<V> {

    private final FutureTask<V> task;
    private final BlockingQueue<Future<V>> completionQueue;

    QueueingFuture(FutureTask<V> task, BlockingQueue<Future<V>> completionQueue) {
        this.task = task;
        this.completionQueue = completionQueue;
    }

    // The one line that does the work. The enqueue happens *after* the task has published its
    // outcome, so a consumer that takes this future can get() it without waiting.
    public void run() {
        task.run();
        completionQueue.offer(this);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return task.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return task.isCancelled();
    }

    public boolean isDone() {
        return task.isDone();
    }

    public V get() throws InterruptedException, ExecutionException {
        return task.get();
    }

    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return task.get(timeout, unit);
    }
}
