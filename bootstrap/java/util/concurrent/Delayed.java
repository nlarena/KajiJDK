package java.util.concurrent;

// Minimal java.util.concurrent.Delayed — an element that becomes takeable only after a delay.
// `getDelay()` is the remaining time in **nanoseconds** (<= 0 means ready now); `compareTo`
// (from Comparable) orders by absolute expiration so a DelayQueue can heap them. (Simplified vs.
// the JDK: `getDelay()` takes no `TimeUnit` — it is always nanos.)
public interface Delayed extends Comparable<Delayed> {
    long getDelay();
}
