package java.util.concurrent;

// An element that is not ready yet: it says how long remains before it becomes available.
// This is what turns a priority queue into a *scheduler* — order by remaining delay and the
// head is always the next thing due, and it may only be handed out once its delay is gone.
//
// The {@link Comparable} half is not decoration: a DelayQueue orders elements by it, and an
// implementation whose compareTo disagrees with its getDelay would hand out elements in the
// wrong order. The conventional implementation compares the two delays.
public interface Delayed extends Comparable<Delayed> {

    // Time remaining, in the given unit; zero or negative means the delay has elapsed.
    long getDelay(TimeUnit unit);
}
