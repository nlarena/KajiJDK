package java.util.concurrent;

// Namespace for the four interfaces of reactive streams. Flow itself is uninstantiable and
// carries a single static method; everything interesting is in the nested types.
//
// The protocol they define is *pull*-driven backpressure: a Publisher never pushes at will.
// A Subscriber receives a Subscription and must `request(n)` before any `onNext` arrives,
// so the consumer's own rate bounds the producer, and no buffer can grow without a bound
// the consumer chose. A stream ends exactly once, with `onComplete` or `onError`, and
// `cancel` lets the consumer walk away at any point.
public final class Flow {

    // The buffer size a publisher uses when the caller does not pick one. A power of two,
    // large enough to amortise request round-trips and small enough to bound memory.
    static final int DEFAULT_BUFFER_SIZE = 256;

    private Flow() {
    }

    // The default per-subscriber buffer capacity.
    public static int defaultBufferSize() {
        return DEFAULT_BUFFER_SIZE;
    }

    // A source of items. Subscribing is the only thing a publisher does; everything after
    // that is driven through the Subscription handed to the subscriber.
    public interface Publisher<T> {

        // Register `subscriber`, which will receive exactly one `onSubscribe` call before
        // any item, and then items only as it requests them.
        void subscribe(Subscriber<? super T> subscriber);
    }

    // A sink for items. Its four methods are the whole protocol, and they arrive in order:
    // onSubscribe, then any number of onNext, then at most one of onError / onComplete.
    public interface Subscriber<T> {

        // Handed the control channel. The subscriber must call `request` on it — nothing
        // arrives until it does.
        void onSubscribe(Subscription subscription);

        // One item. Never called more times than were requested.
        void onNext(T item);

        // Terminal: the stream failed. No further call arrives.
        void onError(Throwable throwable);

        // Terminal: the stream ended normally. No further call arrives.
        void onComplete();
    }

    // The subscriber's side of the channel: how much it is willing to receive, and whether
    // it wants to stop.
    public interface Subscription {

        // Allow up to `n` more items. Additive across calls; a non-positive `n` is a
        // protocol error and is reported through onError.
        void request(long n);

        // Stop the flow. Items already in flight may still arrive.
        void cancel();
    }

    // Both ends at once: consumes a stream of T and publishes a stream of R. This is the
    // shape of every stage in the middle of a pipeline.
    public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
    }
}
