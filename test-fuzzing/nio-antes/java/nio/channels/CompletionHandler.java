package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.CompletionHandler — what to do when an asynchronous operation
 * finishes.
 *
 * <p>Two methods and not one, and there is the whole design: success and failure arrive by separate
 * roads, so there is no way of writing the happy case and forgetting the other one. With a single
 * callback that received "result or error", forgetting to look at the error would be the
 * comfortable thing.
 *
 * <p>The `attachment` is an object the caller of the operation hands over and receives back without
 * anybody touching it. It exists because a handler is usually shared by many operations and needs
 * to know which one each notice corresponds to — without forcing a new handler to be created per
 * operation.
 *
 * @param <V> the type of the result
 * @param <A> the type of the attached object
 */
public interface CompletionHandler<V, A> {

    /** The operation finished well. */
    void completed(V result, A attachment);

    /** The operation failed. */
    void failed(Throwable exc, A attachment);
}
