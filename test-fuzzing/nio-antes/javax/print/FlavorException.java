package javax.print;

/**
 * KajiLibrary's javax.print.FlavorException -- the failure was because of the document's format.
 *
 * <p>An interface; see the note of {@link PrintException}. It is implemented by the exception
 * thrown when the document's {@link DocFlavor} is not among the ones the printer accepts.
 */
public interface FlavorException {

    /** The formats it does not accept. */
    DocFlavor[] getUnsupportedFlavors();
}
