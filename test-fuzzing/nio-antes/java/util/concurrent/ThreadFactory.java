package java.util.concurrent;

// The one place a framework asks for a thread instead of writing `new Thread(r)`. Handing
// creation to a factory is what lets a pool's threads be named, grouped or made daemons
// without the pool knowing anything about that policy — the pool asks for a thread, the
// application decides what kind of thread it gets.
public interface ThreadFactory {

    // Build a thread for `r`, or return null if the factory declines to create one.
    Thread newThread(Runnable r);
}
