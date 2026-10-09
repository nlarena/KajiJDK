package java.awt.print;

/**
 * KajiLibrary's java.awt.print.PrinterException -- something went wrong while printing.
 *
 * <p>The base of this package's exceptions. It is checked, and rightly so: a printer failing is a
 * normal condition of the environment, not a program error.
 *
 * <p>Not to be confused with {@code javax.print.PrintException}, which belongs to the other
 * printing system. The two packages coexist --this one is the old one, oriented to drawing; the
 * other is the new one, oriented to documents-- and their exceptions have no inheritance relation.
 */
public class PrinterException extends Exception {

    private static final long serialVersionUID = -3757589981158265819L;

    /** No detail. */
    public PrinterException() {
        super();
    }

    /** With a message. */
    public PrinterException(String msg) {
        super(msg);
    }
}
