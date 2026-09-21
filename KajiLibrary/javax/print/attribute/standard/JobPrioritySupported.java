package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

/**
 * How many levels of {@link JobPriority} the printer really distinguishes.
 *
 * <p>The number is not a priority but a <em>quantity</em>: if it is 5, the scale from 1 to 100 is
 * split into five equal stretches and asking for 3 or for 20 gives the same. It is the only
 * supported-values attribute that is a loose integer and not a set of integers.
 */
public final class JobPrioritySupported extends IntegerSyntax implements SupportedValuesAttribute {

    private static final long serialVersionUID = 2564840378013555894L;

    public JobPrioritySupported(int value) {
        super(value, 1, 100);
    }

    /**
     * The {@code instanceof} is what keeps a JobPrioritySupported from being equal to another
     * integer attribute with the same number.
     */
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
