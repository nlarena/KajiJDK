package javax.print.attribute;

// An AttributeSet that only accepts attributes that are `PrintServiceAttribute`s. The restriction
// is checked at run time; see DocAttributeSet.
public interface PrintServiceAttributeSet extends AttributeSet {

    // ClassCastException if `attribute` is not a PrintServiceAttribute.
    boolean add(Attribute attribute);

    // ClassCastException if any of the attributes is not a PrintServiceAttribute.
    boolean addAll(AttributeSet attributes);
}
