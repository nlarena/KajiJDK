package javax.print.attribute;

// An AttributeSet that only accepts attributes that are `PrintJobAttribute`s. The restriction is
// checked at run time; see DocAttributeSet.
public interface PrintJobAttributeSet extends AttributeSet {

    // ClassCastException if `attribute` is not a PrintJobAttribute.
    boolean add(Attribute attribute);

    // ClassCastException if any of the attributes is not a PrintJobAttribute.
    boolean addAll(AttributeSet attributes);
}
