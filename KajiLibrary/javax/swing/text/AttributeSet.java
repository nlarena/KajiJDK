package javax.swing.text;

import java.util.Enumeration;

/**
 * A set of style attributes, read-only and chainable.
 *
 * <p>What tells it apart from an ordinary map is the **parent**. A set can resolve an attribute
 * it does not have by delegating to another, and that way a paragraph inherits the document's
 * style without copying it. That is why {@link #getAttribute} may answer something
 * {@link #isDefined} denies: the first looks along the chain, the second looks only at this
 * link.
 *
 * <p>The four nested interfaces declare nothing: they are **marks** that classify a key
 * according to what it applies to --the character, the paragraph, the colour, the font-- so that
 * whoever composes styles knows what can be mixed with what.
 *
 * <p><strong>It is the only thing of `javax.swing.text` this library brought.</strong> It is
 * here because {@code javax.accessibility.AccessibleText} names it, and without it that package
 * could not declare two of its methods. Writing it whole --it is self-contained and it is eight
 * methods-- was better than leaving those that depend on it incomplete.
 */
public interface AttributeSet {

    /** The key under which a set keeps its name. */
    Object NameAttribute = "name";

    /** The key under which a set keeps its parent. */
    Object ResolveAttribute = "resolver";

    /** Mark of the keys that apply to a character. */
    public interface CharacterAttribute {
    }

    /** Mark of the keys that apply to a paragraph. */
    public interface ParagraphAttribute {
    }

    /** Mark of the colour keys. */
    public interface ColorAttribute {
    }

    /** Mark of the font keys. */
    public interface FontAttribute {
    }

    /** How many attributes **this** set has, not counting the inherited ones. */
    int getAttributeCount();

    /** Whether this set defines that key by itself. */
    boolean isDefined(Object attrName);

    /** Whether the two sets define exactly the same. */
    boolean isEqual(AttributeSet attr);

    /** An independent copy. */
    AttributeSet copyAttributes();

    /**
     * That key's value.
     *
     * <p>It looks along the chain of parents, so it may return something {@link #isDefined}
     * denies.
     *
     * @return the value, or `null` if it is in no link
     */
    Object getAttribute(Object key);

    /** **This** set's keys, without the inherited ones. */
    Enumeration<?> getAttributeNames();

    /** Whether that key-value pair is in the chain. */
    boolean containsAttribute(Object name, Object value);

    /** Whether all those pairs are in the chain. */
    boolean containsAttributes(AttributeSet attributes);

    /** The set the search goes on in, or `null` if this is the last one. */
    AttributeSet getResolveParent();
}
