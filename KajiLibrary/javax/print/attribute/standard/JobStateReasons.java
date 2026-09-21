package javax.print.attribute.standard;

import java.util.Collection;
import java.util.HashSet;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;

/**
 * The set of reasons that explain a job's {@link JobState}.
 *
 * <p>It is an attribute that <b>is</b> a collection, not one that contains it: it extends
 * {@link HashSet} of {@link JobStateReason}. That is deliberate --it is walked and queried like any
 * set-- and it brings the consequence that it is <b>mutable</b>, unlike the rest of the package.
 * Keeping it in an attribute set and modifying it afterwards changes what that set reports.
 *
 * <p>A job may have zero reasons: "it is printing and all is well" needs no explanation.
 *
 * <p>The only thing overridden is {@code add}, to reject null: a reason that is no reason says
 * nothing, and letting it in would make whoever walks the set blow up later.
 */
public final class JobStateReasons extends HashSet<JobStateReason> implements PrintJobAttribute {

    private static final long serialVersionUID = 8849088261264331812L;

    public JobStateReasons() {
        super();
    }

    public JobStateReasons(int initialCapacity) {
        super(initialCapacity);
    }

    public JobStateReasons(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /** Copies another collection's contents; {@link #add} rejects the nulls inside. */
    public JobStateReasons(Collection<JobStateReason> collection) {
        super(collection);
    }

    public boolean add(JobStateReason o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return super.add(o);
    }

    public final Class<? extends Attribute> getCategory() {
        return JobStateReasons.class;
    }

    public final String getName() {
        return "job-state-reasons";
    }
}
