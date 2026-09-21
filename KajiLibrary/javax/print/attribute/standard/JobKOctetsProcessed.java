package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * How many K-octets of the job were already processed.
 *
 * <p>The progress against {@link JobKOctets}. With several copies it may go past the declared
 * total, because here each pass does count.
 */
public final class JobKOctetsProcessed extends IntegerSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = -6265238509657881806L;

    public JobKOctetsProcessed(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a JobKOctetsProcessed from being equal to another
     * integer attribute with the same number.
     */
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
