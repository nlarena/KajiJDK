package java.lang;

// Thrown when a class's static initializer (<clinit>) completes abruptly with an exception that is
// not an Error. The VM wraps that exception in this error and delivers it to the code whose active
// use triggered initialization (JVMS §5.5). (Our Throwable models no `cause`, so — unlike the real
// JDK — the original exception is not retained; the class becoming erroneous is the observable.)
public class ExceptionInInitializerError extends LinkageError {
    public ExceptionInInitializerError(String message) {
        super(message);
    }

    public ExceptionInInitializerError() {
    }
}
