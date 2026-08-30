package java.lang;

/**
 * KajiLibrary's java.lang.BaseVirtualThread -- the package-private base the JDK shares between its
 * virtual-thread implementations (the {@code VirtualThread} of Project Loom and the bound
 * {@code BoundVirtualThread}).
 *
 * <p>KajiJDK does not have virtual threads, so nothing subclasses this; it exists only for the
 * shape. The JDK's constructor calls a package-private {@code Thread(String, int, boolean)} that
 * KajiLibrary does not have, so this one falls back to the no-arg {@code Thread()} — the parameters
 * are kept for the faithful signature.
 */
abstract class BaseVirtualThread extends Thread {

    BaseVirtualThread(String name, int characteristics, boolean bound) {
        super();
    }

    abstract void park();

    abstract void parkNanos(long nanos);

    abstract void unpark();
}
