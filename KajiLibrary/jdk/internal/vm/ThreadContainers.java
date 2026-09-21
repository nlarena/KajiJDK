package jdk.internal.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * KajiLibrary's jdk.internal.vm.ThreadContainers -- the registry of live {@link ThreadContainer}s.
 *
 * <p>A container knows which threads it has, but nobody knows which containers there are: both
 * things are needed for a diagnostic tool to be able to walk the whole tree of a process. This
 * registry is the second half.
 *
 * <p>The registry is **weak by design in the JDK** --it keeps weak references, so a container
 * nobody uses is collected and disappears by itself--. Here a synchronised list with an explicit
 * {@link #deregisterContainer} is used. The note gave as the reason that this library has no weak
 * references with a queue; it does now (`java.lang.ref.WeakReference` and `ReferenceQueue`, with
 * the GC enqueuing cleared referents), so that reason no longer holds and the JDK's weak registry
 * could be written. The visible difference remains while it is not: a container that is abandoned
 * without being closed **stays in the registry**. `SharedThreadContainer.close()` deregisters it,
 * which is the normal road.
 *
 * <p>{@link #root()} is not a real registered container, but the one that represents "everything
 * that is not inside any": the platform threads that started on their own.
 */
public class ThreadContainers {

    private static final List<ThreadContainer> REGISTERED = new ArrayList<ThreadContainer>();
    private static final Object LOCK = new Object();
    private static final ThreadContainer ROOT = new RootContainer();

    private ThreadContainers() {
    }

    /**
     * Whether **all** the threads can be found or only the ones that are inside a container.
     *
     * <p>Here it is `false`, and reaching that answer took a test. The first version returned
     * `true` on the argument that {@link #root()} enumerates the whole platform by walking the root
     * `ThreadGroup`. **That argument is false on this VM**: `ThreadGroup` keeps no record of its
     * members --it has an `addThread` seam, but nothing calls it--, so `activeCount()` gives 0 and
     * `enumerate()` returns nothing, even with threads running.
     *
     * <p>So a thread that started on its own, outside every container, cannot be found. Saying
     * `false` is what allows whoever asks to know that the walk is going to be incomplete, instead
     * of believing they saw everything.
     */
    public static boolean trackAllThreads() {
        return false;
    }

    /**
     * It registers a container and returns the **key** for deregistering it.
     *
     * <p>The key is typed as an opaque `Object` so that the caller treats it as a key and hands it
     * back to {@link #deregisterContainer} instead of relying on what it is. The note here said it
     * was returned "instead of the container itself"; it **is** the container itself. The JDK's key
     * is a weak reference to the container, and the `Object` type is what would let this one become
     * that without touching any caller.
     */
    public static Object registerContainer(ThreadContainer container) {
        if (container == null) {
            throw new NullPointerException("container");
        }
        synchronized (ThreadContainers.LOCK) {
            ThreadContainers.REGISTERED.add(container);
        }
        return container;
    }

    /** It deregisters what {@link #registerContainer} returned. */
    public static void deregisterContainer(Object key) {
        synchronized (ThreadContainers.LOCK) {
            ThreadContainers.REGISTERED.remove(key);
        }
    }

    /** The root container: the threads that are not inside any. */
    public static ThreadContainer root() {
        return ThreadContainers.ROOT;
    }

    /** The container that encloses `container`, deduced from the stack of scopes. */
    static ThreadContainer parent(ThreadContainer container) {
        ThreadContainer enclosing = container.enclosingScope(ThreadContainer.class);
        if (enclosing != null) {
            return enclosing;
        }
        return container == ThreadContainers.ROOT ? null : ThreadContainers.ROOT;
    }

    /** The registered containers whose parent is `container`. */
    static Stream<ThreadContainer> children(ThreadContainer container) {
        List<ThreadContainer> children = new ArrayList<ThreadContainer>();
        synchronized (ThreadContainers.LOCK) {
            for (ThreadContainer c : ThreadContainers.REGISTERED) {
                if (ThreadContainers.parent(c) == container) {
                    children.add(c);
                }
            }
        }
        return children.stream();
    }

    /**
     * The container where `thread` is, or the root one if it is in none.
     *
     * <p>It is found by asking each registered container whether it has that thread. The JDK reads
     * it from a field of the `Thread` itself, which is O(1); here `Thread` cannot be touched from
     * this package, and the answer is the same.
     */
    public static ThreadContainer container(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("thread");
        }
        synchronized (ThreadContainers.LOCK) {
            for (ThreadContainer c : ThreadContainers.REGISTERED) {
                if (c.threads().anyMatch(t -> t == thread)) {
                    return c;
                }
            }
        }
        return ThreadContainers.ROOT;
    }

    // The root container: it represents "the threads that are not inside any container".
    //
    // It finds them by walking the root thread group, and **on this VM that finds little**:
    // `ThreadGroup` keeps no record of its members (`enumerate` returns 0 even with live threads),
    // so in practice the walk gives the asking thread and nothing else. The walk is left all the
    // same --the day `ThreadGroup` keeps a record, this starts giving the complete answer without
    // touching anything-- and at least the current thread is guaranteed, because a container that
    // says it has zero threads while one is calling it would be lying. `trackAllThreads()` returns
    // `false` for this very reason.
    private static final class RootContainer extends ThreadContainer {

        RootContainer() {
            super(true);
        }

        public String name() {
            return "<root>";
        }

        public ThreadContainer parent() {
            return null;
        }

        public Stream<Thread> threads() {
            ThreadGroup g = Thread.currentThread().getThreadGroup();
            while (g != null && g.getParent() != null) {
                g = g.getParent();
            }
            if (g == null) {
                return Stream.of(Thread.currentThread());
            }
            Thread[] buf = new Thread[g.activeCount() + 8];
            int n = g.enumerate(buf, true);
            List<Thread> live = new ArrayList<Thread>();
            for (int i = 0; i < n; i++) {
                if (buf[i] != null) {
                    live.add(buf[i]);
                }
            }
            Thread self = Thread.currentThread();
            if (!live.contains(self)) {
                live.add(self);
            }
            return live.stream();
        }
    }
}
