package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * The job's size in units of 1024 octets, rounded up.
 *
 * <p>It measures the document's data, not what it takes printed, and it is counted <em>only
 * once</em> even if {@link Copies} asks for several: it is the bytes that have to be sent, not the
 * ones that have to be printed.
 */
public final class JobKOctets extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -8959710146498202869L;

    public JobKOctets(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a JobKOctets from being equal to another
     * integer attribute with the same number.
     */
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
