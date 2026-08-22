package java.util.concurrent;

// A two-party rendezvous that swaps objects: each thread calls {@link #exchange} with the
// value it offers and receives the partner's, both returning only once the pair has met.
//
// The JDK uses a lock-free arena of slots to spread contention across many pairs. KajiJDK
// exchanges through a single parked slot guarded by the intrinsic monitor of a private
// `sync` object: the first arriver parks its value and waits, the second takes it and
// hands its own back. Extra threads wait for the in-flight handshake to finish, so pairs
// are matched one at a time — the same semantics, without the arena's parallelism.
//
// Single-exit style throughout (finding #105). `throws InterruptedException` is omitted (no
// interruption in KajiJDK); TimeoutException is declared, since it is genuinely raised and
// our javac enforces checked exceptions.
public class Exchanger<V> {

    private final Object sync = new Object();
    // The value parked by the first arriver, and whether one is parked.
    private Object item;
    private boolean waiting;
    // The partner's value handed back to the parked thread, and whether it has been.
    private Object taken;
    private boolean handed;

    public Exchanger() {
    }

    // Offer `x` and receive the partner's value, blocking until a partner arrives.
    public V exchange(V x) {
        Object result;
        synchronized (sync) {
            // Let any in-flight handshake finish before joining as a new party.
            while (handed) {
                sync.wait();
            }
            if (!waiting) {
                // First arriver: park the offer and wait for a partner to take it.
                item = x;
                waiting = true;
                sync.notifyAll();
                while (!handed) {
                    sync.wait();
                }
                result = taken;
                // Clear the slot and let any queued party start a fresh handshake.
                item = null;
                taken = null;
                waiting = false;
                handed = false;
                sync.notifyAll();
            } else {
                // Second arriver: take the parked value and hand this one back.
                result = item;
                taken = x;
                handed = true;
                sync.notifyAll();
            }
        }
        return (V) result;
    }

    // Like {@link #exchange}, but gives up after the timeout with a TimeoutException,
    // withdrawing the offer so no later partner can complete a stale handshake.
    public V exchange(V x, long timeout, TimeUnit unit) throws TimeoutException {
        Object result;
        synchronized (sync) {
            while (handed) {
                sync.wait();
            }
            if (!waiting) {
                item = x;
                waiting = true;
                sync.notifyAll();
                long ms = unit.toMillis(timeout);
                if (ms > 0L) {
                    sync.wait(ms);
                }
                if (!handed) {
                    // No partner arrived: withdraw the offer before giving up.
                    item = null;
                    waiting = false;
                    sync.notifyAll();
                    throw new TimeoutException();
                }
                result = taken;
                item = null;
                taken = null;
                waiting = false;
                handed = false;
                sync.notifyAll();
            } else {
                result = item;
                taken = x;
                handed = true;
                sync.notifyAll();
            }
        }
        return (V) result;
    }
}
