package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintServiceAttribute;

/**
 * Cuantos trabajos hay en la cola de la impresora, contando el que se esta imprimiendo.
 */
public final class QueuedJobCount extends IntegerSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 7499723077864047742L;

    public QueuedJobCount(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un QueuedJobCount de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof QueuedJobCount;
    }

    public final Class<? extends Attribute> getCategory() {
        return QueuedJobCount.class;
    }

    public final String getName() {
        return "queued-job-count";
    }
}
