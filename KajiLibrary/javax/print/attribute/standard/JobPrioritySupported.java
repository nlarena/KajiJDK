package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

/**
 * Cuantos niveles de {@link JobPriority} distingue de verdad la impresora.
 *
 * <p>El numero no es una prioridad sino una <em>cantidad</em>: si vale 5, la escala de 1 a 100 se
 * reparte en cinco tramos iguales y pedir 3 o pedir 20 da lo mismo. Es el unico atributo de valores
 * soportados que es un entero suelto y no un conjunto de enteros.
 */
public final class JobPrioritySupported extends IntegerSyntax implements SupportedValuesAttribute {

    private static final long serialVersionUID = 2564840378013555894L;

    public JobPrioritySupported(int value) {
        super(value, 1, 100);
    }

    /** El {@code instanceof} es lo que impide que un JobPrioritySupported de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobPrioritySupported;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobPrioritySupported.class;
    }

    public final String getName() {
        return "job-priority-supported";
    }
}
