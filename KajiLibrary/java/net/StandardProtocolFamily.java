package java.net;

// The three protocol families the platform defines.
//
// It is an enum and not loose constants because the set is closed on the platform's side: there are
// exactly these three, and a `switch` over them can be exhaustive. That it implements
// `ProtocolFamily` all the same is what leaves the door open to third-party families without opening
// this enum.
//
// None of this asks for a network stack: they are names.
public enum StandardProtocolFamily implements ProtocolFamily {

    /** IPv4. */
    INET,

    /** IPv6. */
    INET6,

    /** Unix-domain sockets. */
    UNIX;
}
