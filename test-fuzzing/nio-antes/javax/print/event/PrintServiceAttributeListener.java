package javax.print.event;

/**
 * KajiLibrary's javax.print.event.PrintServiceAttributeListener -- listens to the printer's
 * changes.
 *
 * <p>It is registered with {@code PrintService.addPrintServiceAttributeListener}. Unlike the job
 * one, this one carries no filter: they all arrive.
 */
public interface PrintServiceAttributeListener {

    /** Something about the printer changed. */
    void attributeUpdate(PrintServiceAttributeEvent psae);
}
