package java.util.concurrent;

// A {@link Future} that also knows *when* it is due. It adds no method of its own — it is
// the intersection of Future and {@link Delayed}, which is exactly the handle a scheduled
// submission needs: cancellable like any future, and orderable by remaining delay so the
// scheduler can keep the next-due task at the head of its queue.
public interface ScheduledFuture<V> extends Delayed, Future<V> {
}
