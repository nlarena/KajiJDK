package javax.print.attribute;

// An AttributeSet that only accepts attributes that are `PrintRequestAttribute`s. The restriction
// is checked at run time; see DocAttributeSet.
public interface PrintRequestAttributeSet extends AttributeSet {

    // ClassCastException if `attribute` is not a PrintRequestAttribute.
    boolean add(Attribute attribute);

    // ClassCastException if any of the attributes is not a PrintRequestAttribute.
    boolean addAll(AttributeSet attributes);
}
