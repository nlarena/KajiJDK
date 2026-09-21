package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintServiceAttribute;

/**
 * How many jobs there are in the printer's queue, counting the one being printed.
 */
public final class QueuedJobCount extends IntegerSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 7499723077864047742L;

    public QueuedJobCount(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a QueuedJobCount from being equal to another
     * integer attribute with the same number.
     */
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
