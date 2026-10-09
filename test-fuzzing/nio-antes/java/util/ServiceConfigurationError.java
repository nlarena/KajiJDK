package java.util;

// Something went wrong loading a service provider.
//
// It is an `Error` and not an `Exception`, which is a strong choice of the JDK's and a reasoned one:
// a broken service configuration is not a condition the program can handle, it is a badly built
// deployment. Treating it as recoverable would lead to programs that start up half way.
public class ServiceConfigurationError extends Error {

    // With the given message.
    public ServiceConfigurationError(String msg) {
        super(msg);
    }

    // With the given message and `cause` as the cause.
    public ServiceConfigurationError(String msg, Throwable cause) {
        super(msg, cause);
    }
}
