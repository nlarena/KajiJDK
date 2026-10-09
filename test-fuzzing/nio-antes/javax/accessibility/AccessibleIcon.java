package javax.accessibility;

/**
 * The description of an icon, for whoever cannot see it.
 *
 * <p>The description is the only thing that matters here: an icon without a description is
 * invisible to an assistive technology, however perfectly drawn. The size is there so that an
 * equivalent gap can be laid out.
 */
public interface AccessibleIcon {

    /**
     * What the icon represents, in words.
     *
     * @return the description, or `null` if it has none
     */
    String getAccessibleIconDescription();

    /** Changes the description. */
    void setAccessibleIconDescription(String description);

    /** Width of the icon, or -1 if not known. */
    int getAccessibleIconWidth();

    /** Height of the icon, or -1 if not known. */
    int getAccessibleIconHeight();
}
