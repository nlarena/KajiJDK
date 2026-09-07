package java.net;

// The one that manufactures the implementation underneath a socket.
//
// A single method, and its reason for being is that `Socket` and `ServerSocket` should not carry
// their transport wired inside them: a factory is installed and every new socket starts using another
// implementation --a tunnel, a test socket, a transport of one's own-- without touching the code that
// uses them.
//
// In KajiJDK there is no `Socket` or `ServerSocket` consulting it (there are no network natives), but
// the interface does not promise there are: it promises that **if** anyone manufactures a
// `SocketImpl`, it is asked for here. That is true exactly as written.
public interface SocketImplFactory {

    /** A fresh implementation, without creating the system socket yet. */
    SocketImpl createSocketImpl();
}
