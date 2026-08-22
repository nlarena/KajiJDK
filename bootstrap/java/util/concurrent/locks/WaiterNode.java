package java.util.concurrent.locks;

// A node in an AbstractQueuedSynchronizer's waiter stack — one parked thread.
class WaiterNode {
    volatile Thread thread;
    volatile WaiterNode next;
}
