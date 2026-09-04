package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Con cuanta prioridad se atiende el trabajo en la cola: 1 la mas baja, 100 la mas alta.
 *
 * <p>La escala de cien la fija IPP y es la razon del rango cerrado. Una impresora no tiene por que
 * distinguir cien niveles: puede agrupar. Cuantos distingue de verdad lo dice {@link
 * JobPrioritySupported}.
 */
public final class JobPriority extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -4599900369040602769L;

    public JobPriority(int value) {
        super(value, 1, 100);
    }

    /** El {@code instanceof} es lo que impide que un JobPriority de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobPriority;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobPriority.class;
    }

    public final String getName() {
        return "job-priority";
    }
}
