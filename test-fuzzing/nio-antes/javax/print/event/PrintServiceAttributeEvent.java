package javax.print.event;

import javax.print.PrintService;
import javax.print.attribute.PrintServiceAttributeSet;

/**
 * KajiLibrary's javax.print.event.PrintServiceAttributeEvent -- something about the printer
 * changed.
 *
 * <p>It is about the printer, not a job: it ran out of paper, changed state, the queue filled up.
 * The set brings only the attributes that changed, just as in {@link PrintJobAttributeEvent}.
 */
public class PrintServiceAttributeEvent extends PrintEvent {

    private static final long serialVersionUID = -7565987018140326600L;

    /** The ones that changed. */
    private final PrintServiceAttributeSet attributes;

    /**
     * @param source the printer
     * @param attributes the attributes that changed
     * @throws IllegalArgumentException if the printer is null
     */
    public PrintServiceAttributeEvent(PrintService source, PrintServiceAttributeSet attributes) {
        super(source);
        this.attributes = attributes;
    }

    /** The printer. */
    public PrintService getPrintService() {
        return (PrintService) getSource();
    }

    /** The ones that changed. */
    public PrintServiceAttributeSet getAttributes() {
        return this.attributes;
    }
}
