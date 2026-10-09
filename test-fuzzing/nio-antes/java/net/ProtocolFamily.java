package java.net;

// An address's protocol family: IPv4, IPv6, or Unix-domain sockets.
//
// A single method, `name()`, and that is the point: the interface exists so that the APIs that open
// channels can accept families other than the three standard ones without changing their signatures.
// The `StandardProtocolFamily` enum implements it; anyone can contribute another.
public interface ProtocolFamily {

    String name();
}
