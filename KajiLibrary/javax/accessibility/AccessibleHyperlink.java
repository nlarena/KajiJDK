package javax.accessibility;

/**
 * A link within a text.
 *
 * <p>It is an {@link AccessibleAction} because a link is, above all, something that can be
 * **done**. What it adds is where it is: the stretch of text it occupies, so that whoever reads can
 * announce it in its place and not at the end.
 *
 * <p>{@link #isValid} exists because the document may change underneath: a link obtained before an
 * edit may be pointing at a stretch that no longer exists.
 */
public abstract class AccessibleHyperlink implements AccessibleAction {

    /** For subclasses. */
    protected AccessibleHyperlink() {
    }

    /** Whether the link still points at a stretch that exists. */
    public abstract boolean isValid();

    /** How many actions it has; for a link, normally one. */
    public abstract int getAccessibleActionCount();

    /**
     * Follows the link.
     *
     * @return `true` if it could
     */
    public abstract boolean doAccessibleAction(int i);

    /** The text of the link. */
    public abstract String getAccessibleActionDescription(int i);

    /** Where it points: normally a `URL`. */
    public abstract Object getAccessibleActionObject(int i);

    /** What is shown as a link: a text or an image. */
    public abstract Object getAccessibleActionAnchor(int i);

    /** Where the link's stretch starts. */
    public abstract int getStartIndex();

    /** Where it ends. */
    public abstract int getEndIndex();
}
