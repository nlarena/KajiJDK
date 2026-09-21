package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * How many jobs there are ahead of this one in the queue.
 *
 * <p>Zero means it is the next one, not that it is already printing.
 */
public final class NumberOfInterveningJobs extends IntegerSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 2568141124844982746L;

    public NumberOfInterveningJobs(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a NumberOfInterveningJobs from being equal to another
     * integer attribute with the same number.
     */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof NumberOfInterveningJobs;
    }

    public final Class<? extends Attribute> getCategory() {
        return NumberOfInterveningJobs.class;
    }

    public final String getName() {
        return "number-of-intervening-jobs";
    }
}
