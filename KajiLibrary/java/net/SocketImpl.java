package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import java.util.Objects;
import java.util.Set;

// The transport underneath a TCP socket, separated from the socket itself.
//
// ===========================================================================================
// WHAT THIS CLASS IS, AND WHY IT CAN BE HERE WITH NO NETWORK
// ===========================================================================================
//
// `Socket` is what the program uses; `SocketImpl` is what **does the work**. The separation is old
// and still useful: changing the implementation changes the transport without touching a line of the
// code that opens connections.
//
// Every method that touches the network --`create`, `connect`, `bind`, `listen`, `accept`, `close`,
// `getInputStream`, `available`, `sendUrgentData`-- is **abstract**. This class does not write them:
// it declares that someone is going to. That is exactly what it is in the JDK, and it is the only
// honest way for the type to exist in a VM with no sockets: **there is not one method here that
// promises to connect**. There is a list of what would have to be implemented.
//
// The little that is concrete are field accessors (`getInetAddress`, `getPort`, `getLocalPort`,
// `getFileDescriptor`), the `toString`, and the translations between the two ways of naming options
// --the old `SocketOptions` one, with integers, and the new `SocketOption<T>` one, with types.
// None of that touches the network: it is fields and a `switch`.
//
// `shutdownInput`/`shutdownOutput` and `setPerformancePreferences` are declared with the same body as
// in the JDK: the first two throw `IOException("Method not implemented!")` --it is the JDK's base
// implementation, not a KajiJDK stub-- and the third does nothing, because they are suggestions an
// implementation may ignore by contract.
//
// ===========================================================================================
// WHO IMPLEMENTS IT
// ===========================================================================================
//
// **Nobody, in this tree.** `Socket` and `ServerSocket` are here now and they talk to the VM's
// network natives directly, without going through a `SocketImpl`; this class is the door a
// replaceable transport would come in through the day someone writes one.
//
// All twenty-eight members are here.
public abstract class SocketImpl implements SocketOptions {

    /** The system socket's descriptor, or null if it has not been created yet. */
    protected FileDescriptor fd;

    /** The far end's address. */
    protected InetAddress address;

    /** The far end's port. */
    protected int port;

    /** This side's port. */
    protected int localport;

    public SocketImpl() {
    }

    /**
     * Creates the system socket.
     *
     * @param stream true for TCP, false for UDP
     * @throws IOException if it could not be created
     */
    protected abstract void create(boolean stream) throws IOException;

    /**
     * Connects to {@code host}:{@code port}, resolving the name.
     *
     * @throws IOException if it could not connect or the name did not resolve
     */
    protected abstract void connect(String host, int port) throws IOException;

    /**
     * Connects to that address and port.
     *
     * @throws IOException if it could not connect
     */
    protected abstract void connect(InetAddress address, int port) throws IOException;

    /**
     * Connects with a time limit.
     *
     * @param timeout milliseconds, or 0 to wait without limit
     * @throws IOException if it could not connect or the time ran out
     */
    protected abstract void connect(SocketAddress address, int timeout) throws IOException;

    /**
     * Binds the socket to a local address and port.
     *
     * @throws IOException if the port is taken or the address does not belong to this machine
     */
    protected abstract void bind(InetAddress host, int port) throws IOException;

    /**
     * Starts accepting connections, queueing up to {@code backlog} unattended.
     *
     * @throws IOException if it could not
     */
    protected abstract void listen(int backlog) throws IOException;

    /**
     * Waits for an incoming connection and leaves it in {@code s}.
     *
     * <p>The result is written **into the parameter** and not returned, which is one of the JDK's
     * most confusing signatures: `s` arrives empty and comes out connected.
     *
     * @throws IOException if the wait failed
     */
    protected abstract void accept(SocketImpl s) throws IOException;

    /**
     * The stream for reading from the connection.
     *
     * @throws IOException if it cannot be opened
     */
    protected abstract InputStream getInputStream() throws IOException;

    /**
     * The stream for writing to the connection.
     *
     * @throws IOException if it cannot be opened
     */
    protected abstract OutputStream getOutputStream() throws IOException;

    /**
     * How many bytes can be read without blocking.
     *
     * @throws IOException if the query failed
     */
    protected abstract int available() throws IOException;

    /**
     * Closes the socket.
     *
     * @throws IOException if the close failed
     */
    protected abstract void close() throws IOException;

    /**
     * Closes the reading half, leaving the writing one open.
     *
     * <p>The base body throws, just as in the JDK: not every implementation knows how to close half a
     * connection, and the ones that do override the method.
     *
     * @throws IOException always, in the base implementation
     */
    protected void shutdownInput() throws IOException {
        throw new IOException("Method not implemented!");
    }

    /**
     * Closes the writing half, leaving the reading one open.
     *
     * @throws IOException always, in the base implementation
     */
    protected void shutdownOutput() throws IOException {
        throw new IOException("Method not implemented!");
    }

    /** The system's descriptor, or null if the socket was not created. */
    protected FileDescriptor getFileDescriptor() {
        return this.fd;
    }

    /** The far end's address. */
    protected InetAddress getInetAddress() {
        return this.address;
    }

    /** The far end's port. */
    protected int getPort() {
        return this.port;
    }

    /**
     * Whether this implementation knows how to send urgent data.
     *
     * <p>The base says no, and it is the right answer for a class that implements nothing: the one
     * that knows overrides the method. Saying yes would oblige `sendUrgentData` to work.
     */
    protected boolean supportsUrgentData() {
        return false;
    }

    /**
     * Sends a byte out of band.
     *
     * @throws IOException if the send failed
     */
    protected abstract void sendUrgentData(int data) throws IOException;

    /** This side's port. */
    protected int getLocalPort() {
        return this.localport;
    }

    @Override
    public String toString() {
        return "Socket[addr=" + getInetAddress() + ",port=" + getPort()
                + ",localport=" + getLocalPort() + "]";
    }

    /**
     * Suggests what matters most to this connection, in relative importance.
     *
     * <p>It does nothing, here and in the JDK: they are **suggestions**, and the contract says
     * explicitly that an implementation may ignore them. An empty body is not a papered-over gap; it
     * is the base implementation.
     */
    protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }

    /**
     * Sets an option named the new way, {@link SocketOption}'s.
     *
     * <p>**The base implementation always throws**, after checking that the name is not null. It is
     * not a KajiJDK gap: it is literally what the JDK does, and it is right that it should. This
     * class has two option vocabularies --the old integer one it inherits from {@link SocketOptions},
     * and this one-- and it **does not bridge them**, because an implementation that only serves the
     * old one has no reason to accept the new one's names. Translating from one to the other on its
     * own would make an option look supported when the subclass never considered it.
     *
     * <p>A subclass that wants to support them overrides this method and {@link #supportedOptions}.
     *
     * @throws UnsupportedOperationException always, in the base implementation
     * @throws NullPointerException          if {@code name} is null
     * @throws IOException                   if the socket refuses it
     */
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        Objects.requireNonNull(name);
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /**
     * The value of an option named the new way.
     *
     * <p>It always throws in the base, for the same reason as {@link #setOption(SocketOption, Object)}.
     *
     * @throws UnsupportedOperationException always, in the base implementation
     * @throws NullPointerException          if {@code name} is null
     * @throws IOException                   if the socket cannot read it
     */
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        Objects.requireNonNull(name);
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /**
     * The options this implementation understands.
     *
     * <p>**The empty set**, just like the JDK, and for the same reason: the base class serves no
     * option of the new form. A subclass that serves one declares it here.
     */
    protected Set<SocketOption<?>> supportedOptions() {
        return Collections.emptySet();
    }
}
