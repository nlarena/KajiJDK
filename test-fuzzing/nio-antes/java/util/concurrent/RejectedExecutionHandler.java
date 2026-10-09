package java.util.concurrent;

// The escape hatch for a submission an executor cannot accept — its queue is saturated, or
// it has been shut down. Rejection is a *policy*, not a fact: the same situation can
// reasonably mean "throw", "silently drop", "run it on the caller's thread" or "evict the
// oldest queued task", and only the application knows which. So the executor delegates.
//
// Running the task on the caller's thread is the interesting one: it throttles the
// submitter, which is the only backpressure a pool can apply to code that outruns it.
public interface RejectedExecutionHandler {

    // Called by `executor` when it cannot take `r`.
    void rejectedExecution(Runnable r, ThreadPoolExecutor executor);
}
