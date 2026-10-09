package java.awt.print;

/**
 * KajiLibrary's java.awt.print.PrinterAbortException -- the job was stopped on purpose.
 *
 * <p>The difference from a plain {@link PrinterException} is the <b>cause</b>: this one means that
 * someone cancelled it --the user, or the program with {@code PrinterJob.cancel()}--, not that
 * something broke. A program that catches it should neither retry nor report an error.
 */
public class PrinterAbortException extends PrinterException {

    private static final long serialVersionUID = 4725169026278854136L;

    /** No detail. */
    public PrinterAbortException() {
        super();
    }

    /** With a message. */
    public PrinterAbortException(String msg) {
        super(msg);
    }
}
