package java.lang.instrument;

/**
 * KajiLibrary's java.lang.instrument.IllegalClassFormatException -- the transformer was given bytes
 * that are not a class.
 *
 * <p>A {@link ClassFileTransformer} throws it to say that the bytes it <b>received</b> are no good
 * to it. It is the only way it has of refusing without breaking the load: the virtual machine
 * catches it, ignores that transformer and carries on with the rest.
 *
 * <p>The other way of refusing --returning null-- means "not interested, I am not touching it". The
 * difference matters: null is silent, and this is the channel for saying that something was wrong.
 */
public class IllegalClassFormatException extends Exception {

    private static final long serialVersionUID = -3841736710924794009L;

    /** With no detail. */
    public IllegalClassFormatException() {
        super();
    }

    /** With a message saying what was wrong. */
    public IllegalClassFormatException(String s) {
        super(s);
    }
}
