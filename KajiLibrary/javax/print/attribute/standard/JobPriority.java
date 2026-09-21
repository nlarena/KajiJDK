package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * With how much priority the job is served in the queue: 1 the lowest, 100 the highest.
 *
 * <p>The scale of a hundred is fixed by IPP and it is the reason for the closed range. A printer
 * does not have to distinguish a hundred levels: it may group. How many it really distinguishes is
 * said by {@link JobPrioritySupported}.
 */
public final class JobPriority extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -4599900369040602769L;

    public JobPriority(int value) {
        super(value, 1, 100);
    }

    /**
     * The {@code instanceof} is what keeps a JobPriority from being equal to another
     * integer attribute with the same number.
     */
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
