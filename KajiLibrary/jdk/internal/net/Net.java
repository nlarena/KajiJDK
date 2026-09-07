package jdk.internal.net;

/**
 * TCP's seam between the library and the VM.
 *
 * <p>The same design as {@link jdk.internal.proc.Proc}: the native does the minimum and **knows
 * nothing about Java's classes**. It takes and returns strings, arrays and integers; who a `Socket`
 * is is the Java side's problem, and it can change without touching the VM.
 *
 * <p>A socket is a `handle`, an index into a table of the VM's. A handle is needed rather than a
 * one-shot operation because a socket **is** state between calls: its peer, its deadlines, whether
 * one of its halves has been closed. Handles are **not recycled**, so an old one never points at a
 * new socket -- which is the error that would be hardest to find.
 *
 * <h2>The error codes, which are not all -1</h2>
 *
 * <p>{@link #read} returns **-1** at end of stream and **-2** if the deadline expired, and that
 * distinction is the reason there is not a single sentinel: a closed connection and one that is still
 * alive but quiet are two different things, and `Socket`'s contract tells them apart --a
 * `SocketTimeoutException` is not an EOF. With a single -1 there would be no way of knowing which
 * happened.
 *
 * <h2>Nothing here waits: the -3</h2>
 *
 * <p>{@link #read} and {@link #accept} return **-3** for "there is nothing yet", and **neither of
 * them blocks**. It has to be that way: this VM's Java threads share one interpreter, so a native
 * standing still waiting inside does not let any other thread run --including the one that was going
 * to connect or to answer. A blocking `accept` would not be slow, it would be a deadlock.
 *
 * <p>The waiting goes on the Java side: retrying with a short {@link Thread#sleep} between attempts.
 * Sleeping is something this VM does know how to handle --it releases the interpreter-- so the thread
 * that waits keeps nobody from advancing. And since Java counts the time, `ServerSocket.accept` can
 * honour `setSoTimeout`, which a system accept did not allow.
 */
public final class Net {

    private Net() {
    }

    /**
     * Connects to that host and port. It returns the handle, or -1 if it could not.
     *
     * @param timeoutMs zero means no limit
     */
    public static native int connect(String host, int port, int timeoutMs);

    /**
     * Binds and listens. It returns the handle, or -1.
     *
     * <p>A port of zero lets the system choose; the one it picked is read with {@link #localPort}.
     */
    public static native int listen(String host, int port, int backlog);

    /**
     * Accepts a connection **without waiting**. It returns the new socket's handle, **-3** if there
     * is nobody yet, or -1 if it failed. See the header: whoever wants to wait, retries.
     */
    public static native int accept(int handle);

    /**
     * Reads into `buf` **without waiting**. It returns how many bytes it put there, **-1** at end of
     * stream, **-3** if nothing has arrived yet. The **-2** is reserved for the expired deadline,
     * which is decided by the caller: this native counts no time.
     */
    public static native int read(int handle, byte[] buf, int off, int len);

    /** Writes. `true` only if everything was written. */
    public static native boolean write(int handle, byte[] buf, int off, int len);

    /** Closes the socket. A handle that does not exist is ignored. */
    public static native void close(int handle);

    /** Closes the reading half. */
    public static native boolean shutdownIn(int handle);

    /** Closes the writing half. */
    public static native boolean shutdownOut(int handle);

    /** The local port, or -1. It serves both a stream and a listener. */
    public static native int localPort(int handle);

    /** The local address, or `null`. */
    public static native String localAddress(int handle);

    /** The peer's port, or -1. */
    public static native int remotePort(int handle);

    /** The peer's address, or `null`. */
    public static native String remoteAddress(int handle);

    /** The read deadline in milliseconds; zero is no limit. */
    public static native boolean setSoTimeout(int handle, int ms);

    /** Turns Nagle's algorithm on or off. */
    public static native boolean setTcpNoDelay(int handle, boolean on);

    // ---- UDP -------------------------------------------------------------------------------
    //
    // The same rules as TCP: nothing here waits, and "nothing has arrived yet" is **-3**. A datagram
    // has no end of stream --there is no connection to close-- so a -1 is always an error.

    /**
     * Binds a datagram socket. It returns the handle, or -1.
     *
     * <p>Port zero: the system chooses it, and it is read with {@link #localPort}.
     */
    public static native int udpBind(String host, int port);

    /** Sends a datagram. `true` only if it went out whole: a split datagram is not a datagram. */
    public static native boolean udpSend(int handle, String host, int port,
            byte[] buf, int off, int len);

    /**
     * Receives a datagram **without waiting**. It returns how many bytes it put there, **-3** if
     * nothing has arrived yet, -1 if it failed.
     *
     * <p>It records the sender, which is read with {@link #udpSenderAddress} and
     * {@link #udpSenderPort}. The three are **a single operation**: whoever receives has to read the
     * sender before another thread receives over the same socket.
     */
    public static native int udpReceive(int handle, byte[] buf, int off, int len);

    /** Who the last datagram received came from, or `null` if there was none. */
    public static native String udpSenderAddress(int handle);

    /** Which port the last datagram received came from, or -1. */
    public static native int udpSenderPort(int handle);

    /**
     * Joins a multicast group.
     *
     * @param iface the interface: an IPv4 address, an interface index in IPv6, or the empty string to
     *     let the system choose
     */
    public static native boolean udpJoin(int handle, String group, String iface);

    /** Leaves a multicast group. See {@link #udpJoin}. */
    public static native boolean udpLeave(int handle, String group, String iface);

    /** The hop limit of the multicast packets going out of that socket. */
    public static native boolean udpSetTtl(int handle, int ttl);

    // ---- the two that do have to block --------------------------------------------------------
    //
    // Binding the local end before connecting, and probing whether a host answers, both need a
    // **real** system `connect`, which blocks. They are started here, run on a separate system
    // thread, and the answer is collected with {@link #answerPoll} without hanging the VM.

    /**
     * Starts probing whether that host answers. It returns the pigeonhole's id.
     *
     * <p>A refusal counts as an answer: the RST is sent by the host, so it proves it is alive just as
     * an accepted connection does. Only silence counts as unreachable.
     *
     * @param local the address the probe goes out from, or the empty string to let the system choose
     * @param ttl the hop limit, or zero for whichever comes by default
     */
    public static native int reachableStart(String host, String local, int ttl);

    /**
     * Starts connecting to {@code host}:{@code port} **going out through** {@code local}:{@code
     * localPort}. It returns the pigeonhole's id; the answer is the socket's handle, or -1.
     *
     * @param local the empty string for the wildcard, which is what a null local address asks for
     */
    public static native int connectFromStart(String host, int port, String local, int localPort);

    /**
     * The answer, or **-3** if it has not arrived yet.
     *
     * <p>What it means depends on who is asking: the reachability probe answers 1 or 0, the connect
     * answers the handle or -1.
     */
    public static native int answerPoll(int answer);

    /** Releases the pigeonhole. It always has to be called, whether the answer was waited for or not. */
    public static native void answerFree(int answer);

    /**
     * Sends a byte **out of band**.
     *
     * <p>It is not writing to the stream: it goes with a protocol flag, and whoever receives it sees
     * it by a separate path.
     */
    public static native boolean sendUrgent(int handle, int b);

    /** {@link #poll} bit: the socket has something to read, or a connection to accept. */
    public static final int POLL_READ = 1;

    /** {@link #poll} bit: the socket can be written to, or a connect has finished. */
    public static final int POLL_WRITE = 2;

    /** {@link #poll} bit: the socket has an error or the peer hung up. */
    public static final int POLL_ERROR = 4;

    /**
     * Asks which of those sockets are ready, waiting up to `timeoutMs`.
     *
     * <p>This is the one call a selector cannot do without. Its whole job is to answer "which of
     * these has something" **without reading**, and every other way of finding out consumes: a read
     * that returns data has taken it, and a read that returns nothing has told you about one socket
     * only.
     *
     * <p>Three parallel arrays and not a list of objects because a selector makes this call on every
     * turn of its loop, and one object per registered channel per turn is the cost that gives
     * selectors a bad name.
     *
     * @param handles the sockets to ask about
     * @param events what to watch for on each, as {@link #POLL_READ} and {@link #POLL_WRITE}
     * @param revents filled in with what each one has; also carries {@link #POLL_ERROR}
     * @param timeoutMs how long to wait, 0 to ask and return, negative to wait as long as it takes
     * @return how many sockets reported something, or -1 when this platform has no poll
     */
    public static native int poll(int[] handles, int[] events, int[] revents, int timeoutMs);
}
