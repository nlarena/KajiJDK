package javax.accessibility;

/**
 * Implemented by every object that wants to be accessible.
 *
 * <p>It is deliberately minimal: a single method. All the information lives in the
 * {@link AccessibleContext}, and not in the object itself, so that a class does not have to fill up
 * with accessibility methods to take part.
 *
 * <p>That indirection is the package's central decision: a component **has** a context instead of
 * **being** accessible, and that way the cost of accessibility --which is real, in memory and in
 * work-- is paid only when somebody asks for it.
 */
public interface Accessible {

    /** This object's accessibility information. */
    AccessibleContext getAccessibleContext();
}
