package javax.print.event;

import javax.print.PrintService;
import javax.print.attribute.PrintServiceAttributeSet;

/**
 * KajiLibrary's javax.print.event.PrintServiceAttributeEvent -- cambio algo de la impresora.
 *
 * <p>Es de la impresora, no de un trabajo: se quedo sin papel, cambio de estado, se lleno la cola. El
 * conjunto trae solo los atributos que cambiaron, igual que en {@link PrintJobAttributeEvent}.
 */
public class PrintServiceAttributeEvent extends PrintEvent {

    private static final long serialVersionUID = -7565987018140326600L;

    /** Los que cambiaron. */
    private final PrintServiceAttributeSet attributes;

    /**
     * @param source la impresora
     * @param attributes los atributos que cambiaron
     * @throws IllegalArgumentException si la impresora es null
     */
    public PrintServiceAttributeEvent(PrintService source, PrintServiceAttributeSet attributes) {
        super(source);
        this.attributes = attributes;
    }

    /** La impresora. */
    public PrintService getPrintService() {
        return (PrintService) getSource();
    }

    /** Los que cambiaron. */
    public PrintServiceAttributeSet getAttributes() {
        return this.attributes;
    }
}
