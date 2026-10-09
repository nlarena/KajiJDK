package javax.swing.text;

import java.util.Enumeration;

/**
 * A set of attributes that can be changed.
 *
 * <p>{@link AttributeSet} is read-only on purpose: the sets a document shares among thousands of
 * characters have to be immutable in order to be shareable. This interface is the other half,
 * the one used by whoever is building or editing a set.
 *
 * <p>The <em>resolving parent</em> is what makes the styles chain: an attribute this set does
 * not define is asked of the parent, and so on up to the document's default style.
 */
public interface MutableAttributeSet extends AttributeSet {

    void addAttribute(Object name, Object value);

    void addAttributes(AttributeSet attributes);

    void removeAttribute(Object name);

    /** It removes those names; whatever value they had does not matter. */
    void removeAttributes(Enumeration<?> names);

    /**
     * It removes those this set has with the same value as the other.
     *
     * <p>With the same value, not only the same name: removing "bold = false" from a set where
     * bold is at {@code true} does nothing.
     */
    void removeAttributes(AttributeSet attributes);

    /** See the interface note. */
    void setResolveParent(AttributeSet parent);
}
