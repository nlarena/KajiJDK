package javax.print;

import javax.print.attribute.PrintRequestAttributeSet;

/**
 * KajiLibrary's javax.print.MultiDocPrintJob -- a job that accepts several documents.
 *
 * <p>The documents go in a single job, not in several. The difference matters: they share the
 * request's attributes, come out together in the queue, and are cancelled together.
 */
public interface MultiDocPrintJob extends DocPrintJob {

    /**
     * Prints them all.
     *
     * @throws PrintException if something failed
     */
    void print(MultiDoc multiDoc, PrintRequestAttributeSet attributes) throws PrintException;
}
