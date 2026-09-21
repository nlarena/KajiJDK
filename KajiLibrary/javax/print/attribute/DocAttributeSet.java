package javax.print.attribute;

// An AttributeSet that only accepts attributes that are `DocAttribute`s.
//
// The restriction cannot be put in the signature -- `add` still takes an `Attribute`, because one
// has to be able to pass any AttributeSet to `addAll` --, so it is enforced at run time: whatever
// is not a DocAttribute goes out through ClassCastException. Redeclaring `add`/`addAll` here exists
// precisely to document that exception; it does not change the signature.
public interface DocAttributeSet extends AttributeSet {

    // ClassCastException if `attribute` is not a DocAttribute.
    boolean add(Attribute attribute);

    // ClassCastException if any of the attributes is not a DocAttribute.
    boolean addAll(AttributeSet attributes);
}
