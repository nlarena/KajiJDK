package javax.print.event;

import javax.print.DocPrintJob;
import javax.print.attribute.PrintJobAttributeSet;

/**
 * KajiLibrary's javax.print.event.PrintJobAttributeEvent -- a job's attributes changed.
 *
 * <p>{@link #getAttributes} brings <b>only the ones that changed</b>, not the job's complete state.
 * It is the part that gets misread: a set of a single attribute does not mean the job has only one.
 *
 * <p>The set is read-only; see {@code AttributeSetUtilities.unmodifiableView}.
 */
public class PrintJobAttributeEvent extends PrintEvent {

    private static final long serialVersionUID = -6534469883874742101L;

    /** The ones that changed. */
    private final PrintJobAttributeSet attributes;

    /**
     * @param source the job
     * @param attributes the attributes that changed
     * @throws IllegalArgumentException if the job is null
     */
    public PrintJobAttributeEvent(DocPrintJob source, PrintJobAttributeSet attributes) {
        super(source);
        this.attributes = attributes;
    }

    /** The job. */
    public DocPrintJob getPrintJob() {
        return (DocPrintJob) getSource();
    }

    /** The ones that changed. See the class note. */
    public PrintJobAttributeSet getAttributes() {
        return this.attributes;
    }
}
