package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * Cuantos K-octetos del trabajo ya se procesaron.
 *
 * <p>El avance contra {@link JobKOctets}. Con varias copias puede pasarse del total declarado,
 * porque aca si se cuenta cada pasada.
 */
public final class JobKOctetsProcessed extends IntegerSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = -6265238509657881806L;

    public JobKOctetsProcessed(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un JobKOctetsProcessed de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobKOctetsProcessed;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobKOctetsProcessed.class;
    }

    public final String getName() {
        return "job-k-octets-processed";
    }
}
