package java.util.concurrent.locks;

// A node of an `AbstractQueued(Long)Synchronizer`'s wait queue: **a blocked thread**. A
// package-private, top-level class --not a nested one-- because both synchronizers and each one's
// `ConditionObject` share it; none of this reaches the public contract.
//
// There are **two** monitors in play and they are worth not confusing:
//
//   - the synchronizer's internal monitor guards the queue's links (`prev`, `next`,
//     `inQueue`, `shared`) and the `state`;
//   - **this node's** monitor (`synchronized (node)`) guards `released`, which is the node's permit,
//     and is where the thread sleeps.
//
// That separation is what makes the lost wake-up impossible: the releaser does
// `synchronized (n) { n.released = true; n.notifyAll(); }` and the waiter checks `released`
// **under the same monitor** before falling asleep. If the signal arrived first, the flag is already
// set and the thread never gets to sleep; if it arrives later, the `notifyAll` finds it asleep.
// There is no window between the two because both happen inside the same monitor.
//
// And that is why the node blocks with `Object.wait()` and not with `LockSupport.park()`, even
// though the JDK uses the latter: `wait` has a **timed** form and throws `InterruptedException`
// where the contract says it should. Our VM's `park` has no deadline, and it also *throws*
// `InterruptedException` instead of returning (see `LockSupport`'s header).
final class SyncWaiter {

    // The thread waiting on this node; `null` once it has left the queue.
    Thread thread;

    // The FIFO queue's links. They are guarded by the synchronizer's internal monitor.
    SyncWaiter next;
    SyncWaiter prev;

    // Whether the node waits in shared mode (`acquireShared`) or exclusive (`acquire`).
    boolean shared;

    // Whether it is still in the queue. It keeps a repeated dequeue --the one a `tryAcquireNanos`
    // does when it expires just as it was about to be served-- from breaking the links.
    boolean inQueue;

    // The node's permit. It is guarded by **this node's** monitor.
    boolean released;
}
