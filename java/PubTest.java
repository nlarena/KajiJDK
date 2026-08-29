import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Exercises SubmissionPublisher. Every method returns the number of things that came out wrong,
 * so 0 is a pass.
 *
 * It runs on OUR VM through run-headless, and the SAME source compiles against the JDK, where
 * `main` prints the same counts — so the two can be compared instead of trusted.
 *
 * The executor is DIRECT (delivery on the calling thread) on purpose: it makes the buffer and the
 * demand counter observable step by step, and keeps the test from measuring the thread pool.
 */
public class PubTest {

    // Subscribing through the Flow.Publisher view, because calling `subscribe` on the class that
    // overrides it is reported as ambiguous (finding #254).
    static void suscribir(SubmissionPublisher<String> pub, Flow.Subscriber<String> sub) {
        Flow.Publisher<String> as = pub;
        as.subscribe(sub);
    }

    /** Publish 4 items to a subscriber that asked for all of them, then close. */
    public static int basico() {
        DirectExecutor pool = new DirectExecutor();
        SubmissionPublisher<String> pub = new SubmissionPublisher<String>(pool, 8);
        Recorder rec = new Recorder(100L);
        PubTest.suscribir(pub, rec);
        int bad = 0;
        if (!rec.subscribed()) {
            bad = bad + 1;
        }
        if (pub.getNumberOfSubscribers() != 1) {
            bad = bad + 1;
        }
        if (!pub.hasSubscribers()) {
            bad = bad + 1;
        }
        pub.submit("a");
        pub.submit("b");
        pub.submit("c");
        pub.submit("d");
        if (!rec.seen().equals("a,b,c,d,")) {
            bad = bad + 1;
        }
        if (rec.completed()) {
            bad = bad + 1;
        }
        pub.close();
        if (!rec.completed()) {
            bad = bad + 1;
        }
        if (!pub.isClosed()) {
            bad = bad + 1;
        }
        if (pub.getClosedException() != null) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Nothing arrives before it is requested: that is the whole protocol. */
    public static int demanda() {
        DirectExecutor pool = new DirectExecutor();
        SubmissionPublisher<String> pub = new SubmissionPublisher<String>(pool, 8);
        Recorder rec = new Recorder(0L);
        PubTest.suscribir(pub, rec);
        int bad = 0;
        pub.submit("a");
        pub.submit("b");
        if (!rec.seen().equals("")) {
            bad = bad + 1;
        }
        if (pub.estimateMaximumLag() != 2) {
            bad = bad + 1;
        }
        // Negative on purpose: two items are buffered that the subscriber never asked for.
        if (pub.estimateMinimumDemand() != -2L) {
            bad = bad + 1;
        }
        rec.ask(1L);
        if (!rec.seen().equals("a,")) {
            bad = bad + 1;
        }
        if (pub.estimateMaximumLag() != 1) {
            bad = bad + 1;
        }
        rec.ask(5L);
        if (!rec.seen().equals("a,b,")) {
            bad = bad + 1;
        }
        if (pub.estimateMaximumLag() != 0) {
            bad = bad + 1;
        }
        if (pub.estimateMinimumDemand() != 4L) {
            bad = bad + 1;
        }
        return bad;
    }

    /** A full buffer: offer drops instead of blocking, and says how many it dropped. */
    public static int rebalse() {
        DirectExecutor pool = new DirectExecutor();
        SubmissionPublisher<String> pub = new SubmissionPublisher<String>(pool, 2);
        Recorder rec = new Recorder(0L);
        PubTest.suscribir(pub, rec);
        int bad = 0;
        PubCounter drops = new PubCounter();
        DropCounter onDrop = new DropCounter(drops);
        int first = pub.offer("a", onDrop);
        int second = pub.offer("b", onDrop);
        if (first != 1 || second != 2) {
            bad = bad + 1;
        }
        if (drops.value() != 0) {
            bad = bad + 1;
        }
        int third = pub.offer("c", onDrop);
        if (third != -1) {
            bad = bad + 1;
        }
        // The handler was consulted once, and the retry it asked for failed too.
        if (drops.value() != 1) {
            bad = bad + 1;
        }
        rec.ask(10L);
        if (!rec.seen().equals("a,b,")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Two subscribers at different paces: the slow one must not hold the fast one back. */
    public static int variosSuscriptores() {
        DirectExecutor pool = new DirectExecutor();
        SubmissionPublisher<String> pub = new SubmissionPublisher<String>(pool, 8);
        Recorder fast = new Recorder(100L);
        Recorder slow = new Recorder(1L);
        PubTest.suscribir(pub, fast);
        PubTest.suscribir(pub, slow);
        int bad = 0;
        if (pub.getNumberOfSubscribers() != 2) {
            bad = bad + 1;
        }
        pub.submit("a");
        pub.submit("b");
        pub.submit("c");
        if (!fast.seen().equals("a,b,c,")) {
            bad = bad + 1;
        }
        if (!slow.seen().equals("a,")) {
            bad = bad + 1;
        }
        if (pub.estimateMaximumLag() != 2) {
            bad = bad + 1;
        }
        if (pub.estimateMinimumDemand() != -2L) {
            bad = bad + 1;
        }
        List<Flow.Subscriber<? super String>> who = pub.getSubscribers();
        if (who.size() != 2) {
            bad = bad + 1;
        }
        if (!pub.isSubscribed(fast) || !pub.isSubscribed(slow)) {
            bad = bad + 1;
        }
        // The slow one leaves; the fast one carries on.
        slow.stop();
        pub.submit("d");
        if (!fast.seen().equals("a,b,c,d,")) {
            bad = bad + 1;
        }
        if (pub.getNumberOfSubscribers() != 1) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Closing with a throwable ends every subscription with onError, after it drains. */
    public static int cierreConError() {
        DirectExecutor pool = new DirectExecutor();
        SubmissionPublisher<String> pub = new SubmissionPublisher<String>(pool, 8);
        Recorder rec = new Recorder(100L);
        PubTest.suscribir(pub, rec);
        int bad = 0;
        pub.submit("a");
        IllegalStateException boom = new IllegalStateException("se rompio");
        pub.closeExceptionally(boom);
        if (!rec.seen().equals("a,")) {
            bad = bad + 1;
        }
        if (rec.completed()) {
            bad = bad + 1;
        }
        if (!rec.failed()) {
            bad = bad + 1;
        }
        if (pub.getClosedException() != boom) {
            bad = bad + 1;
        }
        // Subscribing to a closed publisher still gets onSubscribe, then the terminal signal.
        Recorder late = new Recorder(100L);
        PubTest.suscribir(pub, late);
        if (!late.subscribed()) {
            bad = bad + 1;
        }
        if (!late.failed()) {
            bad = bad + 1;
        }
        return bad;
    }

    /** consume() asks for everything and forwards it, with no protocol for the caller to drive. */
    public static int consumo() {
        DirectExecutor pool = new DirectExecutor();
        SubmissionPublisher<String> pub = new SubmissionPublisher<String>(pool, 8);
        Collector sink = new Collector();
        // The future is held in a typed local, not discarded: a file that never NAMES the returned
        // type gets the call compiled with an `Object` return descriptor and it fails to resolve at
        // run time (finding #251). Checking that it completes is worth a line anyway.
        CompletableFuture<Void> done = pub.consume(sink);
        int bad = 0;
        pub.submit("a");
        pub.submit("b");
        if (!sink.seen().equals("a,b,")) {
            bad = bad + 1;
        }
        if (pub.estimateMaximumLag() != 0) {
            bad = bad + 1;
        }
        if (done.isDone()) {
            bad = bad + 1;
        }
        pub.close();
        if (!done.isDone()) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Everything at once, so one call answers "does it work". */
    public static int todo() {
        return PubTest.basico() + PubTest.demanda() + PubTest.rebalse()
                + PubTest.variosSuscriptores() + PubTest.cierreConError() + PubTest.consumo();
    }

    public static void main(String[] args) {
        System.out.println("basico              " + PubTest.basico());
        System.out.println("demanda             " + PubTest.demanda());
        System.out.println("rebalse             " + PubTest.rebalse());
        System.out.println("variosSuscriptores  " + PubTest.variosSuscriptores());
        System.out.println("cierreConError      " + PubTest.cierreConError());
        System.out.println("consumo             " + PubTest.consumo());
        System.out.println("TOTAL               " + PubTest.todo());
    }
}


/** Runs the task right here, so the test observes one deterministic order. */
final class DirectExecutor implements Executor {

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}


/** A subscriber that records what it was given, and asks for exactly what the test tells it to. */
final class Recorder implements Flow.Subscriber<String> {

    private final long initialRequest;
    private final StringBuilder log;
    private Flow.Subscription channel;
    private boolean gotSubscribe;
    private boolean gotComplete;
    private boolean gotError;

    Recorder(long initialRequest) {
        this.initialRequest = initialRequest;
        this.log = new StringBuilder();
        this.channel = null;
        this.gotSubscribe = false;
        this.gotComplete = false;
        this.gotError = false;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.channel = subscription;
        this.gotSubscribe = true;
        if (this.initialRequest > 0L) {
            subscription.request(this.initialRequest);
        }
    }

    @Override
    public void onNext(String item) {
        this.log.append(item);
        this.log.append(',');
    }

    @Override
    public void onError(Throwable throwable) {
        this.gotError = true;
    }

    @Override
    public void onComplete() {
        this.gotComplete = true;
    }

    void ask(long n) {
        this.channel.request(n);
    }

    void stop() {
        this.channel.cancel();
    }

    String seen() {
        return this.log.toString();
    }

    boolean subscribed() {
        return this.gotSubscribe;
    }

    boolean completed() {
        return this.gotComplete;
    }

    boolean failed() {
        return this.gotError;
    }
}


/** A mutable int the drop handler can bump, since a lambda capturing a local is not an option here. */
// El auxiliar lleva el prefijo del probe: `java/` es un paquete por defecto **plano**, asi
// que dos fuentes que declaren la misma clase escriben el mismo `.class` y gana la ultima
// compilada -- el resultado de la suite pasa a depender del orden de compilacion (#273).
final class PubCounter {

    private int n;

    PubCounter() {
        this.n = 0;
    }

    void bump() {
        this.n = this.n + 1;
    }

    int value() {
        return this.n;
    }
}


/** Counts drops and always asks for one retry, so the retry path is exercised too. */
final class DropCounter implements BiPredicate<Flow.Subscriber<? super String>, String> {

    private final PubCounter counter;

    DropCounter(PubCounter counter) {
        this.counter = counter;
    }

    @Override
    public boolean test(Flow.Subscriber<? super String> subscriber, String item) {
        this.counter.bump();
        return true;
    }
}


/** The sink consume() feeds. */
final class Collector implements Consumer<String> {

    private final StringBuilder log;

    Collector() {
        this.log = new StringBuilder();
    }

    @Override
    public void accept(String item) {
        this.log.append(item);
        this.log.append(',');
    }

    String seen() {
        return this.log.toString();
    }
}
