package java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

// Two connected socket channels, made by talking to ourselves over the loopback interface.
//
// ===============================================================================================
// WHY A SOCKET PAIR AND NOT A REAL PIPE
// ===============================================================================================
//
// Because a pipe is only worth having if a selector can watch it, and what a selector watches is
// sockets: `poll` takes socket handles and nothing else. An in-memory queue between two objects
// would read and write perfectly well and be invisible to `select`, which is the one thing it is
// wanted for.
//
// **This is what the JDK does on Windows**, for the same reason: `sun.nio.ch.PipeImpl` there opens a
// listener on the loopback address, connects to it, accepts, and hands back the two ends. What it
// costs is a port number for as long as the connection lives and a trip through the network stack
// that never leaves the machine.
//
// The listener is closed as soon as the connection is up: it was scaffolding, and leaving it around
// would hold a port and let a stranger connect to something nobody is reading.
final class LoopbackPair {

    private LoopbackPair() {
    }

    /**
     * Two channels connected to each other.
     *
     * @return the accepted end first, the connecting end second
     * @throws IOException if the pair cannot be made
     */
    static SocketChannel[] open() throws IOException {
        final ServerSocketChannel listener = ServerSocketChannel.open();
        try {
            listener.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 1);
            final int port = ((InetSocketAddress) listener.getLocalAddress()).getPort();
            final SocketChannel connecting = SocketChannel.open();
            try {
                connecting.connect(
                        new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
                final SocketChannel accepted = listener.accept();
                if (accepted == null) {
                    throw new IOException("the loopback pair did not connect");
                }
                return new SocketChannel[] {accepted, connecting};
            } catch (IOException e) {
                connecting.close();
                throw e;
            }
        } finally {
            listener.close();
        }
    }
}
