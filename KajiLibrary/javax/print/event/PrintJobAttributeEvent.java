package javax.print.event;

import javax.print.DocPrintJob;
import javax.print.attribute.PrintJobAttributeSet;

/**
 * KajiLibrary's javax.print.event.PrintJobAttributeEvent -- cambiaron atributos de un trabajo.
 *
 * <p>{@link #getAttributes} trae <b>solo los que cambiaron</b>, no el estado completo del trabajo. Es
 * la parte que se malinterpreta: un conjunto de un solo atributo no significa que el trabajo tenga uno
 * solo.
 *
 * <p>El conjunto es de solo lectura; ver {@code AttributeSetUtilities.unmodifiableView}.
 */
public class PrintJobAttributeEvent extends PrintEvent {

    private static final long serialVersionUID = -6534469883874742101L;

    /** Los que cambiaron. */
    private final PrintJobAttributeSet attributes;

    /**
     * @param source el trabajo
     * @param attributes los atributos que cambiaron
     * @throws IllegalArgumentException si el trabajo es null
     */
    public PrintJobAttributeEvent(DocPrintJob source, PrintJobAttributeSet attributes) {
        super(source);
        this.attributes = attributes;
    }

    /** El trabajo. */
    public DocPrintJob getPrintJob() {
        return (DocPrintJob) getSource();
    }

    /** Los que cambiaron. Ver la nota de la clase. */
    public PrintJobAttributeSet getAttributes() {
        return this.attributes;
    }
}
