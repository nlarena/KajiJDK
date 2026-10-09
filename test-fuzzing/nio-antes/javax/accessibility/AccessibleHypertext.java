package javax.accessibility;

/**
 * Accessible text with links inside.
 *
 * <p>{@link #getLinkIndex} is the operation that matters: given a point in the text, it says which
 * link contains it. It is what allows announcing "this is a link" while walking the text, instead
 * of having to list the links separately and lose where they were.
 */
public interface AccessibleHypertext extends AccessibleText {

    /** How many links there are. */
    int getLinkCount();

    /**
     * The `linkIndex`-th link.
     *
     * @return the link, or `null` if there are not that many
     */
    AccessibleHyperlink getLink(int linkIndex);

    /**
     * Which link contains that character.
     *
     * @return the link number, or -1 if that character is in none
     */
    int getLinkIndex(int charIndex);
}
