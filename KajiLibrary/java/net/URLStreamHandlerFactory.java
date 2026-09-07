package java.net;

// The one that knows how to manufacture a protocol's handler.
//
// It is the extension point that makes `java.net.URL` open: the class does not carry a handler per
// protocol wired inside it, it asks the installed factory instead. That way a program can teach `URL`
// a scheme the platform does not know --`classpath:`, `res:`, one of its own-- without touching
// `URL`.
//
// A single method, and returning null is part of the contract: it means "I know nothing about that
// protocol", and there `URL` goes on with its internal handlers.
//
// Pure computation: nothing omitted.
public interface URLStreamHandlerFactory {

    /**
     * The handler for {@code protocol}, or null if this factory does not know that protocol.
     *
     * @param protocol the scheme, in lower case and without the colon ("http", "file")
     */
    URLStreamHandler createURLStreamHandler(String protocol);
}
