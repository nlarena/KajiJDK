package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.UnsupportedAddressTypeException — the address is of a type this
 * channel does not support.
 */
public class UnsupportedAddressTypeException extends IllegalArgumentException {

    private static final long serialVersionUID = 1000000023L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public UnsupportedAddressTypeException() {
        super();
    }
}
