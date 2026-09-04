package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * El tamano del trabajo en unidades de 1024 octetos, redondeado para arriba.
 *
 * <p>Mide los datos del documento, no lo que ocupa impreso, y se cuenta <em>una sola vez</em>
 * aunque {@link Copies} pida varias: son los bytes que hay que mandar, no los que hay que imprimir.
 */
public final class JobKOctets extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -8959710146498202869L;

    public JobKOctets(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un JobKOctets de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobKOctets;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobKOctets.class;
    }

    public final String getName() {
        return "job-k-octets";
    }
}
