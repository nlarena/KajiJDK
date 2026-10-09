package jdk.internal.vm;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * KajiLibrary's jdk.internal.vm.SharedThreadContainer -- a container **with no owner**.
 *
 * <p>The difference with an ordinary {@link ThreadContainer} is in the name: this one does not
 * belong to the thread that created it. It is what a thread pool needs, where the one that opens
 * the container and the ones that run inside have nothing to do with each other, and where anybody
 * may close it.
 *
 * <p>That is why {@link #owner()} gives `null` and why it is {@link AutoCloseable}: its lifetime is
 * not tied to a call that starts and ends, so it has to be closed by hand --or with
 * `try`-with-resources, which is the way of not forgetting--.
 *
 * <p>The set of threads is a keys-only {@link ConcurrentHashMap}, and it has to be really
 * concurrent: the threads come in and go out by themselves, in parallel, with nothing serialising
 * them.
 */
public class SharedThreadContainer extends ThreadContainer implements AutoCloseable {

    private final String name;
    private final Set<Thread> threads = ConcurrentHashMap.newKeySet();
    private volatile Object key;
    private volatile boolean closed;

    private SharedThreadContainer(String name) {
        super(true);
        this.name = name;
    }

    /**
     * It creates one nested in `parent`.
     *
     * <p>The `parent` is accepted and **not kept**, and it is as well to say why nothing is lost:
     * the parent of a container comes from the stack of scopes where it was created ({@link
     * ThreadContainers}), not from a field. Keeping it as well would open the possibility of the
     * two saying different things.
     */
    public static SharedThreadContainer create(ThreadContainer parent, String name) {
        return SharedThreadContainer.create(name);
    }

    /** It creates one and registers it. */
    public static SharedThreadContainer create(String name) {
        SharedThreadContainer c = new SharedThreadContainer(name);
        c.key = ThreadContainers.registerContainer(c);
        return c;
    }

    public String name() {
        return this.name;
    }

    /** Always `null`: it is shared, it has no owner. */
    public Thread owner() {
        return null;
    }

    public void onStart(Thread thread) {
        this.threads.add(thread);
    }

    public void onExit(Thread thread) {
        this.threads.remove(thread);
    }

    public Stream<Thread> threads() {
        return this.threads.stream();
    }

    /**
     * It starts a thread inside this container.
     *
     * <p>It is noted **before** starting it and taken out if the start fails. The order matters:
     * the other way round there would be a window in which the thread is already running and the
     * container does not know it yet, and in that window `threadCount()` would lie.
     *
     * @throws IllegalStateException if the container has already been closed
     */
    public void start(Thread thread) {
        if (this.closed) {
            throw new IllegalStateException("this container has already been closed");
        }
        this.onStart(thread);
        try {
            thread.start();
        } catch (RuntimeException e) {
            this.onExit(thread);
            throw e;
        }
    }

    /**
     * It closes the container and takes it out of the registry.
     *
     * <p>**It neither waits for the threads nor interrupts them**, just like the JDK: closing is
     * saying "nobody else comes in", not "finish". Waiting is the responsibility of whoever opened
     * the scope, which is the only one that knows what their having finished means. Closing twice
     * does nothing.
     */
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        ThreadContainers.deregisterContainer(this.key);
    }

    /** Closing a shared container cannot fail, so the hook says yes. */
    protected boolean tryClose() {
        this.close();
        return true;
    }
}
