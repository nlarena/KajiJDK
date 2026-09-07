package java.net;

// The one that manufactures a `DatagramSocket`'s low-level implementation.
//
// It exists for the same reason as `SocketImplFactory`: so that an application can put its own UDP
// stack --a tunnel, a simulation, a test layer-- underneath the standard API, without anyone using
// `DatagramSocket` noticing.
//
// One per VM, and it is installed with `DatagramSocket.setDatagramSocketImplFactory`.
//
// @deprecated The JDK deprecated it along with the `DatagramSocketImpl` mechanism.
@Deprecated
public interface DatagramSocketImplFactory {

    /** A fresh implementation, without creating the system socket yet. */
    DatagramSocketImpl createDatagramSocketImpl();
}
