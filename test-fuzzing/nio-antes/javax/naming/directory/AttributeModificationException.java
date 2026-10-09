package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.AttributeModificationException -- a modification violated
 * the schema.
 *
 * <p>It is the only one in this package that carries data of its own: the {@link ModificationItem}s
 * that could not be applied. It is needed because {@code modifyAttributes} takes a <b>list</b> of
 * modifications and the specification asks for all or none to be applied -- without knowing which
 * one failed, there would be no way to fix the request.
 *
 * <p>The modifications are set after building the exception and not in the constructor. It is
 * awkward and has its reason: the layer that detects the error is usually deeper than the one that
 * knows which items came in the request.
 */
public class AttributeModificationException extends NamingException {

    private static final long serialVersionUID = 8060676069678710186L;

    /** The ones not applied, or null if not stated. */
    private ModificationItem[] unexecs = null;

    /** With no detail. */
    public AttributeModificationException() {
        super();
    }

    /** With a message saying what the problem was. */
    public AttributeModificationException(String explanation) {
        super(explanation);
    }

    /**
     * Sets the modifications that did not get applied.
     *
     * <p>The array is kept as is, without copying, which is what the JDK does.
     */
    public void setUnexecutedModifications(ModificationItem[] e) {
        this.unexecs = e;
    }

    /** See {@link #setUnexecutedModifications}; null if nobody set them. */
    public ModificationItem[] getUnexecutedModifications() {
        return this.unexecs;
    }

    /**
     * {@code NamingException}'s message and, if any, the <b>first</b> unapplied modification.
     *
     * <p>The first and not all of them: it is the one that usually explains the failure, and a long
     * list in a {@code toString} makes any log unreadable.
     */
    public String toString() {
        String head = super.toString();
        if (this.unexecs == null || this.unexecs.length == 0) {
            return head;
        }
        return head + "First unexecuted modification: " + this.unexecs[0].toString();
    }
}
