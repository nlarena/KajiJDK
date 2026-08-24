package java.util.concurrent;

// A blocking queue where a producer can wait for its element to be *consumed*, not merely
// accepted. An ordinary BlockingQueue only tells the producer that the element was stored;
// `transfer` holds the producer until a consumer has actually taken it, which turns the
// queue into a rendezvous when that is what the protocol needs — and leaves it a plain
// buffered queue when `put`/`offer` are used instead.
//
// The two consumer-count queries exist so a producer can choose between the two modes at
// run time: hand off directly if someone is already waiting, otherwise enqueue and move on.
//
// No `throws InterruptedException` is declared on the blocking methods even though they
// interrupt: re-stating a superinterface's throws clause against an already-compiled
// BlockingQueue is rejected (finding #104), and the descriptor is unaffected.
public interface TransferQueue<E> extends BlockingQueue<E> {

    // Hand `e` straight to a waiting consumer if there is one; false if there is none, in
    // which case nothing is enqueued.
    boolean tryTransfer(E e);

    // Hand `e` to a consumer, waiting as long as it takes.
    void transfer(E e);

    // Like {@link #tryTransfer(Object)} but willing to wait up to the given timeout.
    boolean tryTransfer(E e, long timeout, TimeUnit unit);

    // True if any consumer is currently blocked waiting to take an element.
    boolean hasWaitingConsumer();

    // How many consumers are currently blocked waiting. An estimate under concurrency.
    int getWaitingConsumerCount();
}
