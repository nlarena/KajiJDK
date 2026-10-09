package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;


/**
 * A {@link Flow.Publisher} that hands items to its subscribers by submitting them, each subscriber
 * getting its own buffer and its own pace.
 *
 * <p>The whole class is an answer to one question: a publisher has ONE producer and MANY consumers,
 * and they do not run at the same speed. The reactive protocol says a subscriber only receives what
 * it asked for, so a fast producer and a slow consumer need somewhere for the surplus to sit — and
 * that somewhere has to be per subscriber, because a slow one must not be able to stall the others.
 * Hence one bounded buffer each, and hence the two ways to publish:
 *
 * <ul>
 *   <li>{@link #submit} BLOCKS while any buffer is full — the producer is slowed to the pace of the
 *       slowest consumer, and nothing is lost.
 *   <li>{@link #offer} does not block — an item that does not fit is DROPPED, and the caller is told
 *       how many were dropped through a negative return.
 * </ul>
 *
 * <p>Delivery happens on the executor, never on the caller's thread, so a subscriber that takes its
 * time in {@code onNext} delays only itself.
 *
 * @implNote A KajiLibrary subset. The JDK's version drives each buffer with a lock-free ring and a
 *           compare-and-set state machine; this one guards its state with a monitor, the idiom the
 *           rest of this package already uses ({@link ThreadPoolExecutor} does the same). The
 *           contract is the same — per-subscriber buffering, backpressure through {@code request},
 *           at most one terminal signal — and what differs is throughput under contention.
 *
 *           <p>A dedicated {@code sync} object and {@code synchronized} BLOCKS, not
 *           {@code synchronized} methods: the method modifier is dropped from the emitted flags
 *           (finding #255), so a method that reads as synchronized takes no monitor at all and its
 *           {@code wait}/{@code notifyAll} throw. Every block below has a SINGLE exit, because an
 *           early {@code return} inside one does not emit its {@code monitorexit} (finding #105).
 *
 *           <p>The monitor is never held while a subscriber's callback runs, nor while the executor
 *           is handed a task. That is not a detail: a subscriber may call {@code request} or
 *           {@code cancel} from inside its own {@code onNext}, and a direct executor runs the task
 *           on the calling thread — either one deadlocks against a held monitor.
 */
public class SubmissionPublisher<T> implements Flow.Publisher<T>, AutoCloseable {

    private final Object sync = new Object();

    private final Executor executor;
    private final int maxBufferCapacity;
    private final BiConsumer<? super Flow.Subscriber<? super T>, ? super Throwable> onNextHandler;

    // Live subscriptions, guarded by `sync`. Cancelled ones are pruned lazily, on the next question
    // that has to walk the list, rather than eagerly from cancel() -- a subscriber may cancel from
    // inside its own onNext, and reaching back into the publisher's monitor from there is how
    // deadlocks are built.
    private final ArrayList<PubSubscription<T>> subscriptions;

    private boolean closed;
    private Throwable closedException;

    /**
     * Creates a publisher delivering on {@code executor}, buffering at most {@code maxBufferCapacity}
     * items per subscriber.
     *
     * @param executor where delivery runs; never the submitting thread
     * @param maxBufferCapacity the per-subscriber bound, which must be positive
     * @param handler called when a subscriber's {@code onNext} throws, or {@code null} to cancel
     *                that subscription instead
     */
    public SubmissionPublisher(Executor executor, int maxBufferCapacity,
            BiConsumer<? super Flow.Subscriber<? super T>, ? super Throwable> handler) {
        if (executor == null) {
            throw new NullPointerException();
        }
        if (maxBufferCapacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.executor = executor;
        this.maxBufferCapacity = maxBufferCapacity;
        this.onNextHandler = handler;
        this.subscriptions = new ArrayList<PubSubscription<T>>();
        this.closed = false;
        this.closedException = null;
    }

    /** Creates a publisher with no handler: a subscriber whose {@code onNext} throws is cancelled. */
    public SubmissionPublisher(Executor executor, int maxBufferCapacity) {
        this(executor, maxBufferCapacity, null);
    }

    /** Creates a publisher on the common pool, with the default buffer size. */
    public SubmissionPublisher() {
        this(ForkJoinPool.commonPool(), Flow.defaultBufferSize(), null);
    }

    // ---- subscribing ----

    /**
     * Registers {@code subscriber}, which gets its own buffer and its own {@link Flow.Subscription}.
     *
     * <p>Subscribing to a publisher that is already closed is not an error: the subscriber still
     * gets its {@code onSubscribe}, and then the terminal signal the publisher closed with. That is
     * the protocol — a subscriber always sees {@code onSubscribe} first.
     */
    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        this.doSubscribe(subscriber);
    }

    // The body of subscribe, under a name of its own so that consume() can reach it: calling
    // `subscribe` from inside this class is reported as ambiguous between the interface's
    // declaration and this class's override (finding #254).
    private void doSubscribe(Flow.Subscriber<?> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException();
        }
        PubSubscription<T> subscription = new PubSubscription<T>(
                subscriber, this.executor, this.maxBufferCapacity, this.onNextHandler);
        this.register(subscription);
        // Outside the monitor: onSubscribe is the subscriber's code, and it typically calls
        // request() straight back into the subscription.
        subscriber.onSubscribe(subscription);
        subscription.startIfClosed();
    }

    // Records the subscription. It is built by the caller so that the subscriber's own callback can
    // run afterwards with nothing held.
    private void register(PubSubscription<T> subscription) {
        Throwable failure = null;
        boolean late = false;
        synchronized (this.sync) {
            this.prune();
            if (this.closed) {
                late = true;
                failure = this.closedException;
            } else {
                this.subscriptions.add(subscription);
            }
        }
        if (late) {
            subscription.markComplete(failure);
        }
    }

    // Drops the subscriptions that have been cancelled. Called with `sync` held, from the operations
    // that walk the list anyway: a cancelled subscriber costs one more pass and never a callback
    // under a monitor.
    private void prune() {
        int i = this.subscriptions.size() - 1;
        while (i >= 0) {
            PubSubscription<T> subscription = this.subscriptions.get(i);
            if (subscription.isCancelled()) {
                this.subscriptions.remove(i);
            }
            i = i - 1;
        }
    }

    // A snapshot of the live subscriptions, so the caller can work on them with nothing held.
    private ArrayList<PubSubscription<T>> snapshot() {
        ArrayList<PubSubscription<T>> copy = new ArrayList<PubSubscription<T>>();
        synchronized (this.sync) {
            this.prune();
            int i = 0;
            while (i < this.subscriptions.size()) {
                copy.add(this.subscriptions.get(i));
                i = i + 1;
            }
        }
        return copy;
    }

    // ---- publishing ----

    /**
     * Publishes {@code item}, BLOCKING while any subscriber's buffer is full.
     *
     * @return the largest number of items any subscriber has buffered but not yet taken — how far
     *         behind the slowest one is
     */
    public int submit(T item) {
        if (item == null) {
            throw new NullPointerException();
        }
        ArrayList<PubSubscription<T>> live = this.snapshot();
        int i = 0;
        while (i < live.size()) {
            PubSubscription<T> subscription = live.get(i);
            subscription.put(item);
            i = i + 1;
        }
        return this.estimateMaximumLag();
    }

    /**
     * Publishes {@code item} without blocking, DROPPING it for any subscriber whose buffer is full.
     *
     * @param onDrop consulted before a drop; returning {@code true} asks for one more attempt, which
     *               is worth something because the subscriber may have drained in between
     * @return the lag, as {@link #submit} reports it, when nothing was dropped; otherwise the
     *         NEGATIVE of the number of subscribers that dropped it
     */
    public int offer(T item, BiPredicate<Flow.Subscriber<? super T>, ? super T> onDrop) {
        return this.offerFor(item, 0L, onDrop);
    }

    /**
     * Publishes {@code item}, waiting up to the given time for room before dropping it.
     *
     * @return as {@link #offer(Object, BiPredicate)}
     */
    public int offer(T item, long timeout, TimeUnit unit,
            BiPredicate<Flow.Subscriber<? super T>, ? super T> onDrop) {
        if (unit == null) {
            throw new NullPointerException();
        }
        long millis = unit.toMillis(timeout);
        return this.offerFor(item, millis, onDrop);
    }

    // The two offers differ only in how long a full buffer is waited on, so they share this.
    //
    // The parameter is a RAW BiPredicate, which is not how it should read: passing the caller's
    // `BiPredicate<Flow.Subscriber<? super T>, ? super T>` to a parameter of that very type is
    // rejected (finding #253 -- a wildcard-typed argument is capture-converted and then never
    // related back to the parameter it came from). Raw is the smallest workaround that keeps the
    // two public offer() signatures faithful to the JDK's.
    @SuppressWarnings("rawtypes")
    private int offerFor(T item, long millis, BiPredicate onDrop) {
        if (item == null) {
            throw new NullPointerException();
        }
        ArrayList<PubSubscription<T>> live = this.snapshot();
        int drops = 0;
        int i = 0;
        while (i < live.size()) {
            PubSubscription<T> subscription = live.get(i);
            boolean placed = subscription.offerItem(item, millis);
            if (!placed && onDrop != null) {
                Flow.Subscriber<? super T> subscriber = subscription.subscriber();
                if (this.askDrop(onDrop, subscriber, item)) {
                    placed = subscription.offerItem(item, 0L);
                }
            }
            if (!placed) {
                drops = drops + 1;
            }
            i = i + 1;
        }
        if (drops > 0) {
            return -drops;
        }
        return this.estimateMaximumLag();
    }

    // The one unchecked call the raw parameter of #253 costs: the predicate's real type is
    // BiPredicate<Flow.Subscriber<? super T>, ? super T>, and both arguments match it.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean askDrop(BiPredicate onDrop, Flow.Subscriber<?> subscriber, T item) {
        return onDrop.test(subscriber, item);
    }

    // ---- closing ----

    /**
     * Stops accepting items. Each subscriber still receives everything already buffered, and then
     * {@code onComplete} — closing is not cancelling.
     */
    @Override
    public void close() {
        ArrayList<PubSubscription<T>> live = this.markClosed(null);
        int i = 0;
        while (i < live.size()) {
            PubSubscription<T> subscription = live.get(i);
            subscription.markComplete(null);
            i = i + 1;
        }
    }

    /**
     * Stops accepting items and ends every subscription with {@code onError} after its buffer
     * drains.
     */
    public void closeExceptionally(Throwable error) {
        if (error == null) {
            throw new NullPointerException();
        }
        ArrayList<PubSubscription<T>> live = this.markClosed(error);
        int i = 0;
        while (i < live.size()) {
            PubSubscription<T> subscription = live.get(i);
            subscription.markComplete(error);
            i = i + 1;
        }
    }

    // Flips the flag and empties the list, handing back what was there. Closing twice is a no-op, so
    // the second call gets an empty list and signals nobody.
    private ArrayList<PubSubscription<T>> markClosed(Throwable error) {
        ArrayList<PubSubscription<T>> live = new ArrayList<PubSubscription<T>>();
        synchronized (this.sync) {
            if (!this.closed) {
                this.closed = true;
                this.closedException = error;
                int i = 0;
                while (i < this.subscriptions.size()) {
                    live.add(this.subscriptions.get(i));
                    i = i + 1;
                }
                this.subscriptions.clear();
            }
        }
        return live;
    }

    public boolean isClosed() {
        boolean shut;
        synchronized (this.sync) {
            shut = this.closed;
        }
        return shut;
    }

    /** The throwable this publisher was closed with, or {@code null} if it closed normally. */
    public Throwable getClosedException() {
        Throwable failure;
        synchronized (this.sync) {
            failure = this.closedException;
        }
        return failure;
    }

    // ---- what the publisher can tell you about its subscribers ----

    public boolean hasSubscribers() {
        ArrayList<PubSubscription<T>> live = this.snapshot();
        return live.size() > 0;
    }

    public int getNumberOfSubscribers() {
        ArrayList<PubSubscription<T>> live = this.snapshot();
        return live.size();
    }

    public Executor getExecutor() {
        return this.executor;
    }

    public int getMaxBufferCapacity() {
        return this.maxBufferCapacity;
    }

    /**
     * The current subscribers, in subscription order.
     *
     * <p>The list is built RAW and cast at the end, which is not how it should read. Adding a
     * wildcard-typed value to an {@code ArrayList<Flow.Subscriber<? super T>>} is finding #253
     * again, and here it does not even fail to compile: the {@code add} call is dropped in silence
     * and only its argument is evaluated and popped, so the method returned an empty list. A raw
     * list has no wildcard for the check to lose.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<Flow.Subscriber<? super T>> getSubscribers() {
        ArrayList<PubSubscription<T>> live = this.snapshot();
        ArrayList out = new ArrayList();
        int i = 0;
        while (i < live.size()) {
            PubSubscription<T> subscription = live.get(i);
            out.add(subscription.subscriber());
            i = i + 1;
        }
        return (List<Flow.Subscriber<? super T>>) out;
    }

    public boolean isSubscribed(Flow.Subscriber<? super T> subscriber) {
        ArrayList<PubSubscription<T>> live = this.snapshot();
        int i = 0;
        while (i < live.size()) {
            PubSubscription<T> subscription = live.get(i);
            if (subscription.subscriber() == subscriber) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * The smallest outstanding request across subscribers — how much room the slowest one still has
     * for items it has not been given yet.
     *
     * <p>It can come out NEGATIVE, and that is the useful part: a subscriber with items buffered
     * that it never asked for is behind by that many, so the number says how far the producer has
     * run ahead of the slowest consumer rather than clamping at zero and hiding it.
     */
    public long estimateMinimumDemand() {
        ArrayList<PubSubscription<T>> live = this.snapshot();
        long least = 0L;
        int i = 0;
        while (i < live.size()) {
            PubSubscription<T> subscription = live.get(i);
            long room = subscription.demand() - subscription.buffered();
            if (i == 0 || room < least) {
                least = room;
            }
            i = i + 1;
        }
        return least;
    }

    /** The largest number of items buffered for any one subscriber. */
    public int estimateMaximumLag() {
        ArrayList<PubSubscription<T>> live = this.snapshot();
        int most = 0;
        int i = 0;
        while (i < live.size()) {
            PubSubscription<T> subscription = live.get(i);
            int lag = subscription.buffered();
            if (lag > most) {
                most = lag;
            }
            i = i + 1;
        }
        return most;
    }

    /**
     * Subscribes {@code consumer} to every item, asking for them as fast as they come.
     *
     * <p>This is the escape hatch out of the protocol for a consumer that has no backpressure to
     * apply: it requests without bound, so nothing is ever buffered on its account.
     *
     * @return a future that completes when the publisher closes, or completes exceptionally with
     *         whatever it closed with
     */
    public CompletableFuture<Void> consume(Consumer<? super T> consumer) {
        if (consumer == null) {
            throw new NullPointerException();
        }
        CompletableFuture<Void> done = new CompletableFuture<Void>();
        ConsumeSubscriber<T> subscriber = new ConsumeSubscriber<T>(consumer, done);
        this.doSubscribe(subscriber);
        return done;
    }
}


/**
 * One subscriber's buffer, demand counter and delivery loop.
 *
 * <p>Everything that touches the buffer is inside a {@code synchronized (sync)} block; nothing that
 * calls the subscriber, or the executor, is. That split is the entire concurrency design: {@link
 * #take} hands one item out under the monitor, and {@link #deliver} — which runs on the executor —
 * calls {@code onNext} with the monitor released.
 */
final class PubSubscription<T> implements Flow.Subscription {

    private final Object sync = new Object();

    private final Flow.Subscriber<T> target;
    private final Executor executor;
    private final int capacity;
    private final BiConsumer<? super Flow.Subscriber<? super T>, ? super Throwable> onNextHandler;

    // A ring: items enter at `tail` and leave at `head`, and `count` is what distinguishes full
    // from empty when the two meet.
    private final Object[] buffer;
    private int head;
    private int tail;
    private int count;

    private long demand;
    private boolean cancelled;
    // Set when the publisher closes: the terminal signal is owed, but only once the buffer drains.
    private boolean completing;
    private Throwable error;
    private boolean terminated;
    // One delivery loop at a time, so items reach the subscriber in order.
    private boolean delivering;

    @SuppressWarnings("unchecked")
    PubSubscription(Flow.Subscriber<?> subscriber, Executor executor, int capacity,
            BiConsumer<? super Flow.Subscriber<? super T>, ? super Throwable> onNextHandler) {
        this.target = (Flow.Subscriber<T>) subscriber;
        this.executor = executor;
        this.capacity = capacity;
        this.onNextHandler = onNextHandler;
        this.buffer = new Object[capacity];
        this.head = 0;
        this.tail = 0;
        this.count = 0;
        this.demand = 0L;
        this.cancelled = false;
        this.completing = false;
        this.error = null;
        this.terminated = false;
        this.delivering = false;
    }

    @SuppressWarnings("unchecked")
    Flow.Subscriber<? super T> subscriber() {
        return (Flow.Subscriber<? super T>) this.target;
    }

    boolean isCancelled() {
        boolean gone;
        synchronized (this.sync) {
            gone = this.cancelled;
        }
        return gone;
    }

    long demand() {
        long owed;
        synchronized (this.sync) {
            owed = this.demand;
        }
        return owed;
    }

    int buffered() {
        int held;
        synchronized (this.sync) {
            held = this.count;
        }
        return held;
    }

    // ---- the producer's side ----

    /** Adds `item`, waiting for room. Gives up only if the subscription is cancelled or closed. */
    void put(T item) {
        boolean start = false;
        synchronized (this.sync) {
            boolean interrupted = false;
            while (this.count == this.capacity && !this.cancelled && !this.completing
                    && !interrupted) {
                try {
                    this.sync.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    interrupted = true;
                }
            }
            if (!interrupted && !this.cancelled && !this.completing) {
                start = this.store(item);
            }
        }
        if (start) {
            this.launch();
        }
    }

    /**
     * Adds `item` if there is room, waiting at most `millis` for some to appear.
     *
     * @return false if it did not fit, which the publisher reports as a drop
     */
    boolean offerItem(T item, long millis) {
        boolean placed = false;
        boolean start = false;
        synchronized (this.sync) {
            long left = millis;
            boolean interrupted = false;
            while (this.count == this.capacity && !this.cancelled && !this.completing && left > 0L
                    && !interrupted) {
                long before = System.currentTimeMillis();
                try {
                    this.sync.wait(left);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    interrupted = true;
                }
                long spent = System.currentTimeMillis() - before;
                if (spent <= 0L) {
                    left = 0L;
                } else {
                    left = left - spent;
                }
            }
            if (!interrupted && !this.cancelled && !this.completing
                    && this.count < this.capacity) {
                start = this.store(item);
                placed = true;
            }
        }
        if (start) {
            this.launch();
        }
        return placed;
    }

    // Both entry points end here, with `sync` already held. Reports whether a delivery loop is owed,
    // because starting one means calling the executor, which must not happen under the monitor.
    private boolean store(T item) {
        this.buffer[this.tail] = item;
        this.tail = (this.tail + 1) % this.capacity;
        this.count = this.count + 1;
        return this.claimRound();
    }

    /** Marks the terminal signal as owed. The subscriber still gets whatever is buffered first. */
    void markComplete(Throwable failure) {
        boolean start = false;
        synchronized (this.sync) {
            if (!this.completing) {
                this.completing = true;
                this.error = failure;
                this.sync.notifyAll();
                start = this.claimRound();
            }
        }
        if (start) {
            this.launch();
        }
    }

    /** Kicks the loop for a subscription that was already closed when it was created. */
    void startIfClosed() {
        boolean start = false;
        synchronized (this.sync) {
            if (this.completing) {
                start = this.claimRound();
            }
        }
        if (start) {
            this.launch();
        }
    }

    // ---- the consumer's side ----

    @Override
    public void request(long n) {
        boolean start = false;
        synchronized (this.sync) {
            if (!this.cancelled) {
                if (n <= 0L) {
                    // A protocol error is reported through the subscriber, not thrown at the caller:
                    // the caller here IS the subscriber, usually from inside onSubscribe.
                    this.completing = true;
                    this.error = new IllegalArgumentException("request must be positive");
                    this.count = 0;
                } else {
                    long raised = this.demand + n;
                    if (raised < 0L) {
                        // Saturate rather than wrap: an unbounded request is the normal way to say
                        // "everything".
                        raised = 9223372036854775807L;
                    }
                    this.demand = raised;
                }
                start = this.claimRound();
            }
        }
        if (start) {
            this.launch();
        }
    }

    @Override
    public void cancel() {
        synchronized (this.sync) {
            this.cancelled = true;
            this.count = 0;
            this.sync.notifyAll();
        }
    }

    // Whether this caller is the one that gets to run the delivery loop. Called with `sync` held.
    private boolean claimRound() {
        boolean mine = false;
        if (!this.delivering) {
            boolean owed = this.count > 0 && this.demand > 0L;
            if (owed || this.completing) {
                this.delivering = true;
                mine = true;
            }
        }
        return mine;
    }

    // Hands the loop to the executor, with NO monitor held: a direct executor runs the task right
    // here, and that task takes the monitor.
    private void launch() {
        this.executor.execute(new DeliveryTask<T>(this));
    }

    /**
     * Takes the next item to deliver, or null when there is nothing to hand out right now.
     *
     * <p>Deliberately tiny: it is the only thing the delivery loop does with the monitor held.
     */
    @SuppressWarnings("unchecked")
    T take() {
        Object item = null;
        synchronized (this.sync) {
            if (!this.cancelled && this.count > 0 && this.demand > 0L) {
                item = this.buffer[this.head];
                this.buffer[this.head] = null;
                this.head = (this.head + 1) % this.capacity;
                this.count = this.count - 1;
                this.demand = this.demand - 1L;
                // A producer may be parked on a full buffer.
                this.sync.notifyAll();
            }
        }
        return (T) item;
    }

    // Whether the terminal signal is due: the publisher closed and there is nothing left to deliver.
    private boolean terminalDue() {
        boolean due = false;
        synchronized (this.sync) {
            boolean more = this.count > 0 && this.demand > 0L;
            if (!this.cancelled && !this.terminated && this.completing && !more) {
                this.terminated = true;
                due = true;
            }
        }
        return due;
    }

    private Throwable failure() {
        Throwable failed;
        synchronized (this.sync) {
            failed = this.error;
        }
        return failed;
    }

    // Lets the next producer or requester start a fresh loop.
    private void finishRound() {
        synchronized (this.sync) {
            this.delivering = false;
        }
    }

    private void cancelOnHandlerFailure() {
        synchronized (this.sync) {
            this.cancelled = true;
            this.count = 0;
            this.sync.notifyAll();
        }
    }

    /**
     * The delivery loop, run on the executor with NO monitor held.
     *
     * <p>A subscriber that throws out of {@code onNext} has broken the protocol. The handler given
     * to the publisher gets to see it; with no handler, the subscription is cancelled, which is the
     * only other honest option — carrying on would deliver to a subscriber that just failed.
     */
    void deliver() {
        boolean broke = false;
        T item = this.take();
        while (item != null && !broke) {
            try {
                this.target.onNext(item);
            } catch (RuntimeException failure) {
                if (this.onNextHandler != null) {
                    Flow.Subscriber<? super T> who = this.subscriber();
                    this.onNextHandler.accept(who, failure);
                }
                this.cancelOnHandlerFailure();
                broke = true;
            }
            if (!broke) {
                item = this.take();
            }
        }
        if (!broke && this.terminalDue()) {
            Throwable failed = this.failure();
            if (failed == null) {
                this.target.onComplete();
            } else {
                this.target.onError(failed);
            }
        }
        this.finishRound();
    }
}


/**
 * The delivery loop as a Runnable.
 *
 * <p>A named class and not a lambda: the executor needs a {@link Runnable}, and this file stays
 * inside the subset of the language the rest of KajiLibrary is written in.
 */
final class DeliveryTask<T> implements Runnable {

    private final PubSubscription<T> subscription;

    DeliveryTask(PubSubscription<T> subscription) {
        this.subscription = subscription;
    }

    @Override
    public void run() {
        this.subscription.deliver();
    }
}


/**
 * The subscriber {@link SubmissionPublisher#consume} installs: requests without bound, forwards
 * every item to the consumer, and completes the future when the stream ends.
 */
final class ConsumeSubscriber<T> implements Flow.Subscriber<T> {

    private final Consumer<? super T> consumer;
    private final CompletableFuture<Void> done;

    ConsumeSubscriber(Consumer<? super T> consumer, CompletableFuture<Void> done) {
        this.consumer = consumer;
        this.done = done;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        // Everything, as fast as it comes: this consumer has no backpressure to apply.
        subscription.request(9223372036854775807L);
    }

    @Override
    public void onNext(T item) {
        this.consumer.accept(item);
    }

    @Override
    public void onError(Throwable throwable) {
        this.done.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        this.done.complete(null);
    }
}
