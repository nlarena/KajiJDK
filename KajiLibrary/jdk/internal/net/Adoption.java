package jdk.internal.net;

/**
 * The bridge a `java.nio.channels` channel gets the `java.net` socket that wraps it through.
 *
 * <h2>Why a bridge is needed</h2>
 *
 * <p>{@code SocketChannel.socket()} has to return a {@link java.net.Socket} **over the same system
 * socket** the channel already has open. Manufacturing one like that is not public and cannot be:
 * `java.net.Socket` exposes no way of saying "wrap me this handle", and adding one would be a member
 * the JDK does not have, that is, breaking the contract in order to fix it.
 *
 * <p>Nor can the two packages talk directly --nothing of `java.net` is package-visible from
 * `java.nio.channels`-- so the bridge lives here, in the one place both already know. It is the same
 * pattern the JDK solves with its *shared secrets*, for the same reason and in the same shape: the
 * package above **registers** how to manufacture, the one below **asks**.
 *
 * <h2>Why `Object` and not the types</h2>
 *
 * <p>The methods return `Object` and the caller casts. It is not laziness: if this class named
 * `java.net.Socket`, `java.net` and `jdk.internal.net` would reference each other **in both
 * directions**, and a circular dependency between packages of the base library is exactly what is
 * best avoided (see finding #474 on what happens when compiling in batch). With `Object` the arrow
 * goes one way only.
 *
 * <h2>Who registers, and when</h2>
 *
 * <p>`java.net` registers in {@link java.net.Socket}'s static initializer, so it is enough for that
 * class to be **initialized**. Since the asker may be the first to arrive, {@link #require} forces it
 * before giving up: without that, the load order would decide whether `socket()` works, which is the
 * worst kind of error --the one that shows up depending on who ran first.
 *
 * <p>It is forced **by name**, which is exactly what `Class.forName` promises and the only thing that
 * does not make the dependency between the two packages circular. For a while it could not be done:
 * `forName` did not run the initializer (finding #487, since fixed), and this bridge is what brought
 * it to light.
 */
public final class Adoption {

    private Adoption() {
    }

    /** What `java.net` knows how to do and `java.nio.channels` needs. */
    public interface Factory {

        /** A `java.net.Socket` over that already connected socket. */
        Object tcp(int handle);

        /** A `java.net.ServerSocket` over that already bound socket. */
        Object server(int handle);

        /** A `java.net.DatagramSocket` over that already bound socket. */
        Object datagram(int handle);
    }

    private static volatile Factory factory;

    /** `java.net` installs it. Calling it twice overwrites the previous one, which is intended. */
    public static void register(Factory f) {
        factory = f;
    }

    /** A `java.net.Socket` over that handle. */
    public static Object tcp(int handle) {
        return Adoption.require().tcp(handle);
    }

    /** A `java.net.ServerSocket` over that handle. */
    public static Object server(int handle) {
        return Adoption.require().server(handle);
    }

    /** A `java.net.DatagramSocket` over that handle. */
    public static Object datagram(int handle) {
        return Adoption.require().datagram(handle);
    }

    private static Factory require() {
        Factory f = factory;
        if (f == null) {
            // It has not been loaded yet. It is forced, and that runs its static initializer, which
            // is where it registers.
            try {
                Class.forName("java.net.Socket");
            } catch (ClassNotFoundException e) {
                // It is not there. The message below says the only thing that is known.
            }
            f = factory;
        }
        if (f == null) {
            throw new IllegalStateException("java.net did not register how to adopt a socket");
        }
        return f;
    }
}
