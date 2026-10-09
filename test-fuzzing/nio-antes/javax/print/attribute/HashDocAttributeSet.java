package javax.print.attribute;

import java.io.Serializable;

/**
 * KajiLibrary's javax.print.attribute.HashDocAttributeSet -- a {@link HashAttributeSet} that only
 * accepts {@link DocAttribute}s.
 *
 * <p>It has no body, and that is the interesting part: the four constructors pass {@code
 * DocAttribute.class} to the base class's and there the class ends. All the restriction is done by
 * {@code HashAttributeSet.add} checking against that interface, so putting in an attribute that is
 * not a document one --for example through {@code addAll}, where the static type does not help--
 * goes out through {@code ClassCastException} at run time.
 *
 * <p>The signatures do narrow the type where they can: the constructor takes a {@code
 * DocAttribute[]} and not an {@code Attribute[]}, so that the common case is caught at compile time
 * and only what the signature cannot express is left for run time.
 */
public class HashDocAttributeSet extends HashAttributeSet implements DocAttributeSet, Serializable {

    private static final long serialVersionUID = -1128534486061432528L;

    /** Empty. */
    public HashDocAttributeSet() {
        super(DocAttribute.class);
    }

    /** With one document attribute. NullPointerException if it is null. */
    public HashDocAttributeSet(DocAttribute attribute) {
        super(attribute, DocAttribute.class);
    }

    /** With another document set's attributes. A null set gives the empty set. */
    public HashDocAttributeSet(DocAttributeSet attributes) {
        super(attributes, DocAttribute.class);
    }

    /** With the array's, in order: if there are two of the same category the last one wins. */
    public HashDocAttributeSet(DocAttribute[] attributes) {
        super(attributes, DocAttribute.class);
    }
}
