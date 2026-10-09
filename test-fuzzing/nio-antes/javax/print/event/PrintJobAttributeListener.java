package javax.print.event;

/**
 * KajiLibrary's javax.print.event.PrintJobAttributeListener -- listens to a job's attribute
 * changes.
 *
 * <p>It is registered with {@code DocPrintJob.addPrintJobAttributeListener}, which also receives
 * the set of attributes of interest. Without that filter a long job generates an event for each
 * change of any attribute.
 */
public interface PrintJobAttributeListener {

    /** Something of what one asked to listen to changed. */
    void attributeUpdate(PrintJobAttributeEvent pjae);
}
