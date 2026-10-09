package java.net;

import java.io.IOException;

// The root of the socket subsystem's errors: "the socket said no".
//
// KajiJDK has no network stack, and yet this class **does** belong here. The reason is that an
// exception promises nothing: it does not say "I know how to open a socket", it says "if something
// fails while opening a socket, this is what it is called". It is a type, not a capability.
// Compiling against it and catching it is correct even though nothing in the library throws it in
// this VM -- portable code catches it all the same, and code that builds and throws it on its own
// gets exactly the object it expects.
//
// It is the same criterion that makes it legitimate to have `UnknownHostException` from before there
// was anything to resolve a name with.
public class SocketException extends IOException {

    private static final long serialVersionUID = -5935874303556886934L;

    public SocketException(String msg) {
        super(msg);
    }

    public SocketException() {
    }

    // This one and the `Throwable`-only one arrived in Java 13, when the JDK stopped losing the cause
    // while wrapping operating-system errors.
    public SocketException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SocketException(Throwable cause) {
        super(cause);
    }
}
