package javax.management.relation;

import javax.management.JMException;

/**
 * The root of the relation service's errors.
 *
 * <p>It extends {@link JMException} and not {@link RuntimeException}, and that says something about
 * the design: in a relation service almost everything that can fail is <b>state</b>, not
 * programming. That a role does not exist, that a relation was removed, that an MBean was
 * unregistered -- all of that changes while the system runs, so the compiler forces you to foresee
 * it.
 */
public class RelationException extends JMException {

    private static final long serialVersionUID = 5434016005679159613L;

    /** Without detail. */
    public RelationException() {
        super();
    }

    /** With a message. */
    public RelationException(String message) {
        super(message);
    }
}
