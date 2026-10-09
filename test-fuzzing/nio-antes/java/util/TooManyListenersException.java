package java.util;

// It signals that a **unicast** event source already has its listener.
//
// It is a checked exception, and that is the whole mechanism: Java's event model is multicast by
// default, and the only way of declaring "this source admits a single listener" is for its
// `addXListener` to declare `throws TooManyListenersException`. The signature is the
// documentation.
public class TooManyListenersException extends Exception {

    // With no message.
    public TooManyListenersException() {
        super();
    }

    // With the given message.
    public TooManyListenersException(String s) {
        super(s);
    }
}
