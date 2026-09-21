package javax.print;

import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.event.PrintJobAttributeListener;
import javax.print.event.PrintJobListener;

/**
 * KajiLibrary's javax.print.DocPrintJob -- a print job.
 *
 * <p>It is taken from {@code PrintService.createPrintJob()} and serves <b>only once</b>: calling
 * {@link #print} twice on the same job throws {@code PrintException}. To print something else one
 * has to ask the service for another job.
 *
 * <p>{@link #print} blocks until the service took the document, not until the paper came out. To
 * know what happened later one has to register a {@link PrintJobListener} <b>before</b> calling it.
 *
 * <p>The attribute listener carries a set saying which ones are of interest; see
 * {@link PrintJobAttributeListener}.
 */
public interface DocPrintJob {

    /** Which printer it belongs to. */
    PrintService getPrintService();

    /** The job's current attributes. */
    PrintJobAttributeSet getAttributes();

    /** Registers a state listener. Before {@link #print}. */
    void addPrintJobListener(PrintJobListener listener);

    /** Unregisters it. */
    void removePrintJobListener(PrintJobListener listener);

    /**
     * Registers an attribute listener.
     *
     * @param attributes which are of interest; null means all
     */
    void addPrintJobAttributeListener(PrintJobAttributeListener listener,
                                      PrintJobAttributeSet attributes);

    /** Unregisters it. */
    void removePrintJobAttributeListener(PrintJobAttributeListener listener);

    /**
     * Prints. See the class note: it is used only once and does not wait for the paper.
     *
     * @throws PrintException if it failed, or if this job was already used
     */
    void print(Doc doc, PrintRequestAttributeSet attributes) throws PrintException;
}
